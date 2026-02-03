cmake_minimum_required(VERSION 3.15)
project(mpijni C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 11)

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()


set(SOURCE_FILES
    jse_parallel_MPI_Native.c)
add_library(mpijni SHARED ${SOURCE_FILES})

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
    target_link_libraries(mpijni ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "No java development environment found")
endif()


# add jniutil
include_directories("$ENV{JSE_JNIUTIL_INCLUDE_DIR}")


# add mpi
find_package(MPI)
if(JSE_USE_CXX)
    if(MPI_CXX_FOUND)
        set(MPI_CXX_SKIP_MPICXX TRUE)
        
        target_link_libraries(mpijni MPI::MPI_CXX)
    else()
        message(FATAL_ERROR "No MPI CXX development environment found")
    endif()
else()
    if(MPI_C_FOUND)
        target_link_libraries(mpijni MPI::MPI_C)
    else()
        message(FATAL_ERROR "No MPI C development environment found")
    endif()
endif()


# add mimalloc
option(JSE_USE_MIMALLOC "Use mimalloc to accelerate `malloc` operation" ON)
if(JSE_USE_MIMALLOC)
    add_definitions(-DUSE_MIMALLOC)
    
    include_directories("$ENV{JSE_MIMALLOC_INCLUDE_DIR}")
    target_link_libraries(mpijni "$ENV{JSE_MIMALLOC_LIB_PATH}")
endif()


# copy jarray
option(JSE_COPY_JARRAY "Copy jarray when communicate" ON)
if(JSE_COPY_JARRAY)
    add_definitions(-DCOPY_JARRAY)
endif()

