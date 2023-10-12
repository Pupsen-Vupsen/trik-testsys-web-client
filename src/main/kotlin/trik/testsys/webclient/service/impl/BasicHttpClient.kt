package trik.testsys.webclient.service.impl

import org.springframework.http.*
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import trik.testsys.webclient.service.TrikHttpClient

/**
 * @author Roman Shishkin
 * @since 1.1.0
 */
@Component
class BasicHttpClient : TrikHttpClient {

    private val restTemplate = RestTemplate()

    init {
        val converter = MappingJackson2HttpMessageConverter()
        converter.supportedMediaTypes = listOf(MediaType.APPLICATION_OCTET_STREAM)
        restTemplate.messageConverters.add(converter)
    }

    override fun <T> sendGetRequest(url: String, responseType: Class<T>, headers: HttpHeaders): ResponseEntity<T> {
        val entity = HttpEntity<String>(headers)
        return restTemplate.exchange(url, HttpMethod.GET, entity, responseType)
    }

    override fun <T> sendPostRequest(url: String, body: Any, responseType: Class<T>, headers: HttpHeaders): ResponseEntity<T> {
        val entity = HttpEntity(body, headers)
        return restTemplate.exchange(url, HttpMethod.POST, entity, responseType)
    }

    override fun <T> sendPutRequest(url: String, body: Any, responseType: Class<T>, headers: HttpHeaders): ResponseEntity<T> {
        val entity = HttpEntity(body, headers)
        return restTemplate.exchange(url, HttpMethod.PUT, entity, responseType)
    }
}