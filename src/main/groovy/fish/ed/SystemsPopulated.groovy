package fish.ed

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

import java.util.Map.Entry

@Slf4j
class SystemsPopulated {
	static final String dataURLPath = "https://eddb.io/archive/v6/systems_populated.jsonl"

	Map systemNameToSystemData = new TreeMap()
	JsonSlurper slurper = new JsonSlurper()

	def load() {
		File jsonFile = EDDB.getJsonFile(dataURLPath, 'systems_populated')
		if (!jsonFile.exists()) {
			log.error("Unable to find jsonl file ${jsonFile}")
			return this
		}

		jsonFile.eachLine { String jline ->
			def j = slurper.parseText(jline)
			List factionIds = j['minor_faction_presences']*.minor_faction_id
			systemNameToSystemData[j['name']] = [
				id: j['id'],
				edsm_id: j['edsm_id'],
				factionIds: factionIds,
				population: j['population'],
				x: j['x'] as double,
				y: j['y'] as double,
				z: j['z'] as double,
			]
		}
		log.info "Loaded ${systemNameToSystemData.size()} systems"
		this
	}

	Map systemsWithFactionId(int factionId) {
		systemNameToSystemData?.findAll { (it.value['factionIds'] as List).contains(factionId)} ?: [:]
	}

	Map.Entry systemInfoFromName(String name) {
		systemNameToSystemData?.find { it.key == name }
	}

	List systemsWithin(double range, double x, double y, double z, Factions factions) {
		List closeSystems = systemNameToSystemData?.inject([]) { List all, Entry v ->
			double sx = v.value['x'] as double
			double sy = v.value['y'] as double
			double sz = v.value['z'] as double
			double distFrom = Math.sqrt(Math.pow(sx - x, 2) + Math.pow(sy - y, 2) + Math.pow(sz - z, 2))
			if (distFrom <= range) {
				List factionIds = v.value['factionIds'] as List
				int pfCount = factionIds.inject(0) { int pfc, int id ->
					pfc += factions.factionIdToFactionData[id].isPlayerFaction ? 1 : 0
					pfc
				}
				all += [name: v.key, range: distFrom, fCount: factionIds.size(), pfCount: pfCount]
			}
			all
		} as List

		closeSystems.sort { a, b ->
			a.range <=> b.range
		}
	}
}
