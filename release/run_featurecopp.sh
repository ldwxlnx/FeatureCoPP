#!/bin/bash

function usage() {
    echo "$(basename $0) <config-file> | help";
    echo "Runs FeatureCoPP based on configuration <config-file> or shows FeatureCoPP's help screen.";
}

if(($# != 1)); then
    usage;
    exit 0;
fi

FC_ARGS="";

if [[ "$1" == "help" ]];
then
    FC_ARGS="--help";
else
    FC_ARGS="--config=${1}";
fi

java -jar FeatureCoPP.jar $FC_ARGS;
