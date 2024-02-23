cmake_minimum_required(VERSION 3.14)
project(lmpjni C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 11)

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()


set(SOURCE_FILES
    jse_lmp_NativeLmp.c)
add_library(lmpjni SHARED ${SOURCE_FILES})

option(JSE_USE_CXX "Use CXX compiler" OFF)
if(CMAKE_C_COMPILER_ID MATCHES "MSVC|Intel")
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
    target_link_libraries(lmpjni ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "No Java Environment Found")
endif()


# add jniutil
include_directories($ENV{JSE_JNIUTIL_INCLUDE_DIR})


# add mpi
find_package(MPI)
if(JSE_USE_CXX)
    if(MPI_CXX_FOUND)
        add_definitions(-DLAMMPS_LIB_MPI)
        
        set(CMAKE_COMPILE_FLAGS ${CMAKE_COMPILE_FLAGS} ${MPI_CXX_COMPILE_FLAGS})
        set(CMAKE_LINK_FLAGS ${CMAKE_LINK_FLAGS} ${MPI_CXX_LINK_FLAGS})
        
        include_directories(${MPI_CXX_INCLUDE_DIRS})
        target_link_libraries(lmpjni ${MPI_CXX_LIBRARIES})
    endif()
else()
    if(MPI_C_FOUND)
        add_definitions(-DLAMMPS_LIB_MPI)
        
        set(CMAKE_COMPILE_FLAGS ${CMAKE_COMPILE_FLAGS} ${MPI_C_COMPILE_FLAGS})
        set(CMAKE_LINK_FLAGS ${CMAKE_LINK_FLAGS} ${MPI_C_LINK_FLAGS})
        
        include_directories(${MPI_C_INCLUDE_DIRS})
        target_link_libraries(lmpjni ${MPI_C_LIBRARIES})
    endif()
endif()


# add mimalloc
option(JSE_USE_MIMALLOC "Use mimalloc to accelerate `malloc` operation" ON)
if(JSE_USE_MIMALLOC)
    add_definitions(-DUSE_MIMALLOC)
    
    include_directories($ENV{JSE_MIMALLOC_INCLUDE_DIR})
    target_link_libraries(lmpjni $ENV{JSE_MIMALLOC_LIB_PATH})
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
target_link_libraries(lmpjni $ENV{JSE_LMP_LIB_PATH})

