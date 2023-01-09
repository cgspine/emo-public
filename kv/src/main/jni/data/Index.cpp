//
// Created by cgspi on 2022/12/31.
//

#include <cstdlib>
#include <sys/mman.h>
#include <thread>
#include "Index.h"
#include "../util/log.h"

// Header:
// key_count(4), update_count(4), key_pos(8), value_pos(8)
// ....reserved(12).
// backup_item(item_size()), backup_index(4)

// Item:
// flag(1):key_len(1):key_data(8):value_len(2):value_data(8)
namespace EmoKV {
    Index::Index(void* start, size_t size, IndexMode mode):
        start_(start),
        size_(size),
        mode_(mode),
        write_info_(WriteInfo { false, 0, 0}){
        auto* s = static_cast<uint8_t *>(start_);
        uint32_t backup_index = *reinterpret_cast<uint32_t *>(s + INDEX_HEADER_LEN - sizeof(uint32_t));
        if(backup_index < capability()){
            size_t offset = INDEX_HEADER_LEN + backup_index * item_size();
            uint8_t flag = *static_cast<uint8_t *>(s + offset);
            if(flag_is_editing(flag)){
                //restore
                memcpy(s + offset, s - INDEX_HEADER_LEN - item_size() - sizeof(uint32_t), item_size());
                set_flag_editing(flag, false);
                *static_cast<uint8_t *>(s + offset) = flag;
            }
        }
    }

    Index::~Index() {
        if(mode_ == IndexMode::MEMORY){
            free(start_);
        }else{
            munmap(start_, size_);
        }
    }

    size_t Index::item_size(){
        return sizeof(uint8_t) + sizeof(uint8_t) + sizeof(uint64_t) + sizeof(uint16_t) + sizeof(uint64_t);
    }

    std::unique_ptr<Buf> Index::read(Value* key_storage, Value* value_storage, Buf* key){
        uint32_t index = key->hash(capability());
        while (true){
            size_t init_offset = INDEX_HEADER_LEN + index * item_size();
            size_t offset = init_offset;
            auto start = static_cast<uint8_t *>(start_);
            uint8_t flag =  *static_cast<uint8_t *>(start + offset);
            if(!flag_is_set(flag)){
                return {nullptr};
            }
            offset += sizeof(uint8_t);
            uint8_t key_len = *static_cast<uint8_t *>(start + offset);
            offset += sizeof(uint8_t);
            uint64_t key_data = *reinterpret_cast<uint64_t *>(start + offset);
            auto k = key_storage->get(key_data, key_len);
            if(k->equal(key)){
                while (true){
                    offset = init_offset;
                    auto w_info = write_info_.load();
                    if(w_info.writing && w_info.index == index){
                        // there is a write action.
                        std::this_thread::yield();
                        continue;
                    }
                    flag =  *static_cast<uint8_t *>(start + offset);
                    if(flag_is_deleted(flag)){
                        return {nullptr};
                    }
                    offset += sizeof(uint8_t) + sizeof(uint8_t) + sizeof(uint64_t);
                    uint16_t value_len = *reinterpret_cast<uint16_t *>(start + offset);
                    offset += sizeof(uint16_t);
                    std::unique_ptr<Buf> ret;
                    if(flag_is_ref(flag)){
                        uint64_t value_data = *reinterpret_cast<uint64_t *>(start + offset);
                        ret = value_storage->get(value_data, value_len);
                    }else{
                        auto* copy_data = static_cast<uint8_t *>(malloc(value_len));
                        memcpy(copy_data, start + offset, value_len);
                        ret = std::unique_ptr<Buf>(new Buf(copy_data, value_len, true));
                    }
                    auto new_w_info = write_info_.load();
                    if(new_w_info.version == w_info.version){
                        // version not change, it's happy read.
                        return ret;
                    }
                    // only one version update.
                    if(new_w_info.version - w_info.version == 1){
                        if(new_w_info.index == index){
                            if(new_w_info.writing){
                                std::this_thread::yield();
                            }
                            continue;
                        }
                        return ret;
                    }
                    // bad case: has more than one update, re read.
                }
            }
            index++;
            if(index == capability()){
                index = 0;
            }
        }

    }
    int Index::write(Value* key_storage, Value* value_storage, Buf* key, Buf* value){
        uint32_t index = key->hash(capability());
        bool is_update = false;
        while (true){
            size_t init_offset = INDEX_HEADER_LEN + index * item_size();
            size_t offset = init_offset;
            auto start = static_cast<uint8_t *>(start_);
            uint8_t flag =  *static_cast<uint8_t *>(start + offset);
            if(flag_is_set(flag)){
                offset += sizeof(uint8_t);
                uint8_t key_len = *static_cast<uint8_t *>(start + offset);
                offset += sizeof(uint8_t);
                uint64_t key_data = *reinterpret_cast<uint64_t *>(start + offset);
                auto k = key_storage->get(key_data, key_len);
                if(!k->equal(key)){
                    index++;
                    if(index == capability()){
                        index = 0;
                    }
                    continue;
                }

                offset = init_offset;
                // backup
                *reinterpret_cast<uint32_t *>(start + INDEX_HEADER_LEN - sizeof(uint32_t)) = index;
                memcpy(start + INDEX_HEADER_LEN - item_size() - sizeof(uint32_t), start + offset, item_size());
                set_flag_editing(flag, true);
                *static_cast<uint8_t *>(start + offset) = flag;
                is_update = true;
            }else{
                offset += sizeof(uint8_t);
                *static_cast<uint8_t *>(start + offset) = static_cast<uint8_t>(key->len());
                offset += sizeof(uint8_t);
                uint64_t pos = key_pos();
                int put_key = key_storage->put(pos, key->ptr(), key->len());
                if(put_key == -1){
                    // need expand key storage.
                    return -1;
                }
                *reinterpret_cast<uint64_t *>(start + offset) = pos;
                update_key_count(key_count() + 1);
                update_key_pos(pos + key->len());
            }
            auto last = write_info_.load();
            write_info_.store(WriteInfo{1, last.version + 1, index});
            offset = init_offset + sizeof(uint8_t) * 2 + sizeof(uint64_t);
            *reinterpret_cast<uint16_t *>(start + offset) = static_cast<uint16_t>(value->len());
            offset += sizeof(uint16_t);
            if(value->len() <= sizeof(uint64_t)){
                memcpy(start + offset, value->ptr(), value->len());
            }else{
                uint64_t pos = value_pos();
                int put_value = value_storage->put(pos, value->ptr(), value->len());
                if(put_value == -1){
                    // need expand value storage.
                    set_flag_editing(flag, false);
                    *static_cast<uint8_t *>(start + init_offset) = flag;
                    write_info_.store(WriteInfo{true, last.version + 1, index});
                    return -2;
                }
                *reinterpret_cast<uint64_t *>(start + offset) = pos;
                update_value_pos(pos + value->len());
            }
            if(is_update){
                update_updated_count(updated_count() + 1);
            }
            offset = init_offset;
            set_flag_set(flag, true);
            set_flag_deleted(flag, false);
            set_flag_ref(flag, value->len() > sizeof(uint64_t));
            set_flag_editing(flag, false);
            *static_cast<uint8_t *>(start + offset) = flag;
            write_info_.store(WriteInfo{false, 0, index});
            return 0;
        }
    }

