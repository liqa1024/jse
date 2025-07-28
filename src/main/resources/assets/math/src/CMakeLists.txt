cmake_minimum_required(VERSION 3.15)
project(math C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 11)

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()


set(SOURCE_FILES
    jse_math_operation_ARRAY_Native.c)
add_library(math SHARED ${SOURCE_FILES})

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
    target_link_libraries(math ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "No java development environment found")
endif()


# add jniutil
include_directories("$ENV{JSE_JNIUTIL_INCLUDE_DIR}")


# native optimize
option(JSE_OPT_MAX "Use the most aggressive optimization settings (like AVX512)" OFF)
option(JSE_OPT_BASE "Use basic optimization settings" ON)
option(JSE_OPT_COMPAT "Use the most compatible optimization settings" OFF)

if(CMAKE_C_COMPILER_ID MATCHES "MSVC")
    if(JSE_OPT_MAX)
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} /fp:fast /arch:AVX512")
    elseif(JSE_OPT_BASE)
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} /fp:fast /arch:AVX2")
    elseif(JSE_OPT_COMPAT)
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} /fp:fast")
    endif()
endif()

if(CMAKE_CXX_COMPILER_ID MATCHES "MSVC")
    if(JSE_OPT_MAX)
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /fp:fast /arch:AVX512")
    elseif(JSE_OPT_BASE)
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /fp:fast /arch:AVX2")
    elseif(JSE_OPT_COMPAT)
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /fp:fast")
    endif()
endif()

if(CMAKE_C_COMPILER_ID MATCHES "GNU")
    if(JSE_OPT_MAX)
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -ffast-math -march=native -mavx512f -mfma")
    elseif(JSE_OPT_BASE)
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -ffast-math -march=native -mfma")
    elseif(JSE_OPT_COMPAT)
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -ffast-math")
    endif()
endif()

if(CMAKE_CXX_COMPILER_ID MATCHES "GNU")
    if(JSE_OPT_MAX)
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffast-math -march=native -mavx512f -mfma")
    elseif(JSE_OPT_BASE)
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffast-math -march=native -mfma")
    elseif(JSE_OPT_COMPAT)
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffast-math")
    endif()
endif()

