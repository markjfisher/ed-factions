package fish.ed

import groovy.util.logging.Slf4j

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Slf4j
class FactionStatsOutput {

	Factions factions
	SystemsPopulated systemsPopulated
	EDSM edsm

	static boolean debugOutput = false
	static final DateTimeFormatter shortFormat = DateTimeFormatter.ofPattern('MMM-dd')

	static main(args) {
		def cli = new CliBuilder(usage: "FactionStats -f factionName")
		cli.h(longOpt: 'help', required: false, 'show usage information')
		cli.f(longOpt: 'factionName', required: false, args: 1, 'the faction name in the properties to display')
		cli.t(longOpt: 'today', required: false, args: 1, 'what date to use for today (format yyyyMMdd), assumes current date if unset')
		cli.a(longOpt: 'allFactions', required: false, args: 0, 'When set, show all factions, not just up to main named faction')
		cli.s(longOpt: 'systemName', required: false, args: 1, 'the system name to display TBD')
		cli.d(longOpt: 'debug', required: false, args: 0, 'enable debug output')
		cli.i(required: false, args: 1, 'read faction names from input file')
		cli.j(required: false, args: 1, 'read system names from input file')
		cli.g(longOpt: 'showGone', required: false, args: 0, 'Show "Gone" systems (retreated factions that are still appearing in history, default: false)')
		cli.k(longOpt: 'tickHour', required: false, args: 1, 'Set the system tick hour (UTC) as it keeps drifting. Default: 17 (i.e. 5pm UTC)')
		cli.width = 132

		def options = cli.parse(args)
		if (!options || options.h || (!options.j && !options.i && !options.f && !options.s)) {
			if (options?.h) cli.usage()
			return
		}

		if (options.d) {
			debugOutput = true
		}

		LocalDate today = null
		if (options.t) {
			today = LocalDate.parse(options.t as String, DateTimeFormatter.BASIC_ISO_DATE)
		}
		boolean showAllFactions = options.a
		boolean showGoneFactions = options.g

		int tickHour = 17
		if (options.k) {
			tickHour = Integer.parseInt(options.k as String)
		}

		if (options.f) {
			new FactionStatsOutput().showFactionStats(options.f as String, today, tickHour, showAllFactions, showGoneFactions)
		}
		if (options.s) {
			new FactionStatsOutput().showSystemStats(options.s as String, today, tickHour, showGoneFactions)
		}
		if (options.i) {
			File inFile = new File(options.i as String)
			if (!inFile.exists()) {
				println "Could not find input file $inFile"
				return
			}
			FactionStatsOutput outputter = new FactionStatsOutput()
			outputter.loadData()
			println "Tick Hour: $tickHour\n"
			inFile.eachLine { String faction ->
				println "-" * 3
				outputter.showFactionStats(faction, today, tickHour, showAllFactions, showGoneFactions)
			}
		}
		if (options.j) {
			File inFile = new File(options.j as String)
			if (!inFile.exists()) {
				println "Could not find input file $inFile"
				return
			}
			FactionStatsOutput outputter = new FactionStatsOutput()
			outputter.loadData()
			println "\nTick Hour: $tickHour"
			inFile.eachLine { String system ->
				println "-" * 3
				outputter.showSystemStats(system, today, tickHour, showGoneFactions)
			}
		}
	}

	def loadData() {
		if (!factions) {
			log.info "Loading system and faction data..."
			factions = new Factions().load()
			systemsPopulated = new SystemsPopulated().load()
			edsm = new EDSM()
		}
	}

	def showSystemStats(String name, LocalDate todayDate, int tickHour, boolean showGoneFactions) {
		loadData()

		Map.Entry sd = systemsPopulated.systemInfoFromName(name)
		if (!sd) {
			println "\nNo data for $name"
			return
		}

		Map systemData = sd.value as Map
		int systemId = systemData['edsm_id'] as int
		def edsmDataForSystem = edsm.parseData(systemId, tickHour) as List<Map>
		if (!showGoneFactions) {
			edsmDataForSystem = removeGoneFactions(edsmDataForSystem)
		}

		println ""
		if (edsmDataForSystem.size() > 0) {
			displayFactionsInSystem("", todayDate, true, name, systemData.population as long, edsmDataForSystem)
		}

	}

