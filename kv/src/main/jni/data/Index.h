//
// Created by cgspi on 2022/12/31.
//

#ifndef EMO_INDEX_H
#define EMO_INDEX_H

#include <cstddef>
#include <memory>
#include "../Buf.h"
#include "Value.h"

#define INDEX_HEADER_LEN 64

namespace EmoKV {
    enum IndexMode {
        MMAP = 1,
        MEMORY
    };

    struct WriteInfo {
        bool writing;
        uint32_t version;
        uint32_t index;
    };

    class Index {
    public:
        Index(void* start, size_t size, IndexMode mode);
        ~Index();
        std::unique_ptr<Buf> read(Value* key_storage, Value* value_storage, Buf* key);
        int write(Value* key_storage, Value* value_storage, Buf* key, Buf* value);
        void del(Value* key_storage, Buf* key);
        size_t size() const;
        uint32_t key_count();
        uint32_t updated_count();
        uint32_t capability() const;
        uint64_t key_pos();
        uint64_t value_pos();
        void copy_from(Value* key_storage, Index* from);
        void compact(Value* from_storage, Value* to_storage);
        static bool flag_is_set(uint8_t flag);
        static bool flag_is_ref(uint8_t flag);
        static bool flag_is_editing(uint8_t flag);
        static bool flag_is_deleted(uint8_t flag);
        static void set_flag_set(uint8_t& flag, bool set);
        static void set_flag_ref(uint8_t& flag, bool ref);
        static void set_flag_editing(uint8_t& flag, bool editing);
        static void set_flag_deleted(uint8_t& flag, bool deleted);
        static size_t item_size();
        void update_key_count(uint32_t count);
        void update_updated_count(uint32_t count);
        void update_key_pos(uint64_t pos);
        void update_value_pos(uint64_t pos);

    private:
        void* start_;
        IndexMode mode_;
        size_t size_;
        std::atomic<WriteInfo> write_info_;
    };
}


#endif //EMO_INDEX_H
