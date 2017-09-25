package fish.ed

import groovy.util.logging.Slf4j

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Slf4j
class EDDB {

	static File getJsonFile(String path, String name) {
		File jsonFile = new File("./data/${name}.jsonl")
		File lastModifiedFile = new File("./data/${name}.lastModified")

		// get the last modified time for data
		LocalDateTime lastModified = URLUtils.getLastModified(path)
		LocalDateTime cachedLastModified = URLUtils.dateFromFileContent(name)

		// if different, download the new version, and store the latest date
		if (lastModified && (!cachedLastModified || (lastModified > cachedLastModified))) {
			String lastModifiedAsString = lastModified.atZone(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)
			log.info("Saving ${name} data with timestamp ${lastModifiedAsString}")

			lastModifiedFile.delete()
			jsonFile.delete()

			lastModifiedFile << lastModifiedAsString
			jsonFile << path.toURL().openStream()
		}

		if (!jsonFile.exists()) {
			log.error("Unable to find jsonl file ${jsonFile}")
			return null
		}
		return jsonFile
	}
}
