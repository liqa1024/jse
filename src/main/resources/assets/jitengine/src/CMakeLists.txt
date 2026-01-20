cmake_minimum_required(VERSION 3.15)
project(jitengine C)

set(CMAKE_C_STANDARD 11)

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()


set(SOURCE_FILES
    jse_clib_SimpleJIT.c)
add_library(jitengine SHARED ${SOURCE_FILES})

if(CMAKE_SYSTEM_NAME MATCHES "Windows")
    set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)
    set(BUILD_SHARED_LIBS ON)
endif()


# add jni
find_package(JNI)
if(JNI_FOUND)
    include_directories(${JNI_INCLUDE_DIRS})
    target_link_libraries(jitengine ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "No java development environment found")
endif()

