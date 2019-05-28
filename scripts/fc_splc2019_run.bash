#!/bin/bash

### Author:   Kai Ludwig
### Created:  2019-05-27
### Synopsis: Reproduce SPLC2019 testbed for SPLC 2019 artifact submission

# download prefix to ease automated cleanup
PROJ_PREFIX="SPLC_";
LOGPATTERN="FeatureCoPP_splc2019_*.0.log";
# concatenated statistic sections from  project logs end up here
LOGFILE_COMBINED="SPLC__LOGFILE_COMBINED.LOG";
# speed things a little up on github;-)
WGET_USER_AGENT='Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0';
# expected java major due to FeatureCoPP functionality
typeset -i EXP_JAVA_VERSION=8;
# testbed directory
PROJECT_DIR=$(pwd);
# involved binaries
declare -a REQ_BINARIES=(
    #asdfg # error test
    java
    bash
    grep
    sed
    awk
    cut
    find 
    wget
    tar
    gzip
    bzip2
    xz
    mkdir
    rm
    tail
# add further here
);
# used analysis tool
FEATURECOPP_RUN="java -jar FeatureCoPP/FeatureCoPP.jar --config=FeatureCoPP/conf.d/fc_splc2019artifactContrib.conf";

# used test systems
declare -A SPLC2019_SYSTEM_URLS=(
    [${PROJ_PREFIX}apache-2.4.9.tar.gz]='http://archive.apache.org/dist/httpd/httpd-2.4.9.tar.gz'
    [${PROJ_PREFIX}cpython-3.7.1rc1.tar.gz]='https://github.com/python/cpython/archive/v3.7.1rc1.tar.gz'
    [${PROJ_PREFIX}emacs-26.1.tar.gz]='https://ftp.fau.de/gnu/emacs/emacs-26.1.tar.gz'
    [${PROJ_PREFIX}gimp-2.9.8.tar.bz2]='https://ftp.fau.de/gimp/gimp/v2.9/gimp-2.9.8.tar.bz2'
    [${PROJ_PREFIX}git-2.19.0.tar.gz]='https://github.com/git/git/archive/v2.19.0.tar.gz'
    [${PROJ_PREFIX}glibc-2.9.tar.gz]='https://ftp.gnu.org/gnu/libc/glibc-2.9.tar.gz'
    [${PROJ_PREFIX}imagemagick-7.0.8-12.tar.gz]='https://github.com/ImageMagick/ImageMagick/archive/7.0.8-12.tar.gz'
    [${PROJ_PREFIX}libxml2-2.7.2.tar.gz]='http://xmlsoft.org/sources/libxml2-2.7.2.tar.gz'
    [${PROJ_PREFIX}lighttpd-1.4.50.tar.gz]='https://download.lighttpd.net/lighttpd/releases-1.4.x/lighttpd-1.4.50.tar.gz'
    [${PROJ_PREFIX}linux-4.10.4.tar.gz]='https://mirrors.edge.kernel.org/pub/linux/kernel/v4.x/linux-4.10.4.tar.gz'
    [${PROJ_PREFIX}mysql-8.0.12.tar.gz]='https://github.com/mysql/mysql-server/archive/mysql-8.0.12.tar.gz'
    [${PROJ_PREFIX}openldap-2.4.46.tar.gz]='http://mirror.eu.oneandone.net/software/openldap/openldap-release/openldap-2.4.46.tgz'
    [${PROJ_PREFIX}php-src-php-7.3.0RC2.tar.gz]='https://github.com/php/php-src/archive/php-7.3.0RC2.tar.gz'
    [${PROJ_PREFIX}postgresql-10.1.tar.gz]='https://ftp.postgresql.org/pub/source/v10.1/postgresql-10.1.tar.gz'
    [${PROJ_PREFIX}sendmail-8.12.11.tar.gz]='http://ftp.sendmail.org/sendmail.8.12.11.tar.gz'
    [${PROJ_PREFIX}subversion-1.10.2.tar.gz]='https://archive.apache.org/dist/subversion/subversion-1.10.2.tar.gz'
    [${PROJ_PREFIX}sylpheed-3.6.0.tar.gz]='http://sylpheed.sraoss.jp/sylpheed/v3.6/sylpheed-3.6.0.tar.gz'
    [${PROJ_PREFIX}vim-8.1.tar.bz2]='ftp://ftp.vim.org/pub/vim/unix/vim-8.1.tar.bz2'
    [${PROJ_PREFIX}xfig-3.2.7a.tar.xz]='https://vorboss.dl.sourceforge.net/project/mcj/xfig-3.2.7.tar.xz'
);


