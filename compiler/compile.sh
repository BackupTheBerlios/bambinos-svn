#!/bin/bash

currentDir=`pwd`

java ${currentDir}/bin/compiler/Parser ${currentDir}/src/examples/Util.java
javac ./bin/compiler/Parser.java src/examples/MainModule.java

