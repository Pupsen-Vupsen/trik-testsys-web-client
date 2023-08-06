package trik.testsys.webclient.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.ModelAndView

import trik.testsys.webclient.util.handler.GradingSystemErrorHandler
import trik.testsys.webclient.entity.Developer
import trik.testsys.webclient.entity.WebUser
import trik.testsys.webclient.model.DeveloperModel
import trik.testsys.webclient.service.DeveloperService
import trik.testsys.webclient.service.TaskService
import trik.testsys.webclient.service.WebUserService
import trik.testsys.webclient.util.fp.Either
import trik.testsys.webclient.util.logger.TrikLogger
import java.net.http.HttpResponse.ResponseInfo

/**
 * @author Roman Shishkin
 * @since 1.1.0
 */
@RestController
@RequestMapping("\${app.testsys.api.prefix}/developer")
class DeveloperController @Autowired constructor(
    @Value("\${app.grading-system.path}")
    private val gradingSystemUrl: String,

    private val developerService: DeveloperService,
    private val webUserService: WebUserService,
    private val taskService: TaskService
) {

    @GetMapping("/test")
    fun test(): ModelAndView {
        val modelAndView = ModelAndView("developer")
        modelAndView.addObject("username", "Roman")
        modelAndView.addObject("accessToken", "ed30da0f75d595465d6977e2fd551d2026cc3ff66dd5bd958ac2a50807684cb7")
        println(modelAndView.model)
        return modelAndView
    }

    @GetMapping
    fun getAccess(
        @RequestParam accessToken: String,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Client trying to access developer page.")

        val eitherDeveloperEntities = validateDeveloper(accessToken)
        if (eitherDeveloperEntities.isLeft()) {
            return eitherDeveloperEntities.getLeft()
        }

        val (_, webUser) = eitherDeveloperEntities.getRight()
        val username = webUser.username

        val developerModel = DeveloperModel.Builder()
            .accessToken(accessToken)
            .username(username)
            .build()

        modelAndView.viewName = DEVELOPER_VIEW_NAME
        modelAndView.addAllObjects(developerModel.asMap())
        return modelAndView
    }

    @PostMapping("/task/create")
    fun createTask(
        @RequestParam accessToken: String,
        @RequestParam name: String,
        @RequestParam description: String,
        @RequestBody tests: List<MultipartFile>,
        @RequestBody benchmark: MultipartFile?,
        @RequestBody training: MultipartFile?,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Client trying to create task.")

        val eitherDeveloperEntities = validateDeveloper(accessToken)
        if (eitherDeveloperEntities.isLeft()) {
            return eitherDeveloperEntities.getLeft()
        }
        modelAndView.viewName = DEVELOPER_VIEW_NAME

        val (developer, webUser) = eitherDeveloperEntities.getRight()
        val developerModelBuilder = DeveloperModel.Builder()
            .accessToken(accessToken)
            .username(webUser.username)

        modelAndView.addObject("accessToken", accessToken)

        val testsCount = tests.size.toLong()
        val task = taskService.saveTask(name, description, testsCount, developer, training, benchmark)

        val isTaskPosted = postTask(name, tests, benchmark, training)
        if (!isTaskPosted) {
            developerModelBuilder.postTaskMessage("Задача '${task.fullName}' не была загружена на сервер, попробуйте еще раз.")
            val developerModel = developerModelBuilder.build()
            modelAndView.addAllObjects(developerModel.asMap())

            return modelAndView
        }
        logger.info(accessToken, "Task '${task.fullName}' was successfully posted.")

        developerModelBuilder.postTaskMessage("Задача '${task.fullName}' была успешно загружена на сервер.")
        val developerModel = developerModelBuilder.build()
        modelAndView.addAllObjects(developerModel.asMap())

        return modelAndView
    }

    private fun postTask(
        name: String,
        tests: List<MultipartFile>,
        benchmark: MultipartFile?,
        training: MultipartFile?
    ): Boolean {
        val restTemplate = RestTemplate()
        restTemplate.errorHandler = GradingSystemErrorHandler()

        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA
        headers.setBasicAuth("admin", "admin") // TODO("Change to real credentials.")

        val body = LinkedMultiValueMap<String, Any>()
        body.add("taskName", name)

        tests.forEach { body.add("files", it.resource) }
        benchmark?.let { body.add("benchmark", it.resource) }
        training?.let { body.add("training", it.resource) }

        val url = "$gradingSystemUrl/task/create"
        val responseInfo = restTemplate.postForEntity(
            url,
            body,
            Map::class.java
        )

        if (responseInfo.statusCode != HttpStatus.OK) {
            logger.error("Error while creating task '$name': ${responseInfo.statusCode}")
            return false
        }

        return true
    }

    private fun validateDeveloper(accessToken: String): Either<ModelAndView, DeveloperEntities> {
        val modelAndView = ModelAndView("error")
        val webUser = webUserService.getWebUserByAccessToken(accessToken) ?: run {
            logger.info(accessToken, "Client not found.")
            modelAndView.addObject("message", "Client not found.")

            return Either.left(modelAndView)
        }
        val developer = developerService.getByWebUser(webUser) ?: run {
            logger.info(accessToken, "Client is not a developer.")
            modelAndView.addObject("message", "You are not a developer.")

            return Either.left(modelAndView)
        }
        val developerEntities = DeveloperEntities(developer, webUser)

        logger.info(accessToken, "Client is a developer.")
        return Either.right(developerEntities)
    }

    private data class DeveloperEntities(
        val developer: Developer,
        val webUser: WebUser,
    )

    companion object {
        private val logger = TrikLogger(this::class.java)

        private const val DEVELOPER_VIEW_NAME = "developer"
        private const val POST_TASK_MESSAGE = "postTaskMessage"
    }
}