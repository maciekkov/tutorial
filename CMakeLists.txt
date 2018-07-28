cmake_minimum_required(VERSION 3.1)

project(haxer CXX)

set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH}/cmake ${CMAKE_MODULE_PATH})

set(CMAKE_CXX_FLAGS " -Wall - Werror -Wpedantic -std=c++11")
if(NEW_VARIABLE)
	message(STATUS "WE MADE IT")
endif()
#add_subdirectory(haxer)
#add_subdirectory(utest utest EXCLUDE_FROM_ALL)
