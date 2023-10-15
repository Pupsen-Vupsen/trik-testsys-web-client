package trik.testsys.webclient.service

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap

typealias HttpBody = MultiValueMap<String, Any>

/**
 * @author Roman Shishkin
 * @since 1.1.0
 */
interface TrikHttpClient {

    fun <T> sendGetRequest(url: String, responseType: Class<T>, headers: HttpHeaders): ResponseEntity<T>

    fun <T> sendPostRequest(url: String, body: HttpBody, responseType: Class<T>, headers: HttpHeaders): ResponseEntity<T>

    fun <T> sendPutRequest(url: String, body: HttpBody, responseType: Class<T>, headers: HttpHeaders): ResponseEntity<T>
}