package trik.testsys.webclient.controller.impl

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Controller
import org.springframework.web.client.RestTemplate

import trik.testsys.webclient.entity.impl.Solution
import trik.testsys.webclient.service.impl.SolutionService
import trik.testsys.webclient.util.handler.GradingSystemErrorHandler

/**
 * @author Roman Shishkin
 * @since 1.0.0
 */
@Controller
class SolutionController(@Value("\${app.grading-system.url}") val gradingSystemUrl: String) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private val restTemplate = RestTemplate()

    @Autowired
    private lateinit var solutionService: SolutionService

//    @Scheduled(fixedRate = CHECK_INTERVAL)
    private fun checkSolutions() {
        restTemplate.errorHandler = GradingSystemErrorHandler()

        logger.info("Checking solutions...")
        val solutions = solutionService.getAllSolutions()
        val submissionIds = solutions.map { it.gradingId }
        val idsString = submissionIds.joinToString(",")

        val headers = HttpHeaders()
        headers.setBasicAuth("admin", "@dm1n")

        val url = "$gradingSystemUrl$GRADING_SYSTEM_ENDPOINT$idsString"

        val response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity(null, headers),
            List::class.java
        )

        val statuses = response.body!!.map { it as Map<*, *> }.associate { it["id"] as Int to it["status"] as Int }

        for (solution in solutions) {
            when (statuses[solution.gradingId.toInt()]) {
                100 -> {
                    logger.info("Solution ${solution.id} is in queue.")
                    solution.status = Solution.Status.NOT_STARTED
                }
                102 -> {
                    logger.info("Solution ${solution.id} is in progress.")
                    solution.status = Solution.Status.IN_PROGRESS
                }
                202 -> {
                    logger.info("Solution ${solution.id} is accepted.")
                    solution.status = Solution.Status.PASSED
                }
                406 -> {
                    logger.info("Solution ${solution.id} is failed.")
                    solution.status = Solution.Status.FAILED
                }
                500 -> {
                    logger.info("Solution ${solution.id} is error.")
                    solution.status = Solution.Status.FAILED
                }
                null -> {
                    logger.info("Solution ${solution.id} is not found.")
                    solution.status = Solution.Status.ERROR
                }
            }
            solutionService.saveSolution(solution)
        }

        logger.info("Checking solutions finished.")
    }

    companion object {
        private const val CHECK_INTERVAL = 30_000L
        private const val GRADING_SYSTEM_ENDPOINT = "/submissions/status?id_array="
    }
}