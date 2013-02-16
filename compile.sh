#!/bin/zsh
rm -rf build
mkdir -p build

scalac -sourcepath src -d build src/**/*.scala