#!/bin/csh -f
#

set main=autocorrelator.apps.SDFGroovy

# needs special treatment to support .grvy extention

set script=$0
if( "$script" !~ "/*" ) set script=$PWD/$script
set installDir=$script:h
set libDir=$installDir:h/lib

set OE_DIR=~smdi/prd/oechem
set oeLibDir=$OE_DIR/lib/$MODULEPLATFORM
if $?nonomatch  then
   set natDir=$oeLibDir/*-native-libs/ >& /dev/null
else
   set nonomatch=1
   set natDir=$oeLibDir/*-native-libs/ >& /dev/null
   unset nonomatch
endif

set cp="$libDir/autocorrelator.jar"
set cp="${cp}:$AESTEL_DIR/../bin/lib/*"
set cp="${cp}:$oeLibDir/*"
if( $?natDir ) set cp="${cp}:$natDir"

if( $#argv > 0 && "$1" !~ "-*" ) exec java -cp "$cp" $main -f $*:q

exec java -Xmx30G -cp "$cp" $main $*:q
