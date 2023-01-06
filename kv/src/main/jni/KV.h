//
// Created by cgspi on 2022/12/30.
//

#ifndef EMO_KV_H
#define EMO_KV_H
#include <string>
#include <mutex>
#include <unordered_map>
#include <thread>
#include "Buf.h"
#include "data/Meta.h"
#include "data/Index.h"
#include "data/Value.h"

namespace EmoKV {

    static const int MSG_EXIT = 0x1;
    static const int MSG_COMPACT = 0x2;
    static const int MSG_CLEAN_FILES = 0X4;

    class KV {
    public:
        static KV* make(
                std::string dir,
                size_t index_init_space,
                size_t key_init_space,
                size_t value_init_space,
                float hash_factor,
                int update_count_to_auto_compact
        );
        ~KV();

        std::unique_ptr<Buf> Get(std::unique_ptr<Buf> key);

        bool Put(std::unique_ptr<Buf> key, std::unique_ptr<Buf> value);
        void Del(std::unique_ptr<Buf> key);
        void Compact();

    private:
        std::unique_ptr<Meta> meta_;
        std::unique_ptr<Index> index_;
        std::unique_ptr<Value> key_;
        std::unique_ptr<Value> value_;
        std::atomic_int32_t reading_count_;
        std::thread msg_thread_;
        int msg_ = MSG_CLEAN_FILES;
        std::condition_variable msg_cond_;
        std::mutex msg_lock_;
        std::mutex writing_lock_;
        float hash_factor;
        int update_count_to_auto_compact;
        bool expand_value(bool is_key);
        bool expand_index();
        void msg_runner();
        KV(
                std::unique_ptr<Meta> meta,
                std::unique_ptr<Index> index,
                std::unique_ptr<Value> key,
                std::unique_ptr<Value> value,
                float hash_factor,
                int update_count_to_auto_compact
       );
    };
}




#endif //EMO_KV_H
