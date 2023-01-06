//
// Created by cgspi on 2022/12/31.
//

#ifndef EMO_VALUE_H
#define EMO_VALUE_H

#include <cstddef>
#include <cstdint>
#include "../Buf.h"

namespace EmoKV {
    class Value {
    public:
        Value(void* start, size_t size);
        ~Value();

        std::unique_ptr<Buf> get(uint64_t offset, size_t len);
        int put(uint64_t offset, const uint8_t* data, size_t len) const;
        void copy_to(Value* target, uint64_t src, uint64_t dst, size_t len);
        size_t  size() const;

    private:
        void* start_;
        size_t size_;
    };
}


#endif //EMO_VALUE_H
