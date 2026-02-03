cmake_minimum_required(VERSION 3.18)
project(cudacore LANGUAGES CXX CUDA)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CUDA_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CUDA_STANDARD_REQUIRED ON)

# MSVC hacks for cuda
if(MSVC)
    add_compile_options(-Xcompiler "/wd4819")
endif()

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()

set(SOURCE_FILES
    jse_gpu_CudaCore.cpp)
add_library(cudacore SHARED ${SOURCE_FILES})

if(CMAKE_SYSTEM_NAME MATCHES "Windows")
    set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)
    set(BUILD_SHARED_LIBS ON)
endif()


# add jni
find_package(JNI)
if(JNI_FOUND)
    include_directories(${JNI_INCLUDE_DIRS})
    target_link_libraries(cudacore ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "No java development environment found")
endif()


# cuda additional setting
find_package(CUDAToolkit REQUIRED)
target_link_libraries(cudacore CUDA::cudart)
set_target_properties(cudacore PROPERTIES CUDA_ARCHITECTURES OFF)
