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
		println response
		headers.toArrayString()
	}
}
