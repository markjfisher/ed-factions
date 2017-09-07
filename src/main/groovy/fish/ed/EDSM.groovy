package fish.ed

import groovy.json.JsonSlurper
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class EDSM {
	public static String factionUrl = 'https://www.edsm.net/api-system-v1/factions'
	public static final int showHistory = 1
	public static final int TICK_HOUR = 23


	def slurper = new JsonSlurper()
	def client = HttpClientBuilder.create().build()

	List<Map> parseData(int edsmSystemId) {
		List<Map> factionData = []
		Map systemData = getSystemById(edsmSystemId)
		int storedPosition = 0
		systemData['factions'].each { Map f ->
			LocalDate latestDate = LocalDate.of(1900, 1, 1)
			def influencesGroupedByDate = (f.influenceHistory ?: [:] as Map).inject([:]) { Map d, v ->
				double influence = v.value as double
				// any faction with near 0.0 value doesn't actually exist
				if (influence > 0.000001) {
					Instant influenceInstant = Instant.ofEpochSecond(v.key as int)
					ZonedDateTime zdt = ZonedDateTime.ofInstant(influenceInstant, ZoneOffset.UTC)
					// if the TS is between midnight and the tick time, the data is for the day before, o/w it's today.
					LocalDate ld = LocalDate.from(zdt).minusDays(zdt.getHour() < TICK_HOUR ? 1 : 0)

					// store the influence against the date it applies
					def influencesForThisDate = d[ld] ?: []
					influencesForThisDate += influence
					d[ld] = influencesForThisDate
					if (ld > latestDate) latestDate = ld
				}
				d
			}
			// this may store factions where they are no longer active, but the current dates would hold no values.
			if (influencesGroupedByDate) {
				factionData += [
					name            : f.name,
					position        : storedPosition++,
					currentInfluence: f.influence as double,
					currentDate     : latestDate,
					currentState    : f.state,
					pendingStates   : f.pendingStates*.state.findAll(),
					influenceHistory: influencesGroupedByDate
				]
			}
		}
		factionData
	}

	Map getSystemById(int edsmSystemId) {
		def get = new HttpGet("${factionUrl}?systemId=${edsmSystemId}&showHistory=${showHistory}")
		def response = client.execute(get)
		def reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))
		slurper.parseText(reader.getText()) as Map
	}

}
