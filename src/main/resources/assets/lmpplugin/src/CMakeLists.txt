cmake_minimum_required(VERSION 3.14)
project(lmpplugin CXX)

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
    jseplugin.cpp
    pair_jse.cpp
    fix_jse.cpp
    jse_lmp_LmpPlugin_Pair.cpp
    jse_lmp_LmpPlugin_Fix.cpp
    LmpPair.cpp
    LmpFix.cpp
    LmpPlugin.cpp)
add_library(lmpplugin SHARED ${SOURCE_FILES})

if(CMAKE_SYSTEM_NAME MATCHES "Windows")
    set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)
    set(BUILD_SHARED_LIBS ON)
endif()


# add jse jar path
add_definitions(-DJVM_CLASS_PATH="-Djava.class.path=$ENV{JSE_JAR_PATH_DEF}")
add_definitions(-DJVM_XMX="-Xmx$ENV{JSE_JVM_XMX}")


# add jni
find_package(JNI)
if(JNI_FOUND)
    include_directories(${JNI_INCLUDE_DIRS})
    target_link_libraries(lmpplugin ${JNI_LIBRARIES})
    # delay load jvm.dll on windows
    if((CMAKE_SYSTEM_NAME MATCHES "Windows") AND (CMAKE_CXX_COMPILER_ID STREQUAL "MSVC"))
        target_link_options(lmpplugin PRIVATE "/DELAYLOAD:jvm.dll")
        add_definitions(-DJVM_LIB_PATH="$ENV{JSE_JVM_LIB_PATH_DEF}")
    endif()
else()
    message(FATAL_ERROR "No Java Environment Found")
endif()


# add mpi
find_package(MPI)
if(MPI_CXX_FOUND)
    add_definitions(-DLAMMPS_LIB_MPI)
    
    set(MPI_CXX_SKIP_MPICXX TRUE)
    
    target_link_libraries(lmpplugin MPI::MPI_CXX)
endif()


# add lammps
add_definitions(-DLAMMPS_EXCEPTIONS)
include_directories("$ENV{JSE_LMP_INCLUDE_DIR}")
target_link_libraries(lmpplugin "$ENV{JSE_LMP_LIB_PATH}")

