cmake_minimum_required(VERSION 3.22)
project(nuraya_ios_speech VERSION 1.7.6 LANGUAGES C CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
# Build the same vendored engine as Android as a library. Its standalone
# JavaScript packaging files are intentionally absent from the vendored tree.
add_subdirectory(../../app/src/main/cpp/vendor/whisper whisper)
