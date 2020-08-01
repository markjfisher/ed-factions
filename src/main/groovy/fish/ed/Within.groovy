package fish.ed

import groovy.cli.commons.CliBuilder
import groovy.util.logging.Slf4j

@Slf4j
class Within {
	String name
	double range
	int maxFactions

	Factions factions
	SystemsPopulated systemsPopulated
	EDSM edsm

	static main(args) {
		def cli = new CliBuilder(usage: "FactionStats -f factionName")
		cli.h(longOpt: 'help', required: false, 'show usage information')
		cli.s(longOpt: 'systemName', required: true, args: 1, 'the system name to display TBD')
		cli.r(longOpt: 'range', required: true, args: 1, 'the distance within which to show of the target system')
		cli.m(longOpt: 'maxFactions', required: false, args: 1, 'max number of factions in system to filter on')
		cli.d(longOpt: 'debug', required: false, args: 0, 'enable debug output')
		cli.width = 132

		def options = cli.parse(args)
		if (!options || options.h || !options.s || !options.r) {
			if (options?.h) cli.usage()
			return
		}

		int max = (options.m ?: "10").toInteger()
		new Within(options.s as String, options.r as double, max).findNearest()
	}

	Within(String name, double range, int maxFactions) {
		this.name = name
		this.range = range
		this.maxFactions = maxFactions
	}

	def loadData() {
		log.info "Loading system and faction data..."
		factions = new Factions().load()
		systemsPopulated = new SystemsPopulated().load()
		edsm = new EDSM()
	}

	def findNearest() {
		loadData()
		Map.Entry sd = systemsPopulated.systemInfoFromName(name)
		if (!sd) {
			println "\nNo data for $name"
			return
		}

		println ""
		List<Map> close = systemsPopulated.systemsWithin(range, sd.value.x as double, sd.value.y as double, sd.value.z as double, factions)
		boolean headerPrinted = false
		close.each { Map systemData ->
			String name = systemData.name
			double range = systemData.range
			int fC = systemData.fCount
			int pfC = systemData.pfCount

			String heading = sprintf('%-20s %-8s %2s %2s', "system", "range", "fc", "pf")
			if (!headerPrinted) {
				println heading
				println "-" * (heading.size())
				headerPrinted = true
			}

			if (fC <= maxFactions) {
				println sprintf('%-20s %-8.3f %2d %2d', name, range, fC, pfC)
			}
		}
	}
}
