cmake_minimum_required(VERSION 3.18)
project(nnap CXX)

set(CMAKE_CXX_STANDARD 17)

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()


set(SOURCE_FILES
    jsex_nnap_NNAP.cpp)
add_library(nnap SHARED ${SOURCE_FILES})

if(CMAKE_SYSTEM_NAME MATCHES "Windows")
    set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)
    set(BUILD_SHARED_LIBS ON)
endif()


# add jni
find_package(JNI)
if(JNI_FOUND)
    include_directories(${JNI_INCLUDE_DIRS})
    target_link_libraries(nnap ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "No Java Environment Found")
endif()


# add jniutil
include_directories("$ENV{JSE_JNIUTIL_INCLUDE_DIR}")


# add mimalloc
option(JSE_USE_MIMALLOC "Use mimalloc to accelerate `malloc` operation" ON)
if(JSE_USE_MIMALLOC)
    add_definitions(-DUSE_MIMALLOC)
    
    include_directories("$ENV{JSE_MIMALLOC_INCLUDE_DIR}")
    target_link_libraries(nnap "$ENV{JSE_MIMALLOC_LIB_PATH}")
endif()


# add libtorch
set(Torch_DIR "$ENV{JSE_TORCH_DIR}")
find_package(Torch REQUIRED)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${TORCH_CXX_FLAGS}")
target_link_libraries(nnap "${TORCH_LIBRARIES}")

