# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.
cmake_minimum_required(VERSION 3.6.0)

add_library(EmoKV
        SHARED
        bridge.cpp
        util/log.h
        util/jni.h
        util/fs.h
        data/Meta.h
        data/Meta.cpp
        data/Index.h
        data/Index.cpp
        data/Value.h
        data/Value.cpp
        Buf.h
        Buf.cpp
        KV.h
        KV.cpp
        )

# find log
find_library( # Defines the name of the path variable that stores the location of the NDK library.
        log-lib
        # Specifies the name of the NDK library that CMake needs to locate.
        log)


# Links the native library against one or more other native libraries.
target_link_libraries( # Specifies the target library.
        EmoKV
        ${log-lib})
