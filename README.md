# Purpose

A project to display faction based statistics for Elite Dangerous
on the command line.

# Usage

## Prerequisite

You must have java installed and available on the command line. Please follow java installation
instructions for this at https://java.com/en/download/help/download_options.xml

## Running stats

To run, use

    # Unix type systems
    ./gradlew stats -q -Pargs="-f 'The Order of Mobius'"

    # Windows
    gradlew stats -q -Pargs="-f 'The Order of Mobius'"

Also supports running a single system stats

    # gradlew stats -q -Pargs="-s 'Exioce'"

This will install gradle as required and run the application task to print the statistics out.

The options are:

    -f <faction-name>  # Display all systems for named faction. Use quotes if the name has spaces in it
    -t YYYYMMDD        # override the date to display (default to latest in EDSM history)
    -a                 # show all factions in system
                       #   The default without specifying the flag is to show up to the named faction,
                       #   with a minimum of top 3
    -s <system-name>   # show information only for one system
    -d                 # additional debug output
    -h                 # help output


Example output

    Apathaam [3,506,390]                      SEP-18 SEP-17 SEP-15 SEP-11
    The Order of Mobius            None        57.84  -0.16  14.79  11.60 Boom
    Apathaam Brothers              Boom        14.98   0.88  -0.60   8.28
    Apathaam Patron's Principles   None        11.69  -1.31  -7.59  -3.73 Outbreak
    
    # ... other systems
    
    Leading/Trailing information for The Order of Mobius
    Apathaam              [SEP-18] leads by    42.86  -1.04  15.38   3.32
    # ... other systems

## Showing systems within a range of another system

You can also now find all the systems within given range of a named system with:

    ./gradlew within -q -Pargs="-s 'Exioce' -r 10.0"

    system               range    fc pf
    -----------------------------------
    Exioce               0.000     7  1
    NLTT 7789            8.300     7  0
    Azrael               8.426     8  1

Optionally add '-m <num>' to set maximum number of factions as an additional filter, e.g. '-m 7' would hide Azrael in the above output.

The column fc is 'Faction count' in the system, 'pf' is the number of player factions in the system.

# Data downloads

## EDDB.IO

The nightly dumps from EDDB.io are used to load all system and faction data into local maps.
See https://eddb.io/api for a list of available downloads and explanations.

These dump files are cached in data/ subdir with the timestamp of the file. Subsequent runs
will check the timestamp against the download file (using a HEAD request), and skip downloading
if the local cached file has the same timestamp, and then the cached versions are used to populate
the application state.

## EDSM

The EDSM API is used to fetch the historical influence values, and pending state information.
Once the systems/factions are known and loaded from EDDB downloads, the named faction's systems
are found and EDSM queried for the stats to be output.

# KNOWN ISSUES

- Historical output leaks into factions that are no longer in the system if they are still in EDSM's history

This only affects the -a output as it will show extra factions that should be ignored.

# TODO

- Implement a named system instead of faction.