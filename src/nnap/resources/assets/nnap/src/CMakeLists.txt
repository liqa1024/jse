cmake_minimum_required(VERSION 3.15)
project(nnap CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()


set(SOURCE_FILES
    jsex_nnap_nn_FeedForward.cpp
    jsex_nnap_basis_SphericalChebyshev.cpp
    jsex_nnap_basis_Chebyshev.cpp)
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
    message(FATAL_ERROR "No java development environment found")
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


# native optimize
option(JSE_OPTIM_MAX "Use the most aggressive optimization settings (like AVX512)" OFF)
option(JSE_OPTIM_BASE "Use basic optimization settings" ON)
option(JSE_OPTIM_COMPAT "Use the most compatible optimization settings" OFF)

if(CMAKE_CXX_COMPILER_ID MATCHES "MSVC")
    if(JSE_OPTIM_MAX)
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /fp:fast /arch:AVX512")
    elseif(JSE_OPTIM_BASE)
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /fp:fast /arch:AVX2")
    elseif(JSE_OPTIM_COMPAT)
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /fp:fast")
    endif()
endif()

if(CMAKE_CXX_COMPILER_ID MATCHES "GNU")
    if(JSE_OPTIM_MAX)
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffast-math -march=native -mavx512f -mfma")
    elseif(JSE_OPTIM_BASE)
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffast-math -march=native -mfma")
    elseif(JSE_OPTIM_COMPAT)
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffast-math")
    endif()
endif()

