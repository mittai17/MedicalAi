cmake_minimum_required(VERSION 3.22.1)
project("swasthai_ai")

add_library(swasthai_ai SHARED ai_native.cpp)

set_target_properties(swasthai_ai PROPERTIES
    CXX_STANDARD 17
    CXX_STANDARD_REQUIRED ON
)

find_library(log-lib log)
target_link_libraries(swasthai_ai ${log-lib})