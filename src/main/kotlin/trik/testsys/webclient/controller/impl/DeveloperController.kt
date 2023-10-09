package trik.testsys.webclient.controller.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.format.annotation.DateTimeFormat
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
import org.springframework.web.servlet.view.RedirectView
import trik.testsys.webclient.controller.TrikUserController

import trik.testsys.webclient.util.handler.GradingSystemErrorHandler
import trik.testsys.webclient.entity.impl.Developer
import trik.testsys.webclient.entity.impl.WebUser
import trik.testsys.webclient.model.impl.DeveloperModel
import trik.testsys.webclient.service.impl.*
import trik.testsys.webclient.util.fp.Either
import trik.testsys.webclient.util.logger.TrikLogger
import java.time.LocalDateTime

/**
 * @author Roman Shishkin
 * @since 1.1.0
 */
@RestController
@RequestMapping("\${app.testsys.api.prefix}/developer")
class DeveloperController @Autowired constructor(
    @Value("\${app.grading-system.path}")
    private val gradingSystemUrl: String,

    @Value("\${app.testsys.api.prefix}")
    private val apiPrefix: String,

    private val developerService: DeveloperService,
    private val webUserService: WebUserService,
    private val taskService: TaskService,
    private val adminService: AdminService,
    private val groupService: GroupService
) : TrikUserController {

    @GetMapping
    override fun getAccess(
        @RequestParam accessToken: String,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Client trying to access developer page.")

        val eitherDeveloperEntities = validateDeveloper(accessToken)
        if (eitherDeveloperEntities.isLeft()) {
            return eitherDeveloperEntities.getLeft()
        }

        val (developer, _) = eitherDeveloperEntities.getRight()

        val developerModel = getModel(developer)

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
        modelAndView.view = REDIRECT_VIEW

        val (developer, webUser) = eitherDeveloperEntities.getRight()
        val developerModelBuilder = DeveloperModel.Builder()
            .accessToken(accessToken)
            .username(webUser.username)

        modelAndView.addObject("accessToken", accessToken)

        val task = taskService.saveTask(name, description, developer, tests, training, benchmark)
        postTask(name, tests, benchmark, training)
//        val isTaskPosted = postTask(name, tests, benchmark, training)
//        if (!isTaskPosted) {
//            developerModelBuilder.postTaskMessage("Задача '${task.fullName}' не была загружена на сервер, попробуйте еще раз.")
//            val developerModel = developerModelBuilder.build()
//            modelAndView.addAllObjects(developerModel.asMap())
//
//            return modelAndView
//        }
        logger.info(accessToken, "Task '${task.getFullName()}' was successfully posted.")

        developerModelBuilder.postTaskMessage("Задача '${task.getFullName()}' была успешно загружена на сервер.")
        developerModelBuilder.tasks(developer.tasks)
        developerModelBuilder.admins(adminService.getAll())
        val developerModel = developerModelBuilder.build()
        modelAndView.addAllObjects(developerModel.asMap())

        return modelAndView
    }

    @PostMapping("/task/delete")
    fun deleteTask(
        @RequestParam accessToken: String,
        @RequestParam taskId: Long,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Client trying to delete task.")

        val eitherDeveloperEntities = validateDeveloper(accessToken)
        if (eitherDeveloperEntities.isLeft()) {
            return eitherDeveloperEntities.getLeft()
        }
        modelAndView.view = REDIRECT_VIEW

        val (developer, webUser) = eitherDeveloperEntities.getRight()
        val developerModelBuilder = DeveloperModel.Builder()
            .accessToken(accessToken)
            .username(webUser.username)

        modelAndView.addObject("accessToken", accessToken)

        val task = taskService.getTaskById(taskId) ?: run {
            developerModelBuilder.postTaskMessage("Задача с id '${taskId}' не найдена.")
            val developerModel = developerModelBuilder
                .tasks(developer.tasks)
                .admins(adminService.getAll())
                .build()
            modelAndView.addAllObjects(developerModel.asMap())

            return modelAndView
        }

        val isTaskDeleted = taskService.deleteTask(taskId)
        if (!isTaskDeleted) {
            developerModelBuilder.postTaskMessage("Задача '${task.getFullName()}' не была удалена с сервера, попробуйте еще раз.")
            val developerModel = developerModelBuilder.build()
            modelAndView.addAllObjects(developerModel.asMap())

            return modelAndView
        }
        logger.info(accessToken, "Task '${task.getFullName()}' was successfully deleted.")

        developerModelBuilder.postTaskMessage("Задача '${task.getFullName()}' была успешно удалена с сервера.")
        val developerModel = developerModelBuilder
            .tasks(developer.tasks)
            .admins(adminService.getAll())
            .build()
        modelAndView.addAllObjects(developerModel.asMap())

        return modelAndView
    }

    @PostMapping("/task/changeDeadline")
    fun changeTaskDeadline(
        @RequestParam accessToken: String,
        @RequestParam taskId: Long,
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") deadline: LocalDateTime,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Client trying to create task.")

        val eitherDeveloperEntities = validateDeveloper(accessToken)
        if (eitherDeveloperEntities.isLeft()) {
            return eitherDeveloperEntities.getLeft()
        }
        modelAndView.viewName = DEVELOPER_VIEW_NAME

        val (developer, webUser) = eitherDeveloperEntities.getRight()

        val task = taskService.getTaskById(taskId)
        if (task != null) {
            val serverDeadline = deadline.minusHours(UTC_OFFSET)
            task.deadline = serverDeadline
            taskService.saveTask(task)
        }

        val developerModel = getModel(developer)

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
        headers.setBasicAuth("admin", "@dm1n") // TODO("Change to real credentials.")

        val body = LinkedMultiValueMap<String, Any>()
        body.add("taskName", name)

        tests.forEach { body.add("files", it.resource) }
//        benchmark?.let { body.add("benchmark", it.resource) }
//        training?.let { body.add("training", it.resource) }

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

    @PostMapping("/task/edit")
    fun editTask(
        @RequestParam accessToken: String,
        @RequestParam taskId: Long,
        @RequestParam name: String,
        @RequestParam description: String,
        @RequestBody tests: List<MultipartFile>,
        @RequestBody benchmark: MultipartFile?,
        @RequestBody training: MultipartFile?,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Client trying to update task.")

        val eitherDeveloperEntities = validateDeveloper(accessToken)
        if (eitherDeveloperEntities.isLeft()) {
            return eitherDeveloperEntities.getLeft()
        }
        modelAndView.view = REDIRECT_VIEW

        val (developer, webUser) = eitherDeveloperEntities.getRight()
        val developerModelBuilder = DeveloperModel.Builder()
            .accessToken(accessToken)
            .username(webUser.username)

        modelAndView.addObject("accessToken", accessToken)

        val testsCount = tests.size.toLong()
        val task = taskService.update(taskId, name, description, tests, training, benchmark) ?: run {
            logger.warn(accessToken, "Task with id '$taskId' not found.")

            developerModelBuilder.postTaskMessage("Задача с id '$taskId' не найдена.")
            val developerModel = developerModelBuilder.build()
            modelAndView.addAllObjects(developerModel.asMap())

            return modelAndView
        }

//        val isTaskPosted = postTask(name, tests, benchmark, training)
//        if (!isTaskPosted) {
//            developerModelBuilder.postTaskMessage("Задача '${task.fullName}' не была загружена на сервер, попробуйте еще раз.")
//            val developerModel = developerModelBuilder.build()
//            modelAndView.addAllObjects(developerModel.asMap())
//
//            return modelAndView
//        }
        logger.info(accessToken, "Task '${task.getFullName()}' was successfully updated.")

        developerModelBuilder.postTaskMessage("Задача '${task.getFullName()}' была успешно загружена на сервер.")
        developerModelBuilder.tasks(developer.tasks)
        developerModelBuilder.admins(adminService.getAll())
        val developerModel = developerModelBuilder.build()
        modelAndView.addAllObjects(developerModel.asMap())

        return modelAndView
    }

    @PostMapping("/task/attach")
    fun attachTaskToAdmin(
        @RequestParam accessToken: String,
        @RequestParam taskId: Long,
        @RequestParam adminIds: List<Long>,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Client trying to attach task to admins.")

        val eitherDeveloperEntities = validateDeveloper(accessToken)
        if (eitherDeveloperEntities.isLeft()) {
            return eitherDeveloperEntities.getLeft()
        }
        modelAndView.view = REDIRECT_VIEW

        val (developer, webUser) = eitherDeveloperEntities.getRight()

        val task = taskService.getTaskById(taskId)
        if (task == null) {
            logger.warn(accessToken, "Task with id '$taskId' not found.")

            val postTaskMessage = "Задача с id '$taskId' не найдена."

            val developerModel = getModel(developer, postTaskMessage)
            modelAndView.addAllObjects(developerModel.asMap())

            return modelAndView
        }

        val admins = adminService.getAllByIds(adminIds)
        task.admins.addAll(admins)
        taskService.saveTask(task)

        logger.info(accessToken, "Task '${task.getFullName()}' was successfully attached to admins.")

        val postTaskMessage = "Задача '${task.getFullName()}' была успешно прикреплена к администраторам."

        val developerModel = getModel(developer, postTaskMessage)
        modelAndView.addAllObjects(developerModel.asMap())

        return modelAndView
    }

    @PostMapping("/task/detach")
    fun detachTaskToAdmin(
        @RequestParam accessToken: String,
        @RequestParam taskId: Long,
        @RequestParam adminIds: List<Long>,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Client trying to detach task from admins.")

        val eitherDeveloperEntities = validateDeveloper(accessToken)
        if (eitherDeveloperEntities.isLeft()) {
            return eitherDeveloperEntities.getLeft()
        }
        modelAndView.view = REDIRECT_VIEW

        val (developer, webUser) = eitherDeveloperEntities.getRight()

        val task = taskService.getTaskById(taskId)
        if (task == null) {
            logger.warn(accessToken, "Task with id '$taskId' not found.")

            val postTaskMessage = "Задача с id '$taskId' не найдена."

            val developerModel = getModel(developer, postTaskMessage)
            modelAndView.addAllObjects(developerModel.asMap())

            return modelAndView
        }

        val admins = adminService.getAllByIds(adminIds)
        task.admins.removeAll(admins.toSet())
        taskService.saveTask(task)

        admins.forEach { admin ->
            admin.tasks.remove(task)

            val groups = admin.groups
            groups.forEach { group ->
                group.tasks.remove(task)
            }
            groupService.saveAll(groups)
        }
        adminService.saveAll(admins)

        logger.info(accessToken, "Task '${task.getFullName()}' was successfully detached from admins.")

        val postTaskMessage = "Задача '${task.getFullName()}' была успешно откреплена от администраторов."

        val developerModel = getModel(developer, postTaskMessage)
        modelAndView.addAllObjects(developerModel.asMap())

        return modelAndView
    }

    @PostMapping("/info/change")
    fun changeInfo(
        @RequestParam accessToken: String,
        @RequestParam newUsername: String,
        @RequestParam newAdditionalInfo: String,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Developer trying to change info.")

        val eitherDeveloperEntities = validateDeveloper(accessToken)
        if (eitherDeveloperEntities.isLeft()) {
            return eitherDeveloperEntities.getLeft()
        }
        modelAndView.view = REDIRECT_VIEW

        val (developer, webUser) = eitherDeveloperEntities.getRight()

        webUser.username = newUsername
        webUser.additionalInfo = newAdditionalInfo
        webUserService.saveWebUser(webUser)

        logger.info(accessToken, "Developer info was successfully changed.")

        val developerModel = getModel(developer)
        modelAndView.addAllObjects(developerModel.asMap())

        return modelAndView
    }

    @PostMapping("/task/visibility/change")
    fun changeVisibility(
        @RequestParam accessToken: String,
        @RequestParam taskId: Long,
        @RequestParam isPublic: Boolean,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Developer trying to make task public.")

        val eitherDeveloperEntities = validateDeveloper(accessToken)
        if (eitherDeveloperEntities.isLeft()) {
            return eitherDeveloperEntities.getLeft()
        }
        modelAndView.view = REDIRECT_VIEW

        val (developer, _) = eitherDeveloperEntities.getRight()

        val task = taskService.getTaskById(taskId)
        if (task == null) {
            logger.warn(accessToken, "Task with id '$taskId' not found.")

            val postTaskMessage = "Задача с id '$taskId' не найдена."

            val developerModel = getModel(developer, postTaskMessage)
            modelAndView.addAllObjects(developerModel.asMap())

            return modelAndView
        }

        if (isPublic) {
            val allAdmins = adminService.getAll()
            task.admins.addAll(allAdmins)

            allAdmins.forEach { admin ->
                admin.tasks.add(task)
                adminService.save(admin)
            }

            taskService.saveTask(task)
        } else {
            val allAdmins = adminService.getAll()
            task.admins.removeAll(allAdmins.toSet())

            allAdmins.forEach { admin ->
                admin.tasks.remove(task)
                adminService.save(admin)
            }

            taskService.saveTask(task)
        }

        task.isPublic = isPublic
        taskService.saveTask(task)

        logger.info(accessToken, "Task '${task.getFullName()}' was successfully made public.")

        val postTaskMessage = "Задача '${task.getFullName()}' была успешно сделана публичной."

        val developerModel = getModel(developer, postTaskMessage)
        modelAndView.addAllObjects(developerModel.asMap())

        return modelAndView
    }

    private fun getModel(developer: Developer, postTaskMessage: String? = null): DeveloperModel {
        val webUser = developer.webUser

        val publicTasks = taskService.getAllPublic()

        val developerModel = DeveloperModel.Builder()
            .accessToken(webUser.accessToken)
            .username(webUser.username)
            .postTaskMessage(postTaskMessage)
            .tasks(developer.tasks)
            .publicTasks(publicTasks)
            .admins(adminService.getAll())
            .additionalInfo(webUser.additionalInfo)
            .lastLoginDate(webUser.lastLoginDate)
            .build()

        return developerModel
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
        private const val SERVER_PREFIX = "https://testsys.trikset.com"
        private val REDIRECT_VIEW = RedirectView("${SERVER_PREFIX}/v1/testsys/$DEVELOPER_VIEW_NAME")
        private const val UTC_OFFSET = 3L
    }
}