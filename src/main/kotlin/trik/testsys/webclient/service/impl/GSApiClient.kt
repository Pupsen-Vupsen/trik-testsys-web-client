package trik.testsys.webclient.service.impl

import okhttp3.Headers
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.multipart.MultipartFile
import trik.testsys.webclient.service.HttpBody
import trik.testsys.webclient.service.TrikApiClient
import trik.testsys.webclient.util.logger.TrikLogger
import java.util.Base64

/**
 * @author Roman Shishkin
 * @since 1.1.0.14-alpha
 */
@Component
@Suppress("UnnecessaryVariable")
class GSApiClient @Autowired constructor(
    @Value("\${app.grading-system.url}") private val url: String,
    @Value("\${app.grading-system.username}") private val username: String,
    @Value("\${app.grading-system.password}") private val password: String,

    private val httpClient: BasicHttpClient
) : TrikApiClient {

    //region Problem
    /**
     * Gets problem info by id. Response returns JSON contains:
     * fileIds array, info (trikInfo) and problem id.
     */
    fun getProblem(id: Long): ProblemDto? {
        val result = get("$PROBLEMS_ENDPOINT$id", ProblemDto::class.java) ?: return null
        logger.info("Response: ${result.body}")

        return result.body
    }

    /**
     * Updates problem, changes problem info (trikInfo) and problem files.
     */
//    fun putProblem(id: Long, info: String, files: List<ByteArray>): ProblemDto? {
//        val body = createProblemBody(info, files)
//        val result = put("$PROBLEMS_ENDPOINT$id", body, ProblemDto::class.java) ?: return null
//        logger.info("Response: ${result.body}")
//
//        return result.body
//    }

    /**
     * Creates new problem.
     */
    fun postProblem(info: String, files: List<MultipartFile>): ProblemDto? {
        val body = createProblemBody(info, files)

        val result = post(PROBLEMS_ENDPOINT, body, ProblemDto::class.java) ?: return null
        logger.info("Response: ${result}")

        return null
    }
    //endregion

    //region Problem files
    /**
     * Gets problem file by its id.
     */
    fun getProblemFile(fileId: Long): ByteArray? {
        val result = get("$PROBLEM_FILES_ENDPOINT$fileId", ByteArray::class.java) ?: return null
        logger.info("Response: ${result.body}")

        return result.body
    }
    //endregion

    //region Submission
    /**
     * Gets submission info by id. Response returns JSON contains:
     * problemId, fileIds array, gradingInfo, gradingStatus and gradingResultInfo.
     */
    fun getSubmission(id: Long): SubmissionDto? {
        val result = get("$SUBMISSIONS_ENDPOINT$id", SubmissionDto::class.java) ?: return null
        logger.info("Response: ${result.body}")

        return result.body
    }

    /**
     * Rechecks submission by id.
     */
    fun postSubmission(id: Long): Boolean {
        val body = EMPTY_BODY

        val result = post("$SUBMISSIONS_ENDPOINT$id", body, Void::class.java) ?: return false
        logger.info("Response: ${result.body}")

        return true
    }

    /**
     * Posts submission.
     */
    fun postSubmission(
        problemId: Long,
        gradingInfo: String?,
        files: List<ByteArray>
    ): SubmissionDto? {
        val body = createSubmissionBody(problemId, gradingInfo, files)

        val result = post(SUBMISSIONS_ENDPOINT, body, SubmissionDto::class.java) ?: return null
        logger.info("Response: ${result.body}")

        return result.body
    }
    //endregion

    //region Submission files
    /**
     * Gets submission file by its id. returns response entity with application/octet-stream content type.
     */
    fun getSubmissionFile(id: Long): ByteArray? {
        val result = get("$SUBMISSION_FILES_ENDPOINT$id", ByteArray::class.java) ?: return null
        logger.info("Response: ${result.body}")

        return result.body
    }
    //endregion

    private fun createProblemBody(info: String, files: List<Array<Byte>>): HttpBody {
        val body = LinkedMultiValueMap<String, Any>()
        body.add("info", info)
        files.forEach { byteArray -> body.add("files", byteArray) }

        return body
    }

    private fun createProblemBody(info: String, files: List<MultipartFile>): MultipartBody {
        val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
        bodyBuilder.addFormDataPart("info", info)

        files.forEach { file ->
            bodyBuilder.addFormDataPart("files", file.name, file.bytes.toRequestBody())
        }
        val body = bodyBuilder.build()

        return body
    }

    private fun createSubmissionBody(problemId: Long, gradingInfo: String?, files: List<ByteArray>): HttpBody {
        val body = LinkedMultiValueMap<String, Any>()
        body.add("problemId", problemId)
        body.add("gradingInfo", gradingInfo)
        files.forEach { byteArray -> body.add("files", byteArray) }

        return body
    }

    data class ProblemDto(
        val id: Long,
        val fileIds: List<Long>?,
        val info: String
    )

    data class SubmissionDto(
        val id: Long,
        val problemId: Long,
        val fileIds: List<Long>?,
        val gradingInfo: String?,
        val gradingStatus: String?,
        val gradingResultInfo: String?
    )

    private fun <T> get(endPoint: String, responseType: Class<T>): ResponseEntity<T>? {
        val fullUrl = "$url/$endPoint"
        logger.info("Sending GET request to $fullUrl")

        val headers = setUpHeaders()
        val responseEntity = getResponseOrNull { httpClient.sendGetRequest(fullUrl, responseType, headers) }

        return responseEntity
    }

    private fun <T> post(endPoint: String, body: HttpBody, responseType: Class<T>): ResponseEntity<T>? {
        val fullUrl = "$url/$endPoint"
        logger.info("Sending POST request to $fullUrl")

        val headers = setUpHeadersWithContentType()
        val responseEntity = getResponseOrNull { httpClient.sendPostRequest(fullUrl, body, responseType, headers) }

        return responseEntity
    }

    private fun <T> post(endPoint: String, body: MultipartBody, responseType: Class<T>): String? {
        val fullUrl = "$url/$endPoint"
        logger.info("Sending POST request to $fullUrl")

        val headers = setUpOkHttpHeaders()

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(fullUrl)
            .post(body)
            .header("accept", "text/plain")
            .header("Authorization", "Basic dGVzdHN5c191c2VybmFtZTp0ZXN0c3lzX3BAc3N3b3JkX0oxbzg1QnRDaWJDQVJ5Qw==")
            .header("Content-Type", "multipart/form-data")
            .build()

        val response = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                logger.error("Unexpected code $response")
                return null
            }
            response
        }

        val bodyString = response.body?.string() ?: return null
        return bodyString
    }

    private fun <T> put(endPoint: String, body: HttpBody, responseType: Class<T>): ResponseEntity<T>? {
        val fullUrl = "$url/$endPoint"
        logger.info("Sending PUT request to $fullUrl")

        val headers = setUpHeadersWithContentType()
        val responseEntity = getResponseOrNull { httpClient.sendPutRequest(fullUrl, body, responseType, headers) }

        return responseEntity
    }

    private fun setUpHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.setBasicAuth(username, password)
        return headers
    }

    private fun setUpOkHttpHeaders(): Headers {
        val headersBuilder = Headers.Builder()
        val basicAuth = Base64.getEncoder().encodeToString("$username:$password".toByteArray())

        headersBuilder.add("Authorization", "Basic $basicAuth")
        headersBuilder.add("accept", "text/plain")
        headersBuilder.add("Content-Type", "multipart/form-data")
        return headersBuilder.build()
    }

    private fun setUpHeadersWithContentType(): HttpHeaders {
        val headers = setUpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA
        return headers
    }

    private fun <T> getResponseOrNull(response: () -> ResponseEntity<T>): ResponseEntity<T>? {
        return try {
            val result = response()
            if (!result.statusCode.is2xxSuccessful) {
                logger.error("Response status code is not OK: ${result.statusCode}")
                return null
            }

            result
        } catch (e: Exception) {
            logger.error(e.message)
            logger.error(e.stackTraceToString())
            null
        }
    }

    companion object {
        private val logger = TrikLogger(this::class.java)

        private const val PROBLEMS_ENDPOINT = "problems/"
        private const val PROBLEM_FILES_ENDPOINT = "${PROBLEMS_ENDPOINT}files/"

        private const val SUBMISSIONS_ENDPOINT = "submissions/"
        private const val SUBMISSION_FILES_ENDPOINT = "${SUBMISSIONS_ENDPOINT}files/"

        private val EMPTY_BODY = LinkedMultiValueMap<String, Any>()
    }
}