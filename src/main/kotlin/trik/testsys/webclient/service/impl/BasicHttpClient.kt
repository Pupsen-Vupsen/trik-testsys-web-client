package trik.testsys.webclient.service.impl

import org.springframework.http.*
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import trik.testsys.webclient.service.HttpBody
import trik.testsys.webclient.service.TrikHttpClient

import java.io.File
import java.io.IOException
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.*
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.web.multipart.MultipartFile


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

    override fun <T> sendPostRequest(url: String, body: HttpBody, responseType: Class<T>, headers: HttpHeaders): ResponseEntity<T> {
        val entity = HttpEntity(body, headers)
        return restTemplate.postForEntity(url, entity, responseType)
//        return restTemplate.exchange(url, HttpMethod.POST, entity, responseType)
    }

    override fun <T> sendPutRequest(url: String, body: HttpBody, responseType: Class<T>, headers: HttpHeaders): ResponseEntity<T> {
        val entity = HttpEntity(body, headers)
        return restTemplate.exchange(url, HttpMethod.PUT, entity, responseType)
    }

    fun p(multipartFiles: List<MultipartFile>): String {
        val client = OkHttpClient()

        val requestBodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "info",
                "{ \"trikStudioVersion\": \"karasss/trik-studio:master-2022.2-35-g2913f5\", \"timeout\": 120 }"
            )

        multipartFiles.forEach {
            requestBodyBuilder.addFormDataPart("files", it.name, it.inputStream.readBytes().toRequestBody())
        }

        val request = Request.Builder()
            .url("https://srv3.trikset.com/v1/grading-system/problems")
            .post(requestBodyBuilder.build())
            .header("accept", "text/plain")
            .header("Authorization", "Basic dGVzdHN5c191c2VybmFtZTp0ZXN0c3lzX3BAc3N3b3JkX0oxbzg1QnRDaWJDQVJ5Qw==")
            .header("Content-Type", "multipart/form-data")
            .build()

        val a = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            response.body!!.string()
        }

        return a
    }
}