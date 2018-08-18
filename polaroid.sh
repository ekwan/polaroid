#!/bin/bash

rm -f src/Polaroid.class
javac src/Polaroid.java
if [ $? -eq 1 ] ; then
    exit
fi

java -cp src Polaroid

