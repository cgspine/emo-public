//
// Created by cgspi on 2022/12/31.
//

#ifndef EMO_META_H
#define EMO_META_H
#include <string>

namespace EmoKV {
    class Meta {
    public:
        Meta(std::string& dir);
        ~Meta();
        void updateAllPath(std::string index, std::string key, std::string value);
        void updateIndexPath(std::string path);
        void updateKeyPath(std::string path);
        void updateValuePath(std::string path);

        std::string& dir();
        std::string& meta_path();
        std::string& index_path();
        std::string& key_path();
        std::string& value_path();
        static std::string gen_index_path(std::string& dir);
        static std::string gen_key_path(std::string& dir);
        static std::string gen_value_path(std::string& dir);
    private:
        std::string dir_;
        std::string meta_path_;
        std::string index_path_;
        std::string key_path_;
        std::string value_path_;
        size_t index_size;
        void flush();
    };
}

#endif //EMO_META_H
