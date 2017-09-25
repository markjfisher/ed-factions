package fish.ed

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

@Slf4j
class SystemsPopulated {
	static final String dataURLPath = "https://eddb.io/archive/v5/systems_populated.jsonl"

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
				population: j['population']
			]
		}
		log.info "Loaded ${systemNameToSystemData.size()} systems"
		this
	}

	def systemsWithFactionId(int factionId) {
		systemNameToSystemData?.findAll { (it.value['factionIds'] as List).contains(factionId)} ?: [:]
	}
}
