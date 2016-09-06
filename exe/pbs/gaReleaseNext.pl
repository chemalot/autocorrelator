#!/usr/bin/perl -w

$usage =
 "releaseNext.pl -prefix prefix -cycle n -nTotal tot -percentLeft prc\n"
."   Will release the queJob which runs the next GA cycle if less than prc\n"
."   jobs are still running in the queue.\n";

$prefix="";
$cycle="";
$percentLeft=0;
$nTotal=0;

while( $#ARGV >= 0 && $ARGV[0] =~ /^-/ )
{   if( $ARGV[0] eq "-prefix" && $#ARGV > 0 )
    {  shift;
       $prefix = shift;
       next;
    }elsif( $ARGV[0] eq "-cycle" && $#ARGV > 0 )
    {  shift;
       $cycle = shift;
       next;
    }elsif( $ARGV[0] eq "-nTotal" && $#ARGV > 0 )
    {  shift;
       $nTotal = shift;
       next;
    }elsif( $ARGV[0] eq "-percentLeft" && $#ARGV > 0 )
    {  shift;
       $percentLeft = shift;
       next;
    }else { die "Unknown parameter:$ARGV[0]\n$usage"; }
}

$#ARGV < 0   || die "unknown arguments: " . join(" ",@ARGV) . "\n$usage";
$prefix      || die "No prefix given\n$usage";
$nTotal      || die "nTotal size not given\n$usage";
$percentLeft || die "percentLeft not given\n$usage";

$fullPrefix = $prefix;
$cycle && ($fullPrefix = "${prefix}_$cycle");


$nRunning = 0;
open(QSTAT, "qstat|") || die $!;
while($_=<QSTAT>)
{  @_ = split;
   $#_ > 4 || next;
   $_[2] =~ /^$fullPrefix/ || next;
   $_[4] !~ /^h/ && $nRunning++;  # not on hold
}
close(QSTAT);

warn "nRunning = $nRunning";
$nRunning == 0 && exit(0);
# decrement under assumption that this is run in the queue 
# and counts for one instance
$nRunning--;

warn "nrun = $nRunning ". $nRunning/$nTotal . " " . $percentLeft/100 . "\n";
if( $nRunning/$nTotal > $percentLeft/100 ) { exit(0); }

$cmd = "qalter -hold_jid 0 ${prefix}${cycle}Next";
system($cmd);
