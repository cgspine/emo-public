//
// Created by cgspi on 2022/12/30.
//

#include "KV.h"

#include <sys/mman.h>
#include <unistd.h>
#include <thread>
#include <vector>
#include <dirent.h>
#include "util/log.h"
#include "util/fs.h"

namespace EmoKV {
    KV* KV::make(
            std::string dir,
            size_t index_init_space,
            size_t key_init_space,
            size_t value_init_space,
            float hash_factor,
            int update_count_to_auto_compact
    ) {
        std::unique_ptr<Meta> meta(new Meta(std::move(dir)));
        size_t index_file_size;
        void* index_start = make_mmap(meta->index_path(), index_init_space, index_file_size);
        if(index_start == nullptr){
            return nullptr;
        }
        std::unique_ptr<Index> index(new Index(index_start, index_file_size, IndexMode::MMAP));

        size_t key_file_size;
        void* key_start = make_mmap(meta->key_path(), key_init_space, key_file_size);
        if(key_start == nullptr){
            return nullptr;
        }
        std::unique_ptr<Value> key(new Value(key_start, key_file_size));

        size_t value_file_size;
        void* value_start = make_mmap(meta->value_path(), value_init_space, value_file_size);
        if(value_start == nullptr){
            return nullptr;
        }
        std::unique_ptr<Value> value(new Value(value_start, value_file_size));
        return new KV(
                std::move(meta),
                std::move(index),
                std::move(key),
                std::move(value),
                hash_factor,
                update_count_to_auto_compact
        );
    }

    KV::KV(
            std::unique_ptr<Meta> meta,
            std::unique_ptr<Index> index,
            std::unique_ptr<Value> key,
            std::unique_ptr<Value> value,
            float hash_factor,
            int update_count_to_auto_compact
    ) : meta_(std::move(meta)),
    index_(std::move(index)),
    key_(std::move(key)),
    value_(std::move(value)),
    hash_factor(hash_factor),
    reading_count_(0),
    update_count_to_auto_compact(update_count_to_auto_compact){
        std::function<void()> func = [this]() {
            msg_runner();
        };
        msg_thread_ = std::move(std::thread(func));
    }

    KV::~KV(){
        {
            std::lock_guard<std::mutex> lock(msg_lock_);
            msg_ |= MSG_EXIT;
            msg_cond_.notify_all();
        }
        if(msg_thread_.joinable()){
            msg_thread_.join();
        }
    }

    std::unique_ptr<Buf> KV::Get(std::unique_ptr<Buf> key) {
        auto v = reading_count_.load();
        while (true){
            if(v == -1){
                std::this_thread::yield();
            }else{
                if(reading_count_.compare_exchange_strong(v, v+1)){
                    break;
                }
            }
        }
        auto ret = index_->read(key_.get(), value_.get(), key.get());
        v = reading_count_.load();

        reading_count_.fetch_add(-1);
        return ret;
    }

    bool KV::Put(std::unique_ptr<Buf> key, std::unique_ptr<Buf> value) {
        bool write_failed = false;
        {
            std::lock_guard<std::mutex> lock(writing_lock_);
            int ret = index_->write(key_.get(), value_.get(), key.get(), value.get());
            if(ret == -1){
                if(expand_value(true)){
                    ret = index_->write(key_.get(), value_.get(), key.get(), value.get());
                    write_failed = ret < 0;
                }else{
                    LOG_I("Put: expand key storage failed.");
                    write_failed = true;
                }
            } else if(ret == -2){
                if(expand_value(false)){
                    ret = index_->write(key_.get(), value_.get(), key.get(), value.get());
                    write_failed = ret < 0;
                }else{
                    LOG_I("Put: expand value storage failed.");
                    write_failed = true;
                }
            }

            if(!write_failed){
                if(index_->key_count() * 1.0 / index_->capability() > hash_factor){
                    expand_index();
                }

                if(index_->updated_count() > update_count_to_auto_compact){
                    std::lock_guard<std::mutex> msg_lock(msg_lock_);
                    // double check
                    if(index_->updated_count() > update_count_to_auto_compact){
                        msg_ |= MSG_COMPACT;
                        msg_cond_.notify_all();
                    }
                }
            }
        }
        return !write_failed;
    }

    void KV::Del(std::unique_ptr<Buf> key) {
        {
            std::lock_guard<std::mutex> lock(writing_lock_);
            index_->del(key_.get(), key.get());
        }
    }

    void KV::Compact() {
        std::lock_guard<std::mutex> msg_lock(msg_lock_);
        msg_ |= MSG_COMPACT;
        msg_cond_.notify_all();
    }

    bool KV::expand_index() {
        size_t index_file_size;
        auto new_path = Meta::gen_index_path(meta_->dir());
        void* index_start = make_mmap(new_path, index_->size() * 2, index_file_size);
        if(index_start == nullptr){
            return false;
        }
        std::unique_ptr<Index> index(new Index(index_start, index_file_size, IndexMode::MMAP));
        index->copy_from(key_.get(), index_.get());
        while (true){
            auto zero = 0;
            if(reading_count_.compare_exchange_strong(zero, -1)){
                meta_->updateIndexPath(new_path);
                index_ = std::move(index);
                reading_count_.store(0);
                std::lock_guard<std::mutex> msg_lock(msg_lock_);
                msg_ |= MSG_CLEAN_FILES;
                msg_cond_.notify_all();
                break;
            }
            std::this_thread::yield();
        }
        return true;
    }

    bool KV::expand_value(bool is_key){
        size_t file_size;
        void* map_start;
        if(is_key){
            map_start = make_mmap(meta_->key_path(), key_->size() * 2, file_size);
        }else{
            map_start = make_mmap(meta_->value_path(), value_->size() * 2, file_size);
        }

        if(map_start == nullptr){
            return false;
        }

        while (true){
            auto zero = 0;
            if(reading_count_.compare_exchange_strong(zero, -1)){
                if(is_key){
                    key_ = std::unique_ptr<Value>(new Value(map_start, file_size));
                }else{
                    value_ = std::unique_ptr<Value>(new Value(map_start, file_size));
                }
                reading_count_.store(0);
                break;
            }
            std::this_thread::yield();
        }
        return true;
    }

    void KV::msg_runner() {
        std::this_thread::sleep_for(std::chrono::seconds(5));
        while (true){
            int local_msg;
            {
                std::unique_lock<std::mutex> lock(msg_lock_);
                while (msg_ == 0){
                    msg_cond_.wait(lock);
                }
                local_msg = msg_;
            }

            if((local_msg & MSG_EXIT) == MSG_EXIT){
                break;
            }

            if((local_msg & MSG_COMPACT) == MSG_COMPACT){
                std::lock_guard<std::mutex> lock(writing_lock_);
                size_t index_file_size;
                auto new_index_path = Meta::gen_index_path(meta_->dir());
                auto new_value_path = Meta::gen_value_path(meta_->dir());
                void* index_start = make_mmap(new_index_path, index_->size(), index_file_size);
                if(index_start != nullptr){
                    std::unique_ptr<Index> index(new Index(index_start, index_file_size, IndexMode::MMAP));
                    index->copy_from(key_.get(), index_.get());

                    size_t value_file_size;
                    void* value_start = make_mmap(new_value_path, value_->size(), value_file_size);
                    if(value_start != nullptr){
                        std::unique_ptr<Value> value(new Value(value_start, value_file_size));
                        index->compact(value_.get(), value.get());
                        while (true){
                            auto zero = 0;
                            if(reading_count_.compare_exchange_strong(zero, -1)){
                                meta_->updateAllPath(new_index_path, meta_->key_path(), new_value_path);
                                index_ = std::move(index);
                                value_ = std::move(value);
                                local_msg |= MSG_CLEAN_FILES;
                                reading_count_.store(0);
                                break;
                            }
                            std::this_thread::yield();
                        }
                    }
                }
            }

            if((local_msg & MSG_CLEAN_FILES) == MSG_CLEAN_FILES){
                DIR *dir = opendir(meta_->dir().c_str());
                if(dir){
                    std::vector<std::string> paths;
                    {
                        std::lock_guard<std::mutex> lock(writing_lock_);
                        struct dirent* ptr;
                        while ((ptr = readdir(dir)) != nullptr){
                            if(strcmp(ptr->d_name, ".") != 0 && strcmp(ptr->d_name, "..") != 0){
                                std::string path = meta_->dir() + "/" + ptr->d_name;
                                if(path != meta_->meta_path() &&
                                   path != meta_->key_path() &&
                                   path != meta_->value_path() &&
                                   path != meta_->index_path()){
                                    paths.push_back(std::move(path));
                                }
                            }
                        }
                    }
                    closedir(dir);
                    for (const auto &item : paths){
                        std::remove(item.c_str());
                    }
                }
            }

            std::unique_lock<std::mutex> lock(msg_lock_);
            msg_ = 0;
        }
    }
}
