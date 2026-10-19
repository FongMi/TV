cmake_minimum_required(VERSION 3.22.1)
project(nodebridge)

add_library(node SHARED IMPORTED)
set_target_properties(node PROPERTIES IMPORTED_LOCATION
        "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libnode.so")

add_library(nodebridge SHARED node_bridge.cpp)
target_link_libraries(nodebridge node log)
target_link_options(nodebridge PRIVATE
        "-Wl,-z,max-page-size=16384"
        "-Wl,-z,common-page-size=16384")
