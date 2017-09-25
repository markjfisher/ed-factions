package fish.ed

import groovy.json.JsonSlurper

class Factions {
	static final String dataURLPath = "https://eddb.io/archive/v5/factions.jsonl"

	Map factionIdToFactionData = [:]
	JsonSlurper slurper = new JsonSlurper()

	def load() {
		def url = dataURLPath.toURL()
		url.eachLine { String jline ->
			def j = slurper.parseText(jline)
			factionIdToFactionData[j['id']] = j['name']
		}
		println "Loaded ${factionIdToFactionData.size()} factions"
		this
	}

	int findFaction(String factionName) {
		factionIdToFactionData?.find { it.value == factionName }?.key ?: -1
	}
}