	List<Map> removeGoneFactions(List<Map> data) {
		// filter out any where currentState == "Gone"
		return data.inject([]) { List results, Map factionData ->
			if (factionData["currentState"] != "Gone") {
				results += factionData
			}
			results
		}

	}

	def showFactionStats(String factionName, LocalDate todayDate, int tickHour, boolean showAllFactions, boolean showGoneFactions) {
		loadData()

		def factionId = factions.findFaction(factionName)
		if (factionId == -1) {
			log.info "No factions found for '$factionName'"
			return
		}

		def systemsWithFaction = systemsPopulated.systemsWithFactionId(factionId) as Map<String, Map>
		Map<String, List<Map>> cachedEDSMDataForSystem = [:]
		systemsWithFaction.each { String systemName, Map systemData ->
			int systemId = systemData['edsm_id'] as int
			def edsmDataForSystem = edsm.parseData(systemId, tickHour) as List<Map>
			if (!showGoneFactions) {
				edsmDataForSystem = removeGoneFactions(edsmDataForSystem)
			}
			cachedEDSMDataForSystem[systemName] = edsmDataForSystem
		}

		println ""
		cachedEDSMDataForSystem.each { String systemName, List<Map> edsmDataForSystem ->
			if (edsmDataForSystem.size() > 0) {
				displayFactionsInSystem(factionName, todayDate, showAllFactions, systemName, systemsWithFaction[systemName].population as long, edsmDataForSystem)
			}
		}

		println "Leading/Trailing information for $factionName"
		cachedEDSMDataForSystem.each { String systemName, List<Map> edsmDataForSystem ->
			if (edsmDataForSystem.size() > 0) {
				displayFactionLeads(factionName, todayDate, systemName, edsmDataForSystem)
			}
		}
	}

	def displayFactionsInSystem(String factionName, LocalDate todayDate, boolean showAllFactions, String systemName, long population, List<Map> edsmDataForSystem) {
		boolean hasShownMainFaction = false
		LocalDate fromDate = LocalDate.of(1900, 1, 1)
		edsmDataForSystem.each { Map factionData ->
            if (factionData.currentDate.isAfter(fromDate)) {
				fromDate = factionData.currentDate
			}
		}
		LocalDate d1Date = fromDate.minusDays(1)
		LocalDate d3Date = fromDate.minusDays(3)
		LocalDate d7Date = fromDate.minusDays(7)

		edsmDataForSystem.eachWithIndex { Map factionData, int i ->
			if (i == 0) {
				def headingString = sprintf('%-41s %6s %6s %6s %6s',
					"${systemName} [${NumberFormat.getNumberInstance(Locale.UK).format(population)}]",
					fromDate.format(shortFormat).toUpperCase(),
					d1Date.format(shortFormat).toUpperCase(),
					d3Date.format(shortFormat).toUpperCase(),
					d7Date.format(shortFormat).toUpperCase(),
				)
				println headingString
			}

			Map<LocalDate, List> influenceHistory = factionData.influenceHistory
			float i0 = avg(influenceHistory[fromDate] as List) * 100.0
			float i1 = avg(influenceHistory[d1Date] as List) * 100.0
			float i2 = avg(influenceHistory[d3Date] as List) * 100.0
			float i3 = avg(influenceHistory[d7Date] as List) * 100.0
			def outStr = sprintf('%-30s %-10s %6.2f %6.2f %6.2f %6.2f %s',
				ss(factionData.name as String, 30),
				ss(factionData.currentState as String, 10),
				i0,
				i0 - i1,
				i0 - i2,
				i0 - i3,
				factionData.pendingStates.join(', '))
			if (showAllFactions || i < 3 || !hasShownMainFaction) {
				println outStr
			}
			if (factionData.name == factionName) {
				hasShownMainFaction = true
			}

			if (debugOutput) {
				println "todayDate: ${todayDate}"
				println "fromDate: ${fromDate}"
				println "system : ${systemName}"
				println "faction: ${factionData.name}"
				println "ih0    : ${influenceHistory[fromDate]}"
				println "ih1    : ${influenceHistory[d1Date]}"
				println "ih2    : ${influenceHistory[d3Date]}"
				println "ih3    : ${influenceHistory[d7Date]}"
			}
		}
		println ""
	}

