package fish

import groovy.json.JsonSlurper
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder

class Factions {

	static main(args) {
		def cli = new CliBuilder(usage: "Factions -p /path/to/factionToSystem.properties -f factionCode\nAdd an entry '<factionCode>.name=Name of Your Faction' to display summary of its lead/deficit to next position")
		cli.h(longOpt: 'help', required: false, 'show usage information')
		cli.p(longOpt: 'properties', required: true, args: 1, 'lists of faction codes against target systems to display')
		cli.f(longOpt: 'factionCode', required: true, args: 1, 'the faction code in the properties to display')
		cli.t(longOpt: 'today', required: false, args: 1, 'what date to use for today (format yyyyMMdd), assumes current date if unset')
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

		doStats(systems, today, factionName)
	}

	static doStats(List<String> systems, Date todayDate, String factionName) {
		String factionUrl = 'https://www.edsm.net/api-system-v1/factions'

		def slurper = new JsonSlurper()
		def client = HttpClientBuilder.create().build()

		String today = todayDate.format('yyyyMMdd')
		String d1 = (todayDate - 1).format('yyyyMMdd')
		String d3 = (todayDate - 3).format('yyyyMMdd')
		String d7 = (todayDate - 7).format('yyyyMMdd')

		println "Stats for $today\n"
		TreeMap<String, List> allData = [:]
		systems.each { String s ->
			def urlSystemName = URLEncoder.encode(s, 'UTF-8')
			def get = new HttpGet("${factionUrl}?systemName=${urlSystemName}")
			def response = client.execute(get)
			def reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))
			def jsonResponse = reader.getText()
			def todayFile = new File("data/${urlSystemName}.${today}.json")
			todayFile.delete()
			todayFile << jsonResponse

			def systemMap = slurper.parseText(jsonResponse)

			def d1DataFile = new File("data/${urlSystemName}.${d1}.json")
			def d1Data = d1DataFile.exists() ? d1DataFile.text : "{}"
			def d1Map = slurper.parseText(d1Data)

			def d3DataFile = new File("data/${urlSystemName}.${d3}.json")
			def d3Data = d3DataFile.exists() ? d3DataFile.text : "{}"
			def d3Map = slurper.parseText(d3Data)

			def d7DataFile = new File("data/${urlSystemName}.${d7}.json")
			def d7Data = d7DataFile.exists() ? d7DataFile.text : "{}"
			def d7Map = slurper.parseText(d7Data)

			List allSystemData = []
			systemMap['factions'].eachWithIndex { Map f, int i ->
				def tInf = (f.influence as float) * 100.0
				def d1Inf = ((d1Map?.get("factions")?.find{it.name == f.name}?.get('influence') ?: 0.0) as float) * 100.0
				def d3Inf = ((d3Map?.get("factions")?.find{it.name == f.name}?.get('influence') ?: 0.0) as float) * 100.0
				def d7Inf = ((d7Map?.get("factions")?.find{it.name == f.name}?.get('influence') ?: 0.0) as float) * 100.0

				String fName = shortString(f.name as String, 30)
				String factionState = shortString(f.state as String, 10)
				float t1Inf = tInf - d1Inf
				float t3Inf = tInf - d3Inf
				float t7Inf = tInf - d7Inf
				allSystemData += [position: i, system: s, name: fName, state: factionState, t0Inf: tInf, t1Inf: t1Inf, t3Inf: t3Inf, t7Inf: t7Inf]
			}
			allData[s] = allSystemData
		}

		allData.each { String systemName, List<Map> data ->
			println sprintf('%-30s State       Today   D1     D3     D7', systemName)
			data.each {Map d ->
				println sprintf('%-30s %-10s %6.2f %6.2f %6.2f %6.2f', d.name, d.state, d.t0Inf, d.t1Inf, d.t3Inf, d.t7Inf)
			}
			println ""
		}

		if (factionName) {
			println "Lead/Deficit relative to current          Today   D1     D3     D7"
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
					println sprintf("%-30s %s %6.2f %6.2f %6.2f %6.2f", systemName, leadTrailText, diffToNeighbour, diffToNeighbour - d1ToN, diffToNeighbour - d3ToN, diffToNeighbour - d7ToN)
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
