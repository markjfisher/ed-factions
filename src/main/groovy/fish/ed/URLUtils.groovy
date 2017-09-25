package fish.ed

import org.apache.http.Header
import org.apache.http.client.methods.HttpHead
import org.apache.http.impl.client.HttpClientBuilder

class URLUtils {

	static String getLastModified(String uri) {
		def client = HttpClientBuilder.create().build()
		def head = new HttpHead(uri)
		def response = client.execute(head)
		// def reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))
		Header[] headers = response.getHeaders("Last-Modified")
		if (headers.size() > 0) {
			def lastModified = headers[0].value
		}
		println response
		headers.toArrayString()
	}

	static main(args) {
		getLastModified('https://eddb.io/archive/v5/factions.jsonl')
	}
}
