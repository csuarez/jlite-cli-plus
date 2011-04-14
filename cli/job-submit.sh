#!/bin/sh
JLITE_CLI_DIR=`dirname "$(cd ${0%/*} && echo $PWD/${0##*/})"`
export JLITE_HOME=${JLITE_CLI_DIR%/*}

java -classpath $JLITE_HOME:$JLITE_HOME/bin:$JLITE_HOME/lib/jlite.jar:$($JLITE_HOME/cli/list-jars.sh $JLITE_HOME/lib/external):$($JLITE_HOME/cli/list-jars.sh $JLITE_HOME/lib/glite) jlite.cli.JobSubmit $@
