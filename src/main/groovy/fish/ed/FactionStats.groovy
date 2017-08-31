package fish.ed

import groovy.json.JsonSlurper
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder

class FactionStats {
	static String factionUrl = 'https://www.edsm.net/api-system-v1/factions'
	static String dataPath = "data"
	static int showHistory = 0

	Factions factions
	SystemsPopulated systemsPopulated
	EDSM edsm
	def client = HttpClientBuilder.create().build()

	static main(args) {
		def cli = new CliBuilder(usage: "FactionStats -p /path/to/factionToSystem.properties -f factionCode\nAdd an entry '<factionCode>.name=Name of Your Faction' to display summary of its lead/deficit to next position")
		cli.h(longOpt: 'help', required: false, 'show usage information')
		cli.p(longOpt: 'properties', required: true, args: 1, 'lists of faction codes against target systems to display')
		cli.f(longOpt: 'factionCode', required: true, args: 1, 'the faction code in the properties to display')
		cli.t(longOpt: 'today', required: false, args: 1, 'what date to use for today (format yyyyMMdd), assumes current date if unset')
		cli.o(longOpt: 'overwrite', required: false, args: 0, 'overwrite data if already exists. defaults to false')
		cli.width = 132

		def options = cli.parse(args)
		if (!options || options.h || !options.p || !options.f) {
			if (options?.h) cli.usage()
			return
		}

		File props = new File(options.p)
		String factionCode = options.f

		Properties properties = new Properties()
		properties.load(props.newInputStream())
		String systemNames = properties[factionCode]
		if (!systemNames) {
			println "Could not find a faction in the properties file $props with name $factionCode"
			cli.usage()
			return
		}

		Date today = new Date()
		if (options.t) {
			today = Date.parse( 'yyyyMMdd', options.t as String)
		}

		List<String> systems = systemNames.split(',')
		String factionName = properties["${factionCode}.name" as String] ?: null

		boolean overwrite = options.o ?: false

		new FactionStats().doStats(systems, today, factionName, overwrite)
	}

	def doStats2(String factionName, Date todayDate, boolean overwrite) {
		println "Loading system and faction data..."
		factions = new Factions().load()
		systemsPopulated = new SystemsPopulated().load()
		edsm = new EDSM()

		def factionId = factions.findFaction(factionName)
		def systemsWithFaction = systemsPopulated.systemsWithFactionId(factionId)

		systemsWithFaction.each { systemName, systemData ->
			int systemId = systemData['edsm_id'] as int
			int systemPopulation = systemData['population'] as int
			def edsmDataForSystem = edsm.parseData(systemId)

			// now split the faction data into tick days, then average each day out.
		}
	}

	String getJsonData(String systemName, String forDate, boolean overwrite) {
		def jsonFile = new File("${dataPath}/${systemName}.${forDate}.json")
		if ((jsonFile.exists() && overwrite) || !jsonFile.exists()) {
			def urlSystemName = URLEncoder.encode(systemName, 'UTF-8')
			def get = new HttpGet("${factionUrl}?systemName=${urlSystemName}&showHistory=${showHistory}")
			def response = client.execute(get)
			def reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))
			def jsonResponse = reader.getText()
			jsonFile.delete()
			jsonFile << jsonResponse
		}
		jsonFile.text
	}

	def doStats(List<String> systems, Date todayDate, String factionName, boolean overwrite) {

		def slurper = new JsonSlurper()
		// def client = HttpClientBuilder.create().build()

		String today = todayDate.format('yyyyMMdd')
		String d1 = (todayDate - 1).format('yyyyMMdd')
		String d3 = (todayDate - 3).format('yyyyMMdd')
		String d7 = (todayDate - 7).format('yyyyMMdd')

		println "Stats for $today\n"
		TreeMap<String, List> allData = [:]
		systems.each { String s ->
			String jsonResponse = getJsonData(s, today, overwrite)

			def systemMap = slurper.parseText(jsonResponse)

			def d1DataFile = new File("${dataPath}/${urlSystemName}.${d1}.json")
			def d1Data = d1DataFile.exists() ? d1DataFile.text : "{}"
			def d1Map = slurper.parseText(d1Data)

			def d3DataFile = new File("${dataPath}/${urlSystemName}.${d3}.json")
			def d3Data = d3DataFile.exists() ? d3DataFile.text : "{}"
			def d3Map = slurper.parseText(d3Data)

			def d7DataFile = new File("${dataPath}/${urlSystemName}.${d7}.json")
			def d7Data = d7DataFile.exists() ? d7DataFile.text : "{}"
			def d7Map = slurper.parseText(d7Data)

			List allSystemData = []
			systemMap['factions'].eachWithIndex { Map f, int i ->
				// println f.pendingStates
				def pendingStates = f.pendingStates*.state.findAll().join(', ')
				def tInf = (f.influence as float) * 100.0
				def d1Inf = ((d1Map?.get("factions")?.find{it.name == f.name}?.get('influence') ?: 0.0) as float) * 100.0
				def d3Inf = ((d3Map?.get("factions")?.find{it.name == f.name}?.get('influence') ?: 0.0) as float) * 100.0
				def d7Inf = ((d7Map?.get("factions")?.find{it.name == f.name}?.get('influence') ?: 0.0) as float) * 100.0

				String fName = shortString(f.name as String, 30)
				String factionState = shortString(f.state as String, 10)
				float t1Inf = tInf - d1Inf
				float t3Inf = tInf - d3Inf
				float t7Inf = tInf - d7Inf
				allSystemData += [position: i, system: s, name: fName, state: factionState, t0Inf: tInf, t1Inf: t1Inf, t3Inf: t3Inf, t7Inf: t7Inf, pendingStates: pendingStates]
			}
			allData[s] = allSystemData
		}

		println "                               State       Today   D1     D3     D7   Pending"
		allData.each { String systemName, List<Map> data ->
			println sprintf('%-30s', systemName)
			boolean hasShownMainFaction = false
			data.eachWithIndex { Map d, int i ->
				if (i < 3 || !hasShownMainFaction) {
					println sprintf('%-30s %-10s %6.2f %6.2f %6.2f %6.2f %s', d.name, d.state, d.t0Inf, d.t1Inf, d.t3Inf, d.t7Inf, d.pendingStates)
				}
				if (d.name == factionName) hasShownMainFaction = true
			}
			println ""
		}

		if (factionName) {
			println "Lead/Deficit relative to current"
			allData.each { String systemName, List<Map> data ->
				Map namedFaction = data.find { it.name == factionName }
				if (namedFaction) {
					int position = namedFaction['position'] as int
					def neighbour = position == 0 ? 1 : position - 1
					def neighbourFaction = data.find {it.position == neighbour}
					float diffToNeighbour = namedFaction['t0Inf'] - neighbourFaction['t0Inf']
					float d1ToN = namedFaction['t0Inf'] - namedFaction['t1Inf'] - (neighbourFaction['t0Inf'] - neighbourFaction['t1Inf'])
					float d3ToN = namedFaction['t0Inf'] - namedFaction['t3Inf'] - (neighbourFaction['t0Inf'] - neighbourFaction['t3Inf'])
					float d7ToN = namedFaction['t0Inf'] - namedFaction['t7Inf'] - (neighbourFaction['t0Inf'] - neighbourFaction['t7Inf'])
					String leadTrailText = diffToNeighbour > 0 ? "leads by " : "trails by"
					println sprintf("%-30s %s  %6.2f %6.2f %6.2f %6.2f", systemName, leadTrailText, diffToNeighbour, diffToNeighbour - d1ToN, diffToNeighbour - d3ToN, diffToNeighbour - d7ToN)
				}
			}
		}
	}

	static String shortString(String s, int n) {
		String newS
		if (s.length() <= n) {
			newS = s
		} else {
			newS = s.substring(0, n-3) + "..."
		}
		return newS
	}
}
