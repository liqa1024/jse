cmake_minimum_required(VERSION 3.15)
project(lmpjni CXX)

# MSVC hacks for lammps
if(MSVC)
    if(CMAKE_CXX_COMPILER_ID STREQUAL "MSVC")
        add_compile_options(/Zc:__cplusplus)
        add_compile_options(/wd4244)
        add_compile_options(/wd4267)
        add_compile_options(/EHsc)
    endif()
    add_compile_definitions(_CRT_SECURE_NO_WARNINGS)
endif()

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Need -restrict with Intel compilers
if(CMAKE_CXX_COMPILER_ID STREQUAL "Intel")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -restrict")
endif()

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()


set(SOURCE_FILES
    jse_lmp_NativeLmp.cpp)
add_library(lmpjni SHARED ${SOURCE_FILES})

if(CMAKE_SYSTEM_NAME MATCHES "Windows")
    set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)
    set(BUILD_SHARED_LIBS ON)
endif()


# add jni
find_package(JNI)
if(JNI_FOUND)
    include_directories(${JNI_INCLUDE_DIRS})
    target_link_libraries(lmpjni ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "No java development environment found")
endif()


# add jniutil
include_directories("$ENV{JSE_JNIUTIL_INCLUDE_DIR}")


# add mpi
find_package(MPI)
if(MPI_CXX_FOUND)
    add_definitions(-DLAMMPS_LIB_MPI)
    
    set(MPI_CXX_SKIP_MPICXX TRUE)
    
    target_link_libraries(lmpjni MPI::MPI_CXX)
else()
    message(WARNING "No MPI development environment found, lmpjni will build without MPI support")
endif()


# add mimalloc
option(JSE_USE_MIMALLOC "Use mimalloc to accelerate `malloc` operation" ON)
if(JSE_USE_MIMALLOC)
    add_definitions(-DUSE_MIMALLOC)
    
    include_directories("$ENV{JSE_MIMALLOC_INCLUDE_DIR}")
    target_link_libraries(lmpjni "$ENV{JSE_MIMALLOC_LIB_PATH}")
endif()


# add lammps
option(JSE_LAMMPS_IS_OLD "Build based on old version lammps (like version below 18Sep2020)" OFF)
option(JSE_LAMMPS_HAS_EXCEPTIONS "Build based on lammps which has error handling" ON)
option(JSE_LAMMPS_EXCEPTIONS_NULL_SUPPORT "NULL support for lammps_has_error" ON)

if(JSE_LAMMPS_IS_OLD)
    add_definitions(-DLAMMPS_OLD)
endif()

if(JSE_LAMMPS_HAS_EXCEPTIONS)
    add_definitions(-DLAMMPS_EXCEPTIONS)
endif()

if(JSE_LAMMPS_EXCEPTIONS_NULL_SUPPORT)
    add_definitions(-DLAMMPS_EXCEPTIONS_NULL_SUPPORT)
endif()

include_directories("$ENV{JSE_LMP_INCLUDE_DIR}")
target_link_libraries(lmpjni "$ENV{JSE_LMP_LIB_PATH}")

