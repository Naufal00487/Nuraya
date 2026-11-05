cmake_minimum_required(VERSION 3.22.1)
project(nuraya_quran_ai LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
# The distributed APK is a debug-signed build, but Whisper's CPU kernels must
# still be optimized. AGP's Debug configuration otherwise emits unoptimized
# kernels while our host Release checks use optimized code. Keep debug symbols
# and baseline CPU compatibility; do not enable fast-math or native CPU flags.
if(ANDROID)
    add_compile_options(-O3)
endif()
set(BUILD_SHARED_LIBS OFF CACHE BOOL "Static inference dependencies" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(WHISPER_CURL OFF CACHE BOOL "No network access in inference" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "Portable Android CPUs" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "No extra OpenMP runtime" FORCE)
set(GGML_BACKEND_DL OFF CACHE BOOL "No runtime-loaded plugins" FORCE)
set(GGML_CPU_ALL_VARIANTS OFF CACHE BOOL "" FORCE)
set(GGML_AVX OFF CACHE BOOL "Baseline emulator CPUs" FORCE)
set(GGML_AVX2 OFF CACHE BOOL "" FORCE)
set(GGML_FMA OFF CACHE BOOL "" FORCE)
set(GGML_F16C OFF CACHE BOOL "" FORCE)
set(GGML_SSE42 OFF CACHE BOOL "" FORCE)
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(GGML_CPU_ARM_ARCH "armv8-a" CACHE STRING "Baseline ARM64" FORCE)
endif()

add_subdirectory(vendor/whisper)
add_library(nuraya_quran_ai SHARED quran_whisper_jni.cpp)
target_link_libraries(nuraya_quran_ai PRIVATE whisper)
if(NOT ANDROID)
    find_package(JNI REQUIRED)
    target_include_directories(nuraya_quran_ai PRIVATE ${JNI_INCLUDE_DIRS})
endif()
# Android 15+ devices can use 16 KiB pages. NDK r27's opt-in flag is also set by Gradle.
target_link_options(nuraya_quran_ai PRIVATE "-Wl,-z,max-page-size=16384")

