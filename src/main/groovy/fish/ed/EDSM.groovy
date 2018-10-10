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

	def slurper = new JsonSlurper()
	def client = HttpClientBuilder.create().build()

	List<Map> parseData(int edsmSystemId, int tickHour) {
		List<Map> factionData = []
		Map systemData = getSystemById(edsmSystemId)
		int storedPosition = 0
		systemData['factions'].each { Map f ->
			LocalDate latestDate = LocalDate.of(1900, 1, 1)
			Map<LocalDate, Object> influencesGroupedByDate = (f.influenceHistory ?: [:] as Map).inject([:]) { Map d, v ->
				double influence = v.value as double
				// any faction with near 0.0 value doesn't actually exist
				if (influence > 0.000001) {
					Instant influenceInstant = Instant.ofEpochSecond(v.key as int)
					ZonedDateTime zdt = ZonedDateTime.ofInstant(influenceInstant, ZoneOffset.UTC)
					// if the TS is between midnight and the tick time, the data is for the day before, o/w it's today.
					def subtractForTick
					if (zdt.getHour() >= tickHour && zdt.getHour() <= 23) {
						subtractForTick = 0
					} else {
						subtractForTick = 1
					}
					LocalDate ld = LocalDate.from(zdt).minusDays(subtractForTick)

					// store the influence against the date it applies
					def influencesForThisDate = d[ld] ?: []
					influencesForThisDate += influence
					d[ld] = influencesForThisDate
					if (ld > latestDate) latestDate = ld
				}
				d
			}
			if (!influencesGroupedByDate && f.influence > 0.000001) {
				// we have no historical data for influences but there is a current influence.
				// this happens when faction enters a new system. our best guess is yesterday's data
				latestDate = LocalDate.now().minusDays(1)
				influencesGroupedByDate.put(latestDate, [f.influence])
			}
			// this may store factions where they are no longer active, but the current dates would hold no values.
			if (f.influence < 0.000001) {
				f.state = "Gone"
			}
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
