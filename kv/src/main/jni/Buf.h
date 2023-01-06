//
// Created by cgspi on 2022/12/30.
//

#ifndef EMO_BUF_H
#define EMO_BUF_H

#include <cstdint>

namespace EmoKV {
    class Buf {
    public:
        Buf(const uint8_t* ptr, size_t len, bool own);
        ~Buf();
        const uint8_t* ptr();
        uint32_t hash(uint32_t max);
        bool equal(Buf* buf);
        size_t len() const;
    private:
        const uint8_t* ptr_;
        size_t len_;
        bool own_;
    };
}
#endif //EMO_BUF_H
