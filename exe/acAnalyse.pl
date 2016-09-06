#!/usr/bin/perl -w
$use = "acAnalyze.pl [-debug] -mol sdfName[.sdf,.oeb,...] -tab tabName\n"
      ."            -method lars -responseTag tag -nvar n\n"
      ."            -outR2File fName -log fName\n"
      ."\n";

$method="";
$inFile="";
$tabFile="";
$logFile="";
$outR2File="";
$responseTag="";
$nvar=0;
$debug=0;

while( $#ARGV >= 0 && $ARGV[0] =~ /^-/ )
{  if( $ARGV[0] eq "-debug" )
   {   $debug = 1;
       shift;
       next;
   }
   if( $ARGV[0] eq "-mol" && $#ARGV > 0 )
   {   $inFile = "$ARGV[1]";
       shift;shift;
       next;
   }
   if( $ARGV[0] eq "-tab" && $#ARGV > 0 )
   {   $tabFile = "$ARGV[1]";
       shift;shift;
       next;
   }
   if( $ARGV[0] eq "-log" && $#ARGV > 0 )
   {   $logFile = "$ARGV[1]";
       shift;shift;
       next;
   }
   if( $ARGV[0] eq "-method" && $#ARGV > 0 )
   {   $method = "$ARGV[1]";
       shift;shift;
       next;
   }
   if( $ARGV[0] eq "-nvar" && $#ARGV > 0 )
   {   $nvar = $ARGV[1];
       shift;shift;
       next;
   }
   if( $ARGV[0] eq "-outR2File" && $#ARGV > 0 )
   {   $outR2File = $ARGV[1];
       shift;shift;
       next;
   }
   if( $ARGV[0] eq "-responseTag" && $#ARGV > 0 )
   {   $responseTag = $ARGV[1];
       shift;shift;
       next;
   }

   $ARGV[0] =~ /^-..*/ && die $use;
}

$basePath=$0;
if($basePath !~ /.*\/ARISE\/.*/) { $basePath=`pwd`;}
chomp($basePath);
#$basePath =~ s/\/ARISE\/.*/\/ARISE/;

$method      || die "No method specified!\n$use";
$responseTag || die "No responseTag specified!\n$use";
$nvar        || die "Number of variables not given!\n$use";

$acHome = $ENV{'AC_HOME'};
$rFile =  " $acHome/R/$method.r";
-e $inFile   || die "mol file $inFile does not exist\n$use";
-e $tabFile  || die "tabFile $tabFile does not exist\n$use";
-e $rFile    || die "rFile $rFile does not exist\n$use";

$joinTag = "AC_NUMBER";
$nInputRows = `wc -l $tabFile`;
$nInputRows--;

system("sdfTagTool.csh -in $inFile -out .sdf -rename TITLE=$joinTag | sdfTagTool.csh -in .sdf -out .sdf -split '|' "
      ."-splitTag $joinTag -keepNumeric Y "
      ."-keep $joinTag"
      ." | sdfTagTool.csh -split '_' -splitTag $joinTag -keepNumeric Y -keep $joinTag -in .sdf -out .sdf |sdf2Tab.csh -in .sdf >tmp.$$.tab")
  == 0 || die $!;


system("mergeTabs.csh -pivot $joinTag -in1 $tabFile -in2 tmp.$$.tab " 
      ."-out tmp.2.$$.tab -headers Y")
  == 0 || die $!;

# to get to the tap-input file name include the follwing commands in your R
# file:
# args<-commandArgs()
# args<-args[((1:length(args))[args=="--args"]+1):length(args)]
# inFile<-args[1]
$cmd = "R -q --slave --no-readline --no-init-file --vanilla --args "
      ."-nvar $nvar -in tmp.2.$$.tab -responseTag $responseTag "
      ."-rmColumns $joinTag,SMILES -nInputRows $nInputRows "
      ."<$rFile ";
#warn $cmd;
$cmd = `$cmd`;
($qual,$r2,$nRow) = $cmd =~ /.*qual=([\.\w]+) R2=([\.\w]+) nRow=([\.\w]+)/s;

$d=0;
if(!$logFile)
{  ($d,$logFile) = $inFile =~ /(.*\/)*(.+?)(\.oeb\.gz|\.[^.]*)$/;
   $logFile .= ".log";
}
open(LOG, ">$logFile") || die "$!";
print LOG $cmd;
close(LOG);

if($outR2File)
{  open(R2, ">$outR2File") || die "$outR2File $!";
   print R2 "$qual\t$r2\t$nRow\n";
   close(R2);
}

$debug || unlink("tmp.$$.tab","tmp.2.$$.tab");