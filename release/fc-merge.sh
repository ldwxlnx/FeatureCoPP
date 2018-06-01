#!/bin/bash

function usage() {
    echo "$(basename $0) <inputdir>_split"
}


if (($# != 1)); then
    usage
    exit 0
fi

if [ ! -d "$1" ]; then
    echo "Input directory $1 does not exist! Refusing...";
    exit 1;
fi


java -jar FeatureCoPP.jar --merge "$1"