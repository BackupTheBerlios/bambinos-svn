#!/bin/sh

cd bin

java compiler.Parser ../src/examples/ Util2.java
java compiler.Parser ../src/examples/ Util.java
java compiler.Parser ../src/examples/ MainModule.java

java linker.Linker ../MainModule.obj

cd ..

