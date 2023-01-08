//
// Created by cgspi on 2022/12/31.
//

#include <cstring>
#include <memory>
#include <sys/mman.h>
#include "Value.h"

namespace EmoKV {
    Value::Value(void *start, size_t size):
    start_(start),
    size_(size) {

    }

    Value::~Value(){
        munmap(start_, size_);
    }

    std::unique_ptr<Buf> Value::get(uint64_t offset, size_t len){
        auto* data = static_cast<uint8_t *>(malloc(len));
        memcpy(data, static_cast<uint8_t *>(start_) + offset, len);
        std::unique_ptr<Buf> ret(new Buf(data, len, true));
        return ret;
    }
    int Value::put(uint64_t offset, const uint8_t* data, size_t len) const{
        if(offset + len > size_){
            return -1;
        }
        memcpy(static_cast<uint8_t *>(start_) + offset, data, len);
        return 0;
    }

    void Value::copy_to(Value* target, uint64_t src, uint64_t dst, size_t len){
        memcpy(static_cast<uint8_t *>(target->start_) + dst, static_cast<uint8_t *>(start_) + src, len);
    }

    size_t  Value::size() const{
        return size_;
    }

}