package trik.testsys.webclient.service

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity

/**
 * @author Roman Shishkin
 * @since 1.1.0
 */
interface TrikHttpClient {

    fun <T> sendGetRequest(url: String, responseType: Class<T>, headers: HttpHeaders): ResponseEntity<T>

    fun <T> sendPostRequest(url: String, body: Any, responseType: Class<T>, headers: HttpHeaders): ResponseEntity<T>

    fun <T> sendPutRequest(url: String, body: Any, responseType: Class<T>, headers: HttpHeaders): ResponseEntity<T>
}