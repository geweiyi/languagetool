#!/bin/bash

if [ $# -lt 2 ]
then
  echo "Helps building parts of LanguageTool - for a complete build, run mvn directly"
  echo "Usage: `basename $0` <project> <goals...>"
  echo "Examples:"
  echo "  ./`basename $0` languagetool-standalone clean package (will package the standalone module)"
  echo "  ./`basename $0` en clean test (will test the English module)"
  exit 1
fi

MODULE=$1

if [ \! -d $MODULE ]
then
  MODULE="languagetool-language-modules/$MODULE"
fi

COMMAND="mvn --projects $MODULE --also-make ${@:2}"
echo "Running: $COMMAND"

$COMMAND