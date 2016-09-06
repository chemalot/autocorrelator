#!/bin/csh -f
#

set main=autocorrelator.ac.AutoCorrelator

set script=$0
if( "$script" !~ "/*" ) set script=$PWD/$script
set installDir=$script:h
set libDir=$installDir:h/lib

set OE_DIR=~smdi/prd/oechem

set cp=$libDir/autocorrelator.jar:${libDir}/jdom.jar
set cp={$cp}:$OE_DIR/lib/openeye.oechem.jar
setenv LD_LIBRARY_PATH $OE_DIR/lib/$MODULEPLATFORM

java -cp $cp $main $*:q
