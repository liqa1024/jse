cmake_minimum_required(VERSION 3.14)
project(jep C)

set(CMAKE_C_STANDARD 11)

if (NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()


include_directories(src/main/c/Include)

set(SOURCE_FILES
    src/main/c/Jep/java_access/AnnotatedElement.c
    src/main/c/Jep/java_access/ArrayList.c
    src/main/c/Jep/java_access/AutoCloseable.c
    src/main/c/Jep/java_access/BigInteger.c
    src/main/c/Jep/java_access/Boolean.c
    src/main/c/Jep/java_access/Buffer.c
    src/main/c/Jep/java_access/Byte.c
    src/main/c/Jep/java_access/ByteBuffer.c
    src/main/c/Jep/java_access/ByteOrder.c
    src/main/c/Jep/java_access/Character.c
    src/main/c/Jep/java_access/CharBuffer.c
    src/main/c/Jep/java_access/Class.c
    src/main/c/Jep/java_access/ClassLoader.c
    src/main/c/Jep/java_access/Collection.c
    src/main/c/Jep/java_access/Collections.c
    src/main/c/Jep/java_access/Comparable.c
    src/main/c/Jep/java_access/Double.c
    src/main/c/Jep/java_access/DoubleBuffer.c
    src/main/c/Jep/java_access/Entry.c
    src/main/c/Jep/java_access/Executable.c
    src/main/c/Jep/java_access/Field.c
    src/main/c/Jep/java_access/Float.c
    src/main/c/Jep/java_access/FloatBuffer.c
    src/main/c/Jep/java_access/HashMap.c
    src/main/c/Jep/java_access/IntBuffer.c
    src/main/c/Jep/java_access/Integer.c
    src/main/c/Jep/java_access/Iterable.c
    src/main/c/Jep/java_access/Iterator.c
    src/main/c/Jep/java_access/JepException.c
    src/main/c/Jep/java_access/JPyCallable.c
    src/main/c/Jep/java_access/JPyMethod.c
    src/main/c/Jep/java_access/JPyObject.c
    src/main/c/Jep/java_access/List.c
    src/main/c/Jep/java_access/Long.c
    src/main/c/Jep/java_access/LongBuffer.c
    src/main/c/Jep/java_access/Map.c
    src/main/c/Jep/java_access/Member.c
    src/main/c/Jep/java_access/Method.c
    src/main/c/Jep/java_access/Modifier.c
    src/main/c/Jep/java_access/Number.c
    src/main/c/Jep/java_access/Object.c
    src/main/c/Jep/java_access/Proxy.c
    src/main/c/Jep/java_access/Short.c
    src/main/c/Jep/java_access/ShortBuffer.c
    src/main/c/Jep/java_access/String.c
    src/main/c/Jep/java_access/Throwable.c
    src/main/c/Jep/python/invocationhandler.c
    src/main/c/Jep/python/jep_object.c
    src/main/c/Jep/python/jpycallable.c
    src/main/c/Jep/python/pypointer.c
    src/main/c/Jep/convert_j2p.c
    src/main/c/Jep/convert_p2j.c
    src/main/c/Jep/jep.c
    src/main/c/Jep/jep_exceptions.c
    src/main/c/Jep/jep_numpy.c
    src/main/c/Jep/jep_util.c
    src/main/c/Jep/maininterpreter.c
    src/main/c/Jep/pyembed.c
    src/main/c/Objects/pyjarray.c
    src/main/c/Objects/pyjautocloseable.c
    src/main/c/Objects/pyjbuffer.c
    src/main/c/Objects/pyjclass.c
    src/main/c/Objects/pyjcollection.c
    src/main/c/Objects/pyjconstructor.c
    src/main/c/Objects/pyjfield.c
    src/main/c/Objects/pyjiterable.c
    src/main/c/Objects/pyjiterator.c
    src/main/c/Objects/pyjlist.c
    src/main/c/Objects/pyjmap.c
    src/main/c/Objects/pyjmethod.c
    src/main/c/Objects/pyjmonitor.c
    src/main/c/Objects/pyjmultimethod.c
    src/main/c/Objects/pyjnumber.c
    src/main/c/Objects/pyjobject.c
    src/main/c/Objects/pyjtype.c)
add_library(jep SHARED ${SOURCE_FILES})


if(CMAKE_SYSTEM_NAME MATCHES "Windows")
    set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)
    set(BUILD_SHARED_LIBS ON)
endif()


# add jni
find_package(JNI)
if(JNI_FOUND)
    include_directories(${JNI_INCLUDE_DIRS})
    target_link_libraries(jep ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "No java development environment found")
endif()


# add jniutil (jstring to char *, fopen)
include_directories("$ENV{JSE_JNIUTIL_INCLUDE_DIR}")


# add mimalloc
option(JSE_USE_MIMALLOC "Use mimalloc to accelerate `malloc` operation" ON)
if(JSE_USE_MIMALLOC)
    add_definitions(-DUSE_MIMALLOC)
    
    include_directories("$ENV{JSE_MIMALLOC_INCLUDE_DIR}")
    target_link_libraries(jep "$ENV{JSE_MIMALLOC_LIB_PATH}")
endif()


# add python
option(JSE_JEP_NUMPY_SUPPORT "Numpy support (force)" ON)
find_package(Python3 COMPONENTS Development)
if(Python3_Development_FOUND)
    add_definitions(-DPYTHON_LDLIBRARY="${Python3_LIBRARIES}")
    
    include_directories(${Python3_INCLUDE_DIRS})
    target_link_libraries(jep Python3::Python)
    
    # add numpy
    if(JSE_JEP_NUMPY_SUPPORT)
        find_package(Python3 COMPONENTS Development NumPy)
        if(Python3_NumPy_FOUND)
            add_definitions(-DJEP_NUMPY_ENABLED=1)
            
            include_directories(${Python3_NumPy_INCLUDE_DIRS})
            target_link_libraries(jep Python3::NumPy)
        else()
            message(FATAL_ERROR "numpy development environment not found")
        endif()
    else()
        add_definitions(-DJEP_NUMPY_ENABLED=0)
    endif()
else()
    message(FATAL_ERROR "No python3 development environment found")
endif()


# jep defines
add_definitions(-DPACKAGE=jep)
add_definitions(-DUSE_DEALLOC=1)
add_definitions(-DVERSION="4.2.0")
if(CMAKE_SYSTEM_NAME MATCHES "Windows")
    add_definitions(-DWIN32=1)
    add_definitions(-D_CRT_SECURE_NO_WARNINGS=1)
endif()

# auto-convert BigDecimal internal
add_definitions(-DAUTOCONVERT_BIGDECIMAL)

