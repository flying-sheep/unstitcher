#!/bin/sh
jar cvmf MANIFEST.MF unstitcher.jar blocks.txt items.txt -C build net
zipmerge -i unstitcher.jar /usr/share/scala/lib/scala-library.jar
chmod +x unstitcher.jar