A simple class to display stats for factions.

To run, use

    gradlew -q stats -Pargs="-p factions.properties -f oom -t 20170801"

The options are:

    -p <faction-code-to-system-list-properties-file>
    -f <faction-code-from-properties-file>
    -t <date-for-stats> [default is today's date, format YYYYMMDD]
    
factions.properties contains lists of systems against codes, of the format:

    oom=Apathaam,Azrael,Cardea,Exioce,Njiri,NLTT 10055
    oom.name=The Order of Mobius

Example output

    Stats for 20170801
    
    Apathaam                       State       Today   D1     D3     D7
    The Order of Mobius            Boom        76.32   2.54   7.69  14.49
    Apathaam Patron's Principles   Civil war    8.59  -1.68  -3.20  -2.50
    ....
    
    Lead/Deficit relative to current          Today   D1     D3     D7
    Apathaam                       leads by   67.73   4.22  10.89  16.98
