#!/usr/bin/perl -w

$usage =
 "ga.pl [-debug] [-cycle cycle] -maxCycle mCyc -prefix prefix -queOpts 'opts'\n"
."       -mutationProb 0.1 -breedingPool 20 -childPool 15\n"
."       -xmlFile xmlFileName -directory dir\n"
."   childPool: number of children to create.\n"
."   breedingPool: number of parents from which to create children.\n"
."   cycle: if 0 the default autocorelator is run\n";

$debug="";
$prefix="";
$cycle=0;
$maxCyc=0;
$xmlFile="";
$mutProb="";
$breedPool="";
$childPool="";
$queOpts="";
$xmlFile="";

use Cwd;
my $dir = getcwd;

print $dir . "\n";

while( $#ARGV >= 0 && $ARGV[0] =~ /^-/ )
{   if( $ARGV[0] eq "-prefix" && $#ARGV > 0 )
    {  shift;
       $prefix = shift;
       next;
    }elsif( $ARGV[0] eq "-cycle" && $#ARGV > 0 )
    {  shift;
       $cycle = shift;
       next;
    }elsif( $ARGV[0] eq "-maxCycle" && $#ARGV > 0 )
    {  shift;
       $maxCyc = shift;
       next;
    }elsif( $ARGV[0] eq "-mutationProb" && $#ARGV > 0 )
    {  shift;
       $mutProb = shift;
       next;
    }elsif( $ARGV[0] eq "-breedingPool" && $#ARGV > 0 )
    {  shift;
       $breedPool = shift;
       next;
    }elsif( $ARGV[0] eq "-childPool" && $#ARGV > 0 )
    {  shift;
       $childPool = shift;
       next;
    }elsif( $ARGV[0] eq "-queOpts" && $#ARGV > 0 )
    {  shift;
       $queOpts = shift;
       next;
    }elsif( $ARGV[0] eq "-xmlFile" && $#ARGV > 0 )
    {  shift;
       $xmlFile = shift;
       next;
    }elsif( $ARGV[0] eq "-directory" && $#ARGV > 0 )
    {  shift;
       $dir = shift;
       next;
    }elsif( $ARGV[0] eq "-debug")
    {   $debug = "-debug";
        shift;
    }else { die "Unknown parameter:$ARGV[0]\n$usage"; }
}
$acHome = $ENV{'AC_HOME'};

$dir || die "launch directory not given\n$usage";
$xmlFile || die "xmlFile not found\n$usage";
$prefix  || die "No prefix given\n$usage";
$maxCyc  || die "No maxCycle given\n$usage";
$mutProb   eq "" && die "mutationProb not given\n$usage";
$breedPool eq "" && die "breedingPool size not given\n$usage";
$childPool eq "" && die "childPool size not given\n$usage";

$options = "$debug -maxCycle $maxCyc -prefix $prefix -mutationProb $mutProb "
         ." -breedingPool $breedPool -childPool $childPool "
         ." -directory $dir";
chdir $dir || die "can not change to directory $dir";
if(!$cycle)  # initial run
{  # run autocoralator first xmlFile should create the initial random runs
   if ($queOpts eq "") {
     $cmd="autoCorrelator.csh $debug -prefix $prefix $xmlFile";
   }
   else
   {
    $cmd="autoCorrelator.csh $debug -queOpts '$queOpts' -prefix $prefix $xmlFile";
   }
   system($cmd);
   sleep(3);
   
   #queue job which queues the first GA step
   if ($queOpts eq "") {
   $cmd = "qsub -N ${prefix}1Next -hold_jid '${prefix}*' "
         ." $acHome/exe/ga.csh -cycle 1 $options -xmlFile $xmlFile";
   } else {
   $cmd = "qsub -N ${prefix}1Next -hold_jid '${prefix}*' $queOpts "
         ." $acHome/exe/ga.csh -cycle 1 $options -xmlFile $xmlFile";
   }
   print $cmd . "\n";
   system($cmd);
   exit(0);
}


#GA cycle
# queue computational runs for this step
if ($queOpts eq "") {
$cmd = "$acHome/exe/gaStep.csh $debug -mutationProb $mutProb -breedingPool $breedPool "
      ."-childPool $childPool -prefix $prefix "
      ." -xmlFile $xmlFile -cycle $cycle";
} else {
$cmd = "$acHome/exe/gaStep.csh $debug -mutationProb $mutProb -breedingPool $breedPool "
      ."-childPool $childPool -prefix $prefix -queOpts '$queOpts' "
      ." -xmlFile $xmlFile -cycle $cycle";
}
print $cmd . "\n";
system($cmd);
sleep(3);

if($cycle >= $maxCyc) { exit(0); }

# queue this script again to run next step
if ($queOpts eq "") {
  $cmd =  "qsub -N ${prefix}${cycle}Next -hold_jid '${prefix}_${cycle}*' ";
} else {
  $cmd =  "qsub -N ${prefix}${cycle}Next -hold_jid '${prefix}_${cycle}*' $queOpts ";
}

$cycle++;
$cmd .= " $acHome/exe/ga.csh -cycle $cycle $options -xmlFile $xmlFile";
print $cmd . "\n";
system($cmd);