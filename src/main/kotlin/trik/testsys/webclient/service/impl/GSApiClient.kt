package trik.testsys.webclient.service.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import trik.testsys.webclient.service.TrikApiClient
import trik.testsys.webclient.util.logger.TrikLogger
import java.io.File
import javax.xml.crypto.OctetStreamData

/**
 * @author Roman Shishkin
 * @since 1.1.0.14-alpha
 */
@Component
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
    fun getProblemById(id: Long): ResponseEntity<ProblemDto> {
        val result = get("$PROBLEMS_ENDPOINT$id", ProblemDto::class.java)
        logger.info("Response: ${result.body}")

        return result
    }

    /**
     * Updates problem, changes problem info (trikInfo) and problem files.
     */
    fun putProblemById(id: Long, info: String, files: List<MultipartFile>): ResponseEntity<ProblemDto> {
        val body = mapOf(
            "info" to info,
            "files" to files
        )
        val result = put("$PROBLEMS_ENDPOINT$id", body, ProblemDto::class.java)
        logger.info("Response: ${result.body}")

        return result
    }

    /**
     * Creates new problem.
     */
    fun postProblem(info: String, files: List<MultipartFile>): ResponseEntity<ProblemDto> {
        val body = mapOf(
            "info" to info,
            "files" to files
        )

        val result = post(PROBLEMS_ENDPOINT, body, ProblemDto::class.java)
        logger.info("Response: ${result.body}")

        return result
    }
    //endregion

    //region Problem files
    /**
     * Gets problem file by its id.
     */
    fun getProblemFileById(id: Long): ResponseEntity<MultipartFile> {
        val result = get("$PROBLEM_FILES_ENDPOINT$id", MultipartFile::class.java)
        logger.info("Response: ${result.body}")

        return result
    }
    //endregion

    //region Submission
    /**
     * Gets submission info by id. Response returns JSON contains:
     * problemId, fileIds array, gradingInfo, gradingStatus and gradingResultInfo.
     */
    fun getSubmissionById(id: Long): ResponseEntity<SubmissionDto> {
        val result = get("$SUBMISSIONS_ENDPOINT$id", SubmissionDto::class.java)
        logger.info("Response: ${result.body}")

        return result
    }

    /**
     * Rechecks submission by id.
     */
    fun postSubmission(id: Long): ResponseEntity<SubmissionDto> {
        val body = EMPTY_BODY

        val result = post("$SUBMISSIONS_ENDPOINT$id", body, SubmissionDto::class.java)
        logger.info("Response: ${result.body}")

        return result
    }

    /**
     * Posts submission.
     */
    fun postSubmission(problemId: Long, gradingInfo: String?, files: List<MultipartFile>): ResponseEntity<SubmissionDto> {
        val body = mapOf(
            "problemId" to problemId,
            "gradingInfo" to gradingInfo,
            "files" to files
        )

        val result = post(SUBMISSIONS_ENDPOINT, body, SubmissionDto::class.java)
        logger.info("Response: ${result.body}")

        return result
    }
    //endregion

    //region Submission files
    /**
     * Gets submission file by its id. returns response entity with application/octet-stream content type.
     */
    fun getSubmissionFile(id: Long): ResponseEntity<ByteArray> {
        val result = get("$SUBMISSION_FILES_ENDPOINT$id", ByteArray::class.java)
        logger.info("Response: ${result.body}")

        return result
    }
    //endregion

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

    private fun <T> get(endPoint: String, responseType: Class<T>): ResponseEntity<T> {
        val fullUrl = "$url/$endPoint"
        logger.info("Sending GET request to $fullUrl")

        val headers = setUpHeaders()
        return httpClient.sendGetRequest(fullUrl, responseType, headers)
    }

    private fun <T> post(endPoint: String, body: Any, responseType: Class<T>): ResponseEntity<T> {
        val fullUrl = "$url/$endPoint"
        logger.info("Sending POST request to $fullUrl")

        val headers = setUpHeaders()
        return httpClient.sendPostRequest(fullUrl, body, responseType, headers)
    }

    private fun <T> put(endPoint: String, body: Any, responseType: Class<T>): ResponseEntity<T> {
        val fullUrl = "$url/$endPoint"
        logger.info("Sending PUT request to $fullUrl")

        val headers = setUpHeaders()
        return httpClient.sendPutRequest(fullUrl, body, responseType, headers)
    }

    private fun setUpHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.setBasicAuth(username, password)
        return headers
    }

    companion object {
        private val logger = TrikLogger(this::class.java)

        private const val PROBLEMS_ENDPOINT = "problems/"
        private const val PROBLEM_FILES_ENDPOINT = "${PROBLEMS_ENDPOINT}files/"

        private const val SUBMISSIONS_ENDPOINT = "submissions/"
        private const val SUBMISSION_FILES_ENDPOINT = "${SUBMISSIONS_ENDPOINT}files/"

        private const val TRIK_INFO =
            "{ \"trikStudioVersion\": \"karasss/trik-studio:master-2022.2-35-g2913f5\", \"timeout\": 120 }"

        private const val EMPTY_BODY = ""
    }
}