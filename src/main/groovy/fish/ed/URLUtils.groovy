package fish.ed

import groovy.util.logging.Slf4j
import org.apache.http.Header
import org.apache.http.client.methods.HttpHead
import org.apache.http.impl.client.HttpClientBuilder

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Slf4j
class URLUtils {

	static LocalDateTime getLastModified(String uri) {
		LocalDateTime modifiedDate = null
		def client = HttpClientBuilder.create().build()
		def head = new HttpHead(uri)
		def response = client.execute(head)
		Header[] headers = response.getHeaders("Last-Modified")
		if (headers?.size() > 0) {
			modifiedDate = LocalDateTime.parse(headers[0].value, DateTimeFormatter.RFC_1123_DATE_TIME)
		}
		modifiedDate
	}

	static LocalDateTime dateFromFileContent(String name) {
		LocalDateTime theDate = null
		File inFile = new File("./data/${name}.lastModified")
		if (inFile.exists()) {
			String dateString = inFile.text
			try {
				theDate = LocalDateTime.parse(dateString, DateTimeFormatter.RFC_1123_DATE_TIME)
			} catch(Exception e) {
				log.error("Could not parse date from: ${dateString} in file ${inFile}")
			}
		}
		theDate
	}

	static main(args) {
		// log.info("last modified date: " + getLastModified('https://eddb.io/archive/v5/factions.jsonl'))
		// log.info("parsed date: " + dateFromFileContent('factions'))
	}
}
