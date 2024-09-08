cmake_minimum_required(VERSION 3.14)
project(cpointer C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 11)

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()


set(SOURCE_FILES
    jse_clib_CPointer.c
    jse_clib_IntCPointer.c
    jse_clib_DoubleCPointer.c
    jse_clib_NestedCPointer.c
    jse_clib_NestedIntCPointer.c
    jse_clib_NestedDoubleCPointer.c)
add_library(cpointer SHARED ${SOURCE_FILES})

option(JSE_USE_CXX "Use CXX compiler" OFF)
if(CMAKE_C_COMPILER_ID MATCHES "MSVC")
    set(JSE_USE_CXX ON)
endif()

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
    target_link_libraries(cpointer ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "No Java Environment Found")
endif()


# add jniutil
include_directories($ENV{JSE_JNIUTIL_INCLUDE_DIR})


# add mimalloc
option(JSE_USE_MIMALLOC "Use mimalloc to accelerate `malloc` operation" ON)
if(JSE_USE_MIMALLOC)
    add_definitions(-DUSE_MIMALLOC)
    
    include_directories($ENV{JSE_MIMALLOC_INCLUDE_DIR})
    target_link_libraries(cpointer $ENV{JSE_MIMALLOC_LIB_PATH})
endif()