	def displayFactionLeads(String factionName, LocalDate todayDate, String systemName, List<Map> edsmDataForSystem) {
		Map namedFaction = edsmDataForSystem.find { it.name == factionName }

		if (!namedFaction) {
			println "${systemName} - no data for ${factionName}"
			return
		}
		int position = namedFaction.position
		def neighbour = position == 0 ? 1 : position - 1
		Map neighbourFaction = edsmDataForSystem.find {it.position == neighbour}

		LocalDate fromDateA = calculateFromDate(todayDate, namedFaction)
		LocalDate d1DateA = calculateFromDate(fromDateA.minusDays(1), namedFaction)
		LocalDate d3DateA = calculateFromDate(fromDateA.minusDays(3), namedFaction)
		LocalDate d7DateA = calculateFromDate(fromDateA.minusDays(7), namedFaction)

		LocalDate fromDateB = calculateFromDate(todayDate, neighbourFaction)
		LocalDate d1DateB = calculateFromDate(fromDateB.minusDays(1), neighbourFaction)
		LocalDate d3DateB = calculateFromDate(fromDateB.minusDays(3), neighbourFaction)
		LocalDate d7DateB = calculateFromDate(fromDateB.minusDays(7), neighbourFaction)

		Map<LocalDate, List> aH = namedFaction.influenceHistory
		Map<LocalDate, List> bH = neighbourFaction.influenceHistory

		float i0 = (avg(aH[fromDateA]) - avg(bH[fromDateB])) * 100.0
		float i1 = (avg(aH[fromDateA]) - avg(aH[d1DateA]) - (avg(bH[fromDateB]) - avg(bH[d1DateB]))) * 100.0
		float i2 = (avg(aH[fromDateA]) - avg(aH[d3DateA]) - (avg(bH[fromDateB]) - avg(bH[d3DateB]))) * 100.0
		float i3 = (avg(aH[fromDateA]) - avg(aH[d7DateA]) - (avg(bH[fromDateB]) - avg(bH[d7DateB]))) * 100.0

		String leadTrailText = i0 > 0 ? "leads by" : "trails by"
		println sprintf("%-21s %8s %-10s %6.2f %6.2f %6.2f %6.2f", ss(systemName, 21), "[${fromDateA.format(shortFormat).toUpperCase()}]", leadTrailText, i0, i1, i2, i3)
	}

	static float avg(List fs) {
		if (!fs) return 0.0
		if (fs.size() == 1) return fs[0] as float

		float median = calculateMedian(fs)
		float mode = calculateAvgMode(fs)
		if ((median - mode) > 0.000001) {
			println "Warning, median and mode differ for $fs [median: $median, mode: $mode] - using median"
		}

		return median
	}

	static float calculateMedian(List ls) {
		if (!ls) return 0.0

		List sorted = ls.sort()
		int lsize = sorted.size()
		if (lsize % 2 == 1) {
			int element = ((lsize - 1) / 2) as int
			return sorted[element] as float
		} else {
			int element = (lsize/2) as int
			return ((sorted[element - 1] + sorted[element]) / 2.0) as float
		}
	}

	static float calculateAvgMode(List ls) {
		if (!ls) return 0.0

		def m = [:]
		ls.each {
			m[it] = m[it] == null ? 1 : m[it] + 1
		}
		def keys = m.keySet().sort { -m[it] }
		List allModes = keys.findAll { m[it] == m[keys[0]] }
		return allModes.sum() / allModes.size()
	}

	static String ss(String s, int n) {
		String newS
		if (s.length() <= n) {
			newS = s
		} else {
			newS = s.substring(0, n-3) + "..."
		}
		return newS
	}


	static LocalDate calculateFromDate(LocalDate todayDate, Map factionData) {
		// returns best closest date from faction data

		LocalDate fromDate
		// if todayDate is null, we use the latest for the faction
		if (!todayDate) {
			fromDate = factionData.currentDate
		} else {
			// if faction has this date, use it, else find previous date to it
			if (factionData.influenceHistory[todayDate]) {
				fromDate = todayDate
			} else {
				def knownDates = (factionData.influenceHistory as Map).keySet()?.sort()?.reverse()
				fromDate = knownDates.find { it < todayDate } as LocalDate
				if (!fromDate && knownDates) {
					fromDate = knownDates[-1] as LocalDate
				}
			}
		}
		fromDate
	}
}
