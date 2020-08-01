package fish.ed

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

@Slf4j
class Factions {
	static final String dataURLPath = "https://eddb.io/archive/v6/factions.jsonl"

	Map factionIdToFactionData = [:]
	JsonSlurper slurper = new JsonSlurper()

	def load() {
		File jsonFile = EDDB.getJsonFile(dataURLPath, 'factions')
		if (!jsonFile.exists()) {
			log.error("Unable to find jsonl file ${jsonFile}")
			return this
		}

		jsonFile.eachLine { String jline ->
			def j = slurper.parseText(jline)
			factionIdToFactionData[j['id']] = [
				name: j['name'],
				isPlayerFaction: j['is_player_faction']
			]
		}
		log.info "Loaded ${factionIdToFactionData.size()} factions"
		this
	}

	int findFaction(String factionName) {
		factionIdToFactionData?.find { it.value?.name == factionName }?.key ?: -1
	}
}
