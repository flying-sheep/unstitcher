#!/bin/zsh
rm -rf build
mkdir -p build

scalac -feature -sourcepath src -d build src/**/*.scala