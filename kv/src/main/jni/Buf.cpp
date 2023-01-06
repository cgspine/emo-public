//
// Created by cgspi on 2022/12/30.
//

#include <cstdlib>
#include "Buf.h"
#include "util/log.h"

namespace EmoKV {
    Buf::Buf(const uint8_t* ptr, size_t len, bool own): ptr_(ptr), len_(len), own_(own){

    }
    Buf::~Buf(){
        if(own_){
            free((void *)ptr_);
        }
    }
    uint32_t Buf::hash(uint32_t max){
        uint32_t hash = 0;
        for(size_t i = 0; i < len_; i++){
            hash = hash * 31 + *(ptr_ + i);
        }
        return hash % max;
    }

    bool Buf::equal(Buf* buf){
        if(buf->len_ != len_){
            return false;
        }
        if(buf->ptr_ == ptr_){
            return true;
        }
        for(size_t i =0; i < len_; i++){
            if(*(buf->ptr_ + i) != *(ptr_ + i)){
                return false;
            }
        }
        return true;
    }

    const uint8_t* Buf::ptr(){
        return ptr_;
    }
    size_t Buf::len() const{
        return len_;
    }
}