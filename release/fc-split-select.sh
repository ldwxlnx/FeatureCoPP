#!/bin/bash

function usage() {
    echo "$(basename $0) <inputdir> <java-regex>"
}


if (($# != 2)); then
    usage
    exit 0
fi

if [ ! -d "$1" ]; then
    echo "Input directory $1 does not exist! Refusing...";
    exit 1;
fi


java -jar FeatureCoPP.jar --split "$1" "$2"