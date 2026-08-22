cmake_minimum_required(VERSION 3.22.1)

project(aprendia_llama LANGUAGES C CXX)

include(FetchContent)

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_BLAS OFF CACHE BOOL "" FORCE)
set(GGML_CUDA OFF CACHE BOOL "" FORCE)
set(GGML_METAL OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)

FetchContent_Declare(
        llama_cpp
        GIT_REPOSITORY https://github.com/ggml-org/llama.cpp.git
        GIT_TAG b21e4de74567f5eef213765c9476a843c2e43f0d
)
FetchContent_MakeAvailable(llama_cpp)

add_library(aprendia_llama SHARED aprendia_llama.cpp)

target_compile_features(aprendia_llama PRIVATE cxx_std_17)
target_include_directories(aprendia_llama PRIVATE ${llama_cpp_SOURCE_DIR}/include)
target_link_libraries(aprendia_llama PRIVATE llama android log)
