cmake_minimum_required(VERSION 2.6...3.16)
project(lmpjni C)

set(CMAKE_C_STANDARD 11)

if (NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()

set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY         ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY         ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY         ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY_RELEASE ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE ${CMAKE_BINARY_DIR}/lib)

set(SOURCE_FILES
    jtool_lmp_NativeLmp.c)
add_library(lmpjni SHARED ${SOURCE_FILES})

if (CMAKE_SYSTEM_NAME MATCHES "Windows")
    set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)
    set(BUILD_SHARED_LIBS ON)
endif ()

# add jni
if (CMAKE_SYSTEM_NAME MATCHES "Linux")
    include_directories($ENV{JAVA_HOME}/include $ENV{JAVA_HOME}/include/linux)
elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
    include_directories($ENV{JAVA_HOME}/include $ENV{JAVA_HOME}/include/win32)
elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
    include_directories($ENV{JAVA_HOME}/include $ENV{JAVA_HOME}/include/darwin)
endif ()

# add mpi
find_package(MPI)
if (MPI_FOUND)
    add_definitions(-DLAMMPS_LIB_MPI)
    
    set(CMAKE_COMPILE_FLAGS ${CMAKE_COMPILE_FLAGS} ${MPI_COMPILE_FLAGS})
    set(CMAKE_LINK_FLAGS ${CMAKE_LINK_FLAGS} ${MPI_LINK_FLAGS})
    
    include_directories(${MPI_INCLUDE_PATH})
    target_link_libraries(lmpjni ${MPI_LIBRARIES})
endif(MPI_FOUND)

# add lammps
include_directories($ENV{LAMMPS_HOME}includes)
if (CMAKE_SYSTEM_NAME MATCHES "Linux")
    target_link_libraries(lmpjni $ENV{LAMMPS_HOME}lib/liblammps.so)
elseif (CMAKE_SYSTEM_NAME MATCHES "Windows")
    target_link_libraries(lmpjni $ENV{LAMMPS_HOME}lib/lammps.lib)
elseif (CMAKE_SYSTEM_NAME MATCHES "Darwin")
    target_link_libraries(lmpjni $ENV{LAMMPS_HOME}lib/liblammps.dylib)
endif ()

