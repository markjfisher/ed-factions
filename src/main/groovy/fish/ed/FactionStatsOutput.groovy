package fish.ed

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FactionStatsOutput {

	Factions factions
	SystemsPopulated systemsPopulated
	EDSM edsm

	static final DateTimeFormatter shortFormat = DateTimeFormatter.ofPattern('MMdd')

	static main(args) {
		// how do we do just a system?
		def cli = new CliBuilder(usage: "FactionStats -f factionName")
		cli.h(longOpt: 'help', required: false, 'show usage information')
		cli.f(longOpt: 'factionName', required: false, args: 1, 'the faction name in the properties to display')
		cli.t(longOpt: 'today', required: false, args: 1, 'what date to use for today (format yyyyMMdd), assumes current date if unset')
		cli.a(longOpt: 'allFactions', required: false, args: 0, 'When set, show all factions, not just up to main named faction')
		cli.s(longOpt: 'systemName', required: false, args: 1, 'the system name to display TBD')
		cli.width = 132

		def options = cli.parse(args)
		if (!options || options.h || (!options.f && !options.a)) {
			if (options?.h) cli.usage()
			return
		}

		LocalDate today = null
		if (options.t) {
			today = LocalDate.parse(options.t as String, DateTimeFormatter.BASIC_ISO_DATE)
		}
		boolean showAllFactions = options.a

		if (options.f) {
			new FactionStatsOutput().showFactionStats(options.f as String, today, showAllFactions)
		}
		if (options.a) {
			new FactionStatsOutput().showSystemStats(options.a as String, today)
		}
	}

	def showSystemStats(String systemName, LocalDate todayDate) {}

	def showFactionStats(String factionName, LocalDate todayDate, boolean showAllFactions) {
		println "Loading system and faction data..."
		factions = new Factions().load()
		systemsPopulated = new SystemsPopulated().load()
		edsm = new EDSM()

		def factionId = factions.findFaction(factionName)
		def systemsWithFaction = systemsPopulated.systemsWithFactionId(factionId) as Map<String, Map>

		Map<String, List<Map>> cachedEDSMDataForSystem = [:]
		systemsWithFaction.each { String systemName, Map systemData ->
			int systemId = systemData['edsm_id'] as int
			def edsmDataForSystem = edsm.parseData(systemId) as List<Map>
			cachedEDSMDataForSystem[systemName] = edsmDataForSystem
		}

		println ""
		cachedEDSMDataForSystem.each { String systemName, List<Map> edsmDataForSystem ->
			if (edsmDataForSystem.size() > 0) {
				displayFactionsInSystem(factionName, todayDate, showAllFactions, systemName, systemsWithFaction[systemName].population as int, edsmDataForSystem)
			}
		}

		println "Leading/Trailing information for $factionName"
		cachedEDSMDataForSystem.each { String systemName, List<Map> edsmDataForSystem ->
			if (edsmDataForSystem.size() > 0) {
				displayFactionLeads(factionName, todayDate, systemName, edsmDataForSystem)
			}
		}

	}

	def displayFactionsInSystem(String factionName, LocalDate todayDate, boolean showAllFactions, String systemName, int population, List<Map> edsmDataForSystem) {
		boolean hasShownMainFaction = false
		edsmDataForSystem.eachWithIndex { Map factionData, int i ->

			LocalDate fromDate = calculateFromDate(todayDate, factionData)
			LocalDate d1Date = calculateFromDate(fromDate.minusDays(1), factionData)
			LocalDate d3Date = calculateFromDate(fromDate.minusDays(3), factionData)
			LocalDate d7Date = calculateFromDate(fromDate.minusDays(7), factionData)

			if (i == 0) {
				def headingString = sprintf('%-41s %6s %6s %6s %6s',
					"${systemName} [${NumberFormat.getNumberInstance(Locale.UK).format(population)}]",
					fromDate.format(shortFormat),
					d1Date.format(shortFormat),
					d3Date.format(shortFormat),
					d7Date.format(shortFormat),
				)
				println headingString
			}

			Map<LocalDate, List> influenceHistory = factionData.influenceHistory
			float i0 = last(influenceHistory[fromDate] as List) * 100.0
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
		}
		println ""
	}

	def displayFactionLeads(String factionName, LocalDate todayDate, String systemName, List<Map> edsmDataForSystem) {
		Map namedFaction = edsmDataForSystem.find { it.name == factionName }
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

		float i0 = (last(aH[fromDateA]) - last(bH[fromDateB])) * 100.0
		float i1 = (last(aH[fromDateA]) - avg(aH[d1DateA]) - (last(bH[fromDateB]) - avg(bH[d1DateB]))) * 100.0
		float i2 = (last(aH[fromDateA]) - avg(aH[d3DateA]) - (last(bH[fromDateB]) - avg(bH[d3DateB]))) * 100.0
		float i3 = (last(aH[fromDateA]) - avg(aH[d7DateA]) - (last(bH[fromDateB]) - avg(bH[d7DateB]))) * 100.0

		String leadTrailText = i0 > 0 ? "leads by" : "trails by"
		println sprintf("%-30s %-10s %6.2f %6.2f %6.2f %6.2f", systemName, leadTrailText, i0, i1, i2, i3)
	}

	static float avg(List fs) {
		return fs.sum() / fs.size()
	}

	static float last(List fs) {
		return fs[-1]
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


	LocalDate calculateFromDate(LocalDate todayDate, Map factionData) {
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