    void Index::del(Value *key_storage, Buf *key) {
        uint32_t index = key->hash(capability());
        bool ret = false;
        do{
            size_t init_offset = INDEX_HEADER_LEN + index * item_size();
            size_t offset = init_offset;
            auto start = static_cast<uint8_t *>(start_);
            uint8_t flag =  *static_cast<uint8_t *>(start + offset);
            if(flag_is_set(flag)){
                offset += sizeof(uint8_t);
                uint8_t key_len = *static_cast<uint8_t *>(start + offset);
                offset += sizeof(uint8_t);
                uint64_t key_data = *reinterpret_cast<uint64_t *>(start + offset);
                auto k = key_storage->get(key_data, key_len);
                if(!k->equal(key)){
                    index++;
                    if(index == capability()){
                        index = 0;
                    }
                }else{
                    if(!flag_is_deleted(flag)){
                        set_flag_deleted(flag, true);
                        *static_cast<uint8_t *>(start + init_offset) = flag;
                    }
                    ret = true;
                }
            }
        } while (!ret);
    }

    void Index::copy_from(Value *key_storage, Index *from) {
        update_updated_count(0);
        update_key_pos(from->key_pos());
        update_value_pos(from->value_pos());
        size_t is = item_size();
        auto from_start = static_cast<uint8_t *>(from->start_);
        auto target_start = static_cast<uint8_t *>(start_);
        auto from_cap = from->capability();
        uint32_t key_count = 0;
        for(size_t i = 0; i < from_cap; i++){
            size_t from_init_offset = INDEX_HEADER_LEN + i * is;
            size_t from_offset = from_init_offset;
            uint8_t flag =  *static_cast<uint8_t *>(from_start + from_offset);
            if(flag_is_set(flag) && !flag_is_deleted(flag)){
                from_offset += sizeof(uint8_t);
                uint8_t key_len = *static_cast<uint8_t *>(from_start + from_offset);
                from_offset += sizeof(uint8_t);
                uint64_t key_data = *reinterpret_cast<uint64_t *>(from_start + from_offset);
                auto k = key_storage->get(key_data, key_len);
                uint32_t target_index = k->hash(capability());
                while (true){
                    size_t target_offset = INDEX_HEADER_LEN + target_index * is;
                    uint8_t target_flag = *static_cast<uint8_t *>(target_start + target_offset);
                    if(flag_is_set(target_flag)){
                        target_index ++;
                        if(target_index == capability()){
                            target_index = 0;
                        }
                        continue;
                    }
                    memcpy(target_start + target_offset, from_start + from_init_offset, is);
                    key_count++;
                    break;
                }

            }
        }
        update_key_count(key_count);
    }

