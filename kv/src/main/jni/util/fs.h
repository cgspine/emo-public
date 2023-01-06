//
// Created by cgspi on 2022/12/31.
//

#ifndef EMO_FS_H
#define EMO_FS_H
#include <string>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>

namespace EmoKV {

    static bool isFileExist(const std::string& path){
        if (path.empty()) {
            return false;
        }
        struct stat st = {};
        return lstat(path.c_str(), &st) == 0;
    }

    static size_t getFileSize(int fd) {
        struct stat st = {};
        if (fstat(fd, &st) != -1) {
            return (size_t) st.st_size;
        }
        return -1;
    }

    static void* make_mmap(const std::string& path, size_t mini_space, size_t& size){
        auto fd = open(path.c_str(), O_RDWR | O_CREAT, S_IRWXU);
        if(fd == -1){
            return nullptr;
        }
        size_t file_len = getFileSize(fd);
        if(file_len < mini_space){
            ftruncate(fd, static_cast<off_t>(mini_space));
            file_len = mini_space;
        }
        void* start = mmap(nullptr, file_len, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
        close(fd);
        if((long)fd == -1){
            return nullptr;
        }
        size = file_len;
        return start;
    }
}
#endif //EMO_FS_H
