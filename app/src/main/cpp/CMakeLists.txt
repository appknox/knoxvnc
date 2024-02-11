cmake_minimum_required(VERSION 3.4.1)
project (knoxvnc C)

set(BUILD_SHARED_LIBS OFF CACHE BOOL "Build shared Libs" FORCE)

if (CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
    set(CMAKE_ASM_FLAGS "${CMAKE_ASM_FLAGS} --target=aarch64-linux-android${ANDROID_VERSION}")
elseif (CMAKE_ANDROID_ARCH_ABI MATCHES "^armeabi.*")  # armeabi-v7a || armeabi-v6 || armeabi
    set(CMAKE_ASM_FLAGS "${CMAKE_ASM_FLAGS} --target=arm-linux-androideabi${ANDROID_VERSION}")
endif ()

# build libJPEG
message("------libjpeg-turbo-----")
set(libjpeg_src_DIR ${CMAKE_SOURCE_DIR}/../../../../libjpeg-turbo)
set(libjpeg_build_DIR ${CMAKE_BINARY_DIR}/libjpeg)

add_subdirectory(${libjpeg_src_DIR} ${libjpeg_build_DIR})
set(JPEG_LIBRARY ${libjpeg_build_DIR}/libturbojpeg.a CACHE FILEPATH "")
set(JPEG_INCLUDE_DIR ${libjpeg_src_DIR} CACHE PATH "")
include_directories(
        ${libjpeg_src_DIR}
        ${libjpeg_build_DIR}
)

# build LibVNCServer
message("------LibVNCServer-----")
set(libvnc_src_DIR ${CMAKE_SOURCE_DIR}/../../../../libvncserver)
set(libvnc_build_DIR ${CMAKE_BINARY_DIR}/libvnc)
add_subdirectory(${libvnc_src_DIR} ${libvnc_build_DIR})
include_directories(
        ${libvnc_src_DIR}/include
        ${libvnc_build_DIR}/include
)

# build libvnc
add_library(vnc SHARED vnc.c)
target_link_libraries(vnc
    log
    vncserver)

