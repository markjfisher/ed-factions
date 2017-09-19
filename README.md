A simple class to display stats for factions.

To run, use

    ./gradlew stats -q -Pargs="-f 'The Order of Mobius'"

The options are:

    -f <faction-name>  # Display all systems for named faction. Use quotes if the name has spaces in it
    -t YYYYMMDD        # override the date to display (default to latest in EDSM history)
    -a                 # show all factions in system
    -s <system-name>   # not yet implemented, show information only for one system


Example output

    Apathaam [3,506,390]                      SEP-18 SEP-17 SEP-15 SEP-11
    The Order of Mobius            None        57.84  -0.16  14.79  11.60 Boom
    Apathaam Brothers              Boom        14.98   0.88  -0.60   8.28
    Apathaam Patron's Principles   None        11.69  -1.31  -7.59  -3.73 Outbreak
    
    Leading/Trailing information for The Order of Mobius
    Apathaam              [SEP-18] leads by    42.86  -1.04  15.38   3.32

