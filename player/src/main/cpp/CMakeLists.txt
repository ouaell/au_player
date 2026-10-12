cmake_minimum_required(VERSION 3.22.1)
project(au_mpv)

add_library(au_mpv SHARED au_mpv.cpp)

# Pinned self-built headers (staged by tools/fetch_native.sh, gitignored).
target_include_directories(au_mpv PRIVATE thirdparty/include)

# Prebuilt libmpv for the ABI being built.
add_library(mpv SHARED IMPORTED)
set_target_properties(mpv PROPERTIES
    IMPORTED_LOCATION "${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libmpv.so")

# Prebuilt libavcodec (carries av_jni_set_java_vm for the JVM registration;
# the av_jni API moved from libavutil to libavcodec in current FFmpeg).
add_library(avcodec SHARED IMPORTED)
set_target_properties(avcodec PROPERTIES
    IMPORTED_LOCATION "${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libavcodec.so")

find_library(log-lib log)
find_library(egl-lib EGL)
find_library(gles-lib GLESv3)

target_link_libraries(au_mpv mpv avcodec ${log-lib} ${egl-lib} ${gles-lib})
