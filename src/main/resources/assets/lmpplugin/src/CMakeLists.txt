cmake_minimum_required(VERSION 3.14)
project(lmpplugin CXX)

set(CMAKE_CXX_STANDARD 11)

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()


set(SOURCE_FILES
    pair_jse.cpp
    LmpPair.cpp)
add_library(lmpplugin SHARED ${SOURCE_FILES})

if(CMAKE_SYSTEM_NAME MATCHES "Windows")
    set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)
    set(BUILD_SHARED_LIBS ON)
endif()


# add jni
find_package(JNI)
if(JNI_FOUND)
    include_directories(${JNI_INCLUDE_DIRS})
    target_link_libraries(lmpplugin ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "No Java Environment Found")
endif()


# add jniutil
include_directories($ENV{JSE_JNIUTIL_INCLUDE_DIR})


# add mpi
find_package(MPI)
if(MPI_CXX_FOUND)
    add_definitions(-DLAMMPS_LIB_MPI)
    
    set(MPI_CXX_SKIP_MPICXX TRUE)
    
    target_link_libraries(lmpplugin MPI::MPI_CXX)
endif()


# add mimalloc
option(JSE_USE_MIMALLOC "Use mimalloc to accelerate `malloc` operation" ON)
if(JSE_USE_MIMALLOC)
    add_definitions(-DUSE_MIMALLOC)
    
    include_directories($ENV{JSE_MIMALLOC_INCLUDE_DIR})
    target_link_libraries(lmpplugin $ENV{JSE_MIMALLOC_LIB_PATH})
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

include_directories($ENV{JSE_LMP_INCLUDE_DIR})
target_link_libraries(lmpplugin $ENV{JSE_LMP_LIB_PATH})