function usage() {
    echo "$(basename $0) splc | [config] | purge";
}

function purgeProjDir() {
    local item=$1;
    rm --force --recursive $item;
}

function testBashVersion() {
    # 4.4.12(3)-release
    if (( ${BASH_VERSION%%.*} < 4 )); then
	echo "Bash version >= 4.x required (found: ${BASH_VERSION})! Refusing ...";
	exit 1;
    fi
}

function testJavaVersion() {
    local java_version=$(java -version 2>&1 | grep "version" | awk '{print $3}' | sed 's/"//g');
    typeset -i java_min_version=$(echo $java_version | cut --delimiter='.' --fields=2);
    if (($java_min_version < $EXP_JAVA_VERSION)); then
	echo "Java version >= 1.8.x required (found: ${java_version})! Refusing ...";
    fi
}

function testForBinary() {
    local binary=$1;    
    if ! [ -x "$(command -v $binary)" ]; then
	echo "Error: $binary is not in $PATH! Refusing..." >&2
	exit 1
    fi
}
function testReqBinaries() {
    # array as name ref
    local -n binaries=$1;
    for binary in "${binaries[@]}";
    do
	echo -ne "Searching $binary ...";
	testForBinary "$binary";
	echo "ok";
    done
}

function extractArchive() {
    local archive=$1;
    local dst=$2;
    echo "Extracting $archive to $dst ...";
    tar --extract --skip-old-files --auto-compress --file=$archive --one-top-level=$dst;
}

function downloadSystems() {
    # associative passed by name ref
    local -n urls=$1;
    typeset -i numOfSystems=${#urls[@]};
    typeset -i count=0;
    for key in "${!urls[@]}";
    do
	#echo "$key -> ${urls[$key]}";
	echo "###";
	echo "Downloading system $((++count))/$numOfSystems ...";
	local dstdir="${key/.tar.*/}";
	# exist and > 0 byte?
	if [ -s "$key" ]; then
	    echo "$key already exists! Skipping download...";
	else
	    wget --continue --verbose --show-progress ${urls[$key]} --output-document=$key \
		 --user-agent="$WGET_USER_AGENT";
	fi
	extractArchive "$(basename $key)" "$dstdir";
    done
}

function runProgram() {
    local cmdline=$1;
    eval $cmdline;
}

function concatLogs() {
    local logdir=$1;
    local logpattern=$2;
    local logfile_combined=$3;
    ### remove old log since we append
    rm --force $logfile_combined;
    for logfile in $(find $logdir -type f -name "$logpattern" 2>/dev/null);
    do
	echo "### $logfile ###" >> $logfile_combined;
	tail -31 $logfile >> $logfile_combined;
    done
}

### MAIN
testBashVersion;

if (($# != 1)); then
    echo "Invalid number of arguments! Refusing...";
    usage;
    exit 1;
fi

testReqBinaries REQ_BINARIES;
testJavaVersion;

case $1 in
    "purge")
	echo "Purging $PROJECT_DIR";
	# delete anything prefixed with SPLC_
	purgeProjDir "${PROJ_PREFIX}* *.log";
	;;
    "splc")
	echo "Starting SPLC2019 testbed...";
	### before processing
	if [ ! -d "$projDir" ]; then
	    mkdir --verbose --parents $PROJECT_DIR;
	fi
	cd $PROJECT_DIR;
	downloadSystems SPLC2019_SYSTEM_URLS;
	### processing
	runProgram "$FEATURECOPP_RUN";
	### after processing
	concatLogs "$PROJECT_DIR" "$LOGPATTERN" "$LOGFILE_COMBINED"
	;;
    *)
	echo "Using configuration $1 ...";
	;;
esac
