//
// Created by cgspi on 2022/12/31.
//

#include "Meta.h"

#include <iostream>
#include <fstream>
#include <chrono>

namespace EmoKV {
    Meta::Meta(std::string dir) :
            dir_(std::move(dir)),
            meta_path_(dir_ + "/meta") {
        std::ifstream meta_file;
        meta_file.open(meta_path_, std::ios::in);
        if (meta_file.is_open()) {
            getline(meta_file, index_path_);
            getline(meta_file, key_path_);
            getline(meta_file, value_path_);
            meta_file.close();
        }else{
            updateAllPath(dir_ + "/index_0", dir_ + "/key_0", dir_ + "/value_0");
        }
    }

    Meta::~Meta() = default;

    void Meta::updateAllPath(std::string index, std::string key, std::string value) {
        index_path_ = std::move(index);
        key_path_ = std::move(key);
        value_path_ = std::move(value);
        flush();
    }

    void Meta::updateIndexPath(std::string path) {
        index_path_ = std::move(path);
        flush();
    }

    void Meta::updateKeyPath(std::string path) {
        key_path_ = std::move(path);
        flush();

    }

    void Meta::updateValuePath(std::string path) {
        value_path_ = std::move(path);
        flush();
    }

    void Meta::flush() {
        std::ofstream meta_file;
        meta_file.open(meta_path_, std::ios::out | std::ios::trunc);
        if (meta_file.is_open()) {
            meta_file << index_path_;
            meta_file << "\n";
            meta_file << key_path_;
            meta_file << "\n";
            meta_file << value_path_;
            meta_file << "\n";
            meta_file.close();
        }
    }

    std::string &Meta::dir() {
        return dir_;
    }

    std::string &Meta::meta_path() {
        return meta_path_;
    }

    std::string &Meta::index_path() {
        return index_path_;
    }

    std::string &Meta::key_path() {
        return key_path_;
    }

    std::string &Meta::value_path() {
        return value_path_;
    }

    std::string Meta::gen_value_path(std::string& dir) {
        const auto p1 = std::chrono::system_clock::now();
        auto time = std::chrono::duration_cast<std::chrono::milliseconds>(
                p1.time_since_epoch()).count();
        return dir + "/value_" + std::to_string(time);
    }

    std::string Meta::gen_key_path(std::string& dir) {
        const auto p1 = std::chrono::system_clock::now();
        auto time = std::chrono::duration_cast<std::chrono::milliseconds>(
                p1.time_since_epoch()).count();
        return dir + "/key_" + std::to_string(time);
    }

    std::string Meta::gen_index_path(std::string &dir) {
        const auto p1 = std::chrono::system_clock::now();
        auto time = std::chrono::duration_cast<std::chrono::milliseconds>(
                p1.time_since_epoch()).count();
        return dir + "/index_" + std::to_string(time);
    }

}