cmake_minimum_required(VERSION 3.15)
project(nnap2 CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()

set(SOURCE_FILES
    nnap_interface.cpp)
add_library(nnap2 SHARED ${SOURCE_FILES})

if(CMAKE_SYSTEM_NAME MATCHES "Windows")
    set(BUILD_SHARED_LIBS ON)
endif()

set_target_properties(nnap2 PROPERTIES
    C_VISIBILITY_PRESET hidden
    CXX_VISIBILITY_PRESET hidden
    VISIBILITY_INLINES_HIDDEN YES
)

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