    void Index::compact(Value* from_storage, Value* to_storage){
        uint64_t pos = 0;
        size_t is = item_size();
        auto start = static_cast<uint8_t *>(start_);
        auto cap = capability();
        for(size_t i = 0; i < cap; i++){
            size_t offset = INDEX_HEADER_LEN + i * is;
            uint8_t flag =  *static_cast<uint8_t *>(start + offset);
            if(flag_is_set(flag) && !flag_is_deleted(flag) && flag_is_ref(flag)){
                offset += sizeof(uint8_t) * 2 + sizeof(uint64_t);
                uint16_t value_len = *reinterpret_cast<uint16_t *>(start + offset);
                offset += sizeof(uint16_t);
                uint64_t value_pos = *reinterpret_cast<uint64_t *>(start + offset);
                from_storage->copy_to(to_storage, value_pos, pos, value_len);
                *reinterpret_cast<uint64_t *>(start + offset) = pos;
                pos += value_len;
            }
        }
        update_value_pos(pos);
    }

    size_t Index::size() const {
        return size_;
    }

    uint32_t Index::key_count(){
        auto start = static_cast<uint8_t *>(start_);
        return *reinterpret_cast<uint32_t *>(start);
    }
    uint32_t Index::updated_count(){
        auto start = static_cast<uint8_t *>(start_);
        return *reinterpret_cast<uint32_t *>(start + sizeof(uint32_t));
    }

    uint64_t Index::key_pos(){
        auto start = static_cast<uint8_t *>(start_);
        return *reinterpret_cast<uint64_t *>(start + sizeof(uint32_t) * 2);
    }

    uint64_t Index::value_pos(){
        auto start = static_cast<uint8_t *>(start_);
        return *reinterpret_cast<uint64_t *>(start + sizeof(uint32_t) * 2 + sizeof(uint64_t));
    }

    uint32_t Index::capability() const {
        return (size_ - INDEX_HEADER_LEN) / item_size();
    }
    bool Index::flag_is_set(uint8_t flag){
        return (flag & 0x1) == 0x1;
    }
    bool Index::flag_is_ref(uint8_t flag){
        return (flag & 0x2) == 0x2;
    }

    bool Index::flag_is_editing(uint8_t flag){
        return (flag & 0x4) == 0x4;
    }

    bool Index::flag_is_deleted(uint8_t flag) {
        return (flag & 0x8) == 0x8;
    }

    void Index::set_flag_set(uint8_t& flag, bool set){
        if(set){
            flag |= 0x1;
        }else{
            flag &= ~0x1;
        }
    }

    void Index::set_flag_ref(uint8_t& flag, bool ref){
        if(ref){
            flag |= 0x2;
        }else{
            flag &= ~0x2;
        }
    }

    void Index::set_flag_editing(uint8_t& flag, bool editing){
        if(editing){
            flag |= 0x4;
        }else{
            flag &= ~0x4;
        }
    }

    void Index::set_flag_deleted(uint8_t &flag, bool deleted) {
        if(deleted){
            flag |= 0x8;
        }else{
            flag &= ~0x8;
        }
    }

    void Index::update_key_count(uint32_t count){
        auto * start = static_cast<uint8_t *>(start_);
        auto* c = reinterpret_cast<uint32_t *>(start);
        *c =  count;
    }
    void Index::update_updated_count(uint32_t count){
        auto * start = static_cast<uint8_t *>(start_);
        auto* c = reinterpret_cast<uint32_t *>(start + sizeof(uint32_t));
        *c =  count;
    }
    void Index::update_key_pos(uint64_t pos){
        auto * start = static_cast<uint8_t *>(start_);
        auto* p = reinterpret_cast<uint64_t *>(start + sizeof(uint32_t) * 2);
        *p =  pos;
    }
    void Index::update_value_pos(uint64_t pos){
        auto * start = static_cast<uint8_t *>(start_);
        auto* p = reinterpret_cast<uint64_t *>(start + sizeof(uint32_t) * 2 + sizeof(uint64_t));
        *p =  pos;
    }
}