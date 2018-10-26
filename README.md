# Polaroid

*A utility for making images of genomic regions using IGV.*

## Introduction

*Polaroid* is a utility that takes a list of genomic loci and generates pileup images for each location.  This can be useful for manually vetting called mutation sites or generating training sets.

## Requirements

You must have Java (JDK 7 or later) and IGV installed.
The `polaroid.sh` script assumes a Linux-like environment
(OS X, Cygwin, or actual Linux/Unix).

## Installation

To install this software, change to the desired installation directory and type:

`git clone https://github.com/ekwan/polaroid.git`

## Expected Input/Output

*Input*: A set of `.bam` files and genomic locations.

*Output*: A set of `.png` files showing the IGV pileup for each genomic location.

## Instructions

1. In IGV, go to View...Preferences...Advanced.   Check "enable port" to allow Polaroid
to send commands to IGV.  The default port is 60151.

2. Open `polaroid.config` in any text editor.

* Set `IGV_PORT` to your choice from step 1.  `IGV_IP` should not need changing.
* Add one `BAM` line for each `.bam` to be processed.  Fully qualified path names must be given.
* Set `SNAPSHOT_DIRECTORY` to the desired destination for the `.png` files.  Any existing `.png` files in this directory will be deleted.
* Add one line for each genomic location of interest.  Locations must be of the form `chr1:23456` (one per line please).

3. Run the script: `./polaroid.sh`.  (IGV must be open already.)

## Known Issues

Sometimes, there may be `.bam` files missing from some of the `.png` files.  Other times,
the bases might not be sorted.  In this case, increase the `DELAY` parameter in the
configuration file.

### Authors

Written by: Emir Hamzaogullari, Eugene Kwan, and Michael Lawrence
Massachusetts General Hospital, 2018

