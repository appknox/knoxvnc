
cmake_minimum_required(VERSION 3.4.1)

project (droidVNC-NG C)

# build libJPEG
set(libjpeg_src_DIR ../../../../libjpeg-turbo)
set(libjpeg_build_DIR ${CMAKE_BINARY_DIR}/libjpeg)
# adds a source subdir with specifying a build output dir
add_subdirectory(${libjpeg_src_DIR} ${libjpeg_build_DIR})
# set these variables so FindJPEG can find the library
set(JPEG_LIBRARY ${libjpeg_build_DIR}/libturbojpeg.a)
set(JPEG_INCLUDE_DIR ${libjpeg_src_DIR})
# set include directories so dependent code can find the headers
include_directories(
        ${libjpeg_src_DIR}
        ${libjpeg_build_DIR}
)

# build LibVNCServer
set(libvnc_src_DIR ../../../../libvncserver)
set(libvnc_build_DIR ${CMAKE_BINARY_DIR}/libvnc)
# adds a source subdir with specifying a build output dir
add_subdirectory(${libvnc_src_DIR} ${libvnc_build_DIR})
# need to add the build dir to include dirs as well because of generated rfbconfig.h
include_directories(
        ${libvnc_src_DIR}
        ${libvnc_build_DIR}
)

# build libdroidvnc-ng
add_library(droidvnc-ng SHARED droidvnc-ng.c)
target_link_libraries(droidvnc-ng
    log
    vncserver)
