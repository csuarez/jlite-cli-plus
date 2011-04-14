#!/bin/bash

USAGE=\
'
Usage (to display jar files):
  listJarsForClasspath.sh directory1 directory2 ...
  
Usage (to add jar files to CLASSPATH):
  CLASSPATH=$CLASSPATH:$(listJarsForClasspath.sh directory1 directory2 ...)

For each directory on the command line, this script lists all of the .jar
and .zip files in that directory.  The file names are separated by the
path separator character (: or ; depending on platform).
'

if [ $# -lt 1 ]; then
    echo -e "$USAGE"
    exit 1
fi

case $(uname) in
    CYGWIN*) PATHSEP=";"
        ;;
    *) PATHSEP=":"
        ;;
esac

JARLIST=""

addToJarList ()
{
    # Params: 1 - directory
    #         2 - archive file extension (jar, zip)
    for arcfile in "$1"/*."$2"; do
        if [ -f "$arcfile" ]; then
            # echo "$arcfile"

            if [ ! -z "$JARLIST" ]; then
                JARLIST="$JARLIST$PATHSEP"
            fi

            JARLIST="$JARLIST$arcfile"
        fi
    done
}

for dir in "$@"; do

    if [ ! -e "$dir" ]; then
        echo "Does not exist: $dir"
    elif [ ! -d "$dir" ]; then
        echo "Not a directory: $dir"
    else
        addToJarList "$dir" "jar"
        addToJarList "$dir" "zip"
    fi
done

if [ ! -z "$JARLIST" ]; then
    echo "$JARLIST"
fi
