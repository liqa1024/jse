cmake_minimum_required(VERSION 3.14)
project(jniutil C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 11)

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()


set(SOURCE_FILES
    jniutil.c)
add_library(jniutil SHARED ${SOURCE_FILES})

option(JSE_USE_CXX "Use CXX compiler" OFF)
if(JSE_USE_CXX)
    set_source_files_properties(${SOURCE_FILES} PROPERTIES LANGUAGE CXX)
endif()

if(CMAKE_SYSTEM_NAME MATCHES "Windows")
    set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)
    set(BUILD_SHARED_LIBS ON)
endif()


# add jni
find_package(JNI)
if(JNI_FOUND)
    include_directories(${JNI_INCLUDE_DIRS})
else ()
    message(FATAL_ERROR "No Java Environment Found")
endif()


# add mimalloc
option(JSE_USE_MIMALLOC "Use mimalloc to accelerate `malloc` operation" ON)
if(JSE_USE_MIMALLOC)
    add_definitions(-DUSE_MIMALLOC)
    
    include_directories($ENV{JSE_MIMALLOC_INCLUDE_DIR})
    target_link_libraries(jniutil $ENV{JSE_MIMALLOC_LIB_PATH})
endif()

