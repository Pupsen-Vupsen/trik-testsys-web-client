package trik.testsys.webclient.controllers

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

import trik.testsys.webclient.GradingSystemErrorHandler
import trik.testsys.webclient.enums.SolutionsStatuses
import trik.testsys.webclient.services.SolutionService

@Controller
class SolutionController(@Value("\${app.grading-system-url}") val gradingSystemUrl: String) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private val restTemplate = RestTemplate()

    @Autowired
    private lateinit var solutionService: SolutionService

    @Scheduled(fixedRate = 30_000)
    private fun checkSolutions() {
        restTemplate.errorHandler = GradingSystemErrorHandler()

        logger.info("Checking solutions...")
        val solutions = solutionService.getAllSolutions()
        val submissionIds = solutions.map { it.gradingId }

        val headers = HttpHeaders()
        headers.setBasicAuth("user1", "super")

        val url = "$gradingSystemUrl/submissions/status?id_array=${submissionIds.joinToString(",")}"

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
                    solution.status = SolutionsStatuses.NOT_STARTED
                }
                102 -> {
                    logger.info("Solution ${solution.id} is in progress.")
                    solution.status = SolutionsStatuses.IN_PROGRESS
                }
                202 -> {
                    logger.info("Solution ${solution.id} is accepted.")
                    solution.status = SolutionsStatuses.PASSED
                }
                406 -> {
                    logger.info("Solution ${solution.id} is failed.")
                    solution.status = SolutionsStatuses.FAILED
                }
                500 -> {
                    logger.info("Solution ${solution.id} is error.")
                    solution.status = SolutionsStatuses.FAILED
                }
                null -> {
                    logger.info("Solution ${solution.id} is not found.")
                    solution.status = SolutionsStatuses.ERROR
                }
            }
            solutionService.saveSolution(solution)
        }

        logger.info("Checking solutions finished.")
    }
}