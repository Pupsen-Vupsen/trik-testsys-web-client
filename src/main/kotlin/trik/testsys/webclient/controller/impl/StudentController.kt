package trik.testsys.webclient.controller.impl

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.ui.Model
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartFile

import trik.testsys.webclient.util.handler.GradingSystemErrorHandler
import trik.testsys.webclient.entity.impl.WebUser
import trik.testsys.webclient.models.ResponseMessage
import trik.testsys.webclient.service.impl.*
import java.time.LocalDateTime

@RestController
@RequestMapping("\${app.testsys.api.prefix}/student")
class StudentController(@Value("\${app.grading-system.url}") val gradingSystemUrl: String) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Autowired
    private lateinit var studentService: StudentService

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var webUserService: WebUserService

    @Autowired
    private lateinit var solutionService: SolutionService

    @Autowired
    private lateinit var taskService: TaskService

    private val restTemplate = RestTemplate()

    @GetMapping("/registration")
    fun registration(@RequestParam groupAccessToken: String, model: Model): Any {
        logger.info("[${groupAccessToken.padStart(80)}]: Client trying to register.")

        val group = groupService.getGroupByAccessToken(groupAccessToken) ?: run {
            logger.info("[${groupAccessToken.padStart(80)}]: Invalid  group token.")

            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ResponseMessage(403, "Invalid group token!"))
        }

        if (!group.isAccessible) {
            logger.info("[${groupAccessToken.padStart(80)}]: Group is not accessible.")

            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ResponseMessage(403, "Group is not accessible!"))
        }


        logger.info("[${groupAccessToken.padStart(80)}]: Group token is valid.")

        model.addAttribute("groupAccessToken", groupAccessToken)
        return model
    }

    @PostMapping("/create")
    fun createStudent(@RequestParam groupAccessToken: String, @RequestParam username: String, model: Model): Any {
        logger.info("[${groupAccessToken.padStart(80)}]: Client trying to create student.")

        val group = groupService.getGroupByAccessToken(groupAccessToken) ?: run {
            logger.info("[${groupAccessToken.padStart(80)}]: Invalid  group token.")

            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ResponseMessage(403, "Invalid group token!"))
        }

        if (!group.isAccessible) {
            logger.info("[${groupAccessToken.padStart(80)}]: Group is not accessible.")

            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ResponseMessage(403, "Group is not accessible!"))
        }

        logger.info("[${groupAccessToken.padStart(80)}]: Group token is valid.")

        val webUser = webUserService.saveWebUser(username)
        logger.info("[${groupAccessToken.padStart(80)}]: Web user created.")

        val student = studentService.save(webUser, group)
        logger.info("[${groupAccessToken.padStart(80)}]: Student created.")

        model.addAttribute("id", student.id)
        model.addAttribute("username", username)
        model.addAttribute("accessToken", webUser.accessToken)
        return model
    }

    @GetMapping
    fun getAccess(@RequestParam accessToken: String, model: Model): Any {
        logger.info("[${accessToken.padStart(80)}]: Client trying to access student page.")

        val status = validateStudent(accessToken)
        if (status == WebUser.Status.NOT_FOUND || status == WebUser.Status.ADMIN) {
            logger.info("[${accessToken.padStart(80)}]: Client is not a student.")
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ResponseMessage(403, "You are not a student!"))
        }

        logger.info("[${accessToken.padStart(80)}]: Client is a student.")
        val webUser = webUserService.getWebUserByAccessToken(accessToken)!!
        val student = studentService.getByWebUser(webUser)!!
        val group = student.group

        if (!group.isAccessible) {
            logger.info("[${group.accessToken.padStart(80)}]: Group is not accessible.")

            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ResponseMessage(403, "Group is not accessible!"))
        }

        model.addAttribute("tasks", student.group.tasks.sortedBy { it.id })
        model.addAttribute("solutions", student.solutions.sortedBy { it.date })
        model.addAttribute("id", student.id)
        model.addAttribute("username", webUser.username)
        model.addAttribute("accessToken", accessToken)
        model.addAttribute("groupName", student.group.name)
        return model
    }

    @PostMapping("/solution/upload")
    fun uploadSolution(
        @RequestParam accessToken: String,
        @RequestParam taskId: Long,
        @RequestBody file: MultipartFile,
        model: Model
    ): Any {
        restTemplate.errorHandler = GradingSystemErrorHandler()
        logger.info("[${accessToken.padStart(80)}]: Client trying to upload solution.")

        val status = validateStudent(accessToken)
        if (status == WebUser.Status.NOT_FOUND || status == WebUser.Status.ADMIN) {
            logger.info("[${accessToken.padStart(80)}]: Client is not a student.")
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ResponseMessage(403, "You are not a student!"))
        }

        logger.info("[${accessToken.padStart(80)}]: Client is a student.")
        model.addAttribute("accessToken", accessToken)

        val webUser = webUserService.getWebUserByAccessToken(accessToken)!!
        val student = studentService.getByWebUser(webUser)!!
        val group = student.group

        if (!group.isAccessible) {
            logger.info("[${group.accessToken.padStart(80)}]: Group is not accessible.")

            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ResponseMessage(403, "Group is not accessible!"))
        }

        val task = taskService.getTaskById(taskId) ?: run {
            logger.info("[${accessToken.padStart(80)}]: Invalid task id.")
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ResponseMessage(403, "Invalid task id!"))
        }
        val deadline = task.deadline
        if (deadline != null && deadline < LocalDateTime.now()) {
            logger.info("[${accessToken.padStart(80)}]: Deadline is over.")
            //TODO: add normal page
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ResponseMessage(403, "Deadline is over!"))
        }

        val taskName = "${task.id}: ${task.name}"

        val headers = HttpHeaders()
        headers.setBasicAuth("user1", "super")
        headers.contentType = MediaType.MULTIPART_FORM_DATA

        val body: MultiValueMap<String, Any> = LinkedMultiValueMap()
        body.add("file", file.resource)
        body.add("task_name", taskName)
        body.add("student_id", student.id!!)

        val url = "$gradingSystemUrl/submissions/submission/upload"

        val submissionInfo = restTemplate.postForEntity(
            url,
            HttpEntity(body, headers),
            Map::class.java
        )

        if (submissionInfo.statusCodeValue == 422) {
            logger.info("[${accessToken.padStart(80)}]: Invalid file.")
            model.addAttribute("isCreated", false)
            model.addAttribute(
                "message",
                "Вы отправили некорректный формат файла. Попробуйте отправить файл с расширением .qrs."
            )
            return model
        }

        if (submissionInfo.statusCode == HttpStatus.INTERNAL_SERVER_ERROR) {
            logger.info("[${accessToken.padStart(80)}]: Something went wrong on grading system server.")
            model.addAttribute("isCreated", false)
            model.addAttribute(
                "message",
                "Что-то пошло не так на сервере системы проверки. Обратитесь к администратору и попробуйте позже."
            )
            return model
        }
        model.addAttribute("isCreated", true)

        val solution =
            solutionService.saveSolution(student.id, taskId, submissionInfo.body!!["id"].toString().toLong())!!
        logger.info("[${accessToken.padStart(80)}]: Solution uploaded.")

        model.addAttribute("id", solution.id)
        model.addAttribute("taskName", solution.task.name)
        return model
    }

    private fun validateStudent(accessToken: String): Enum<*> {
        val webUser = webUserService.getWebUserByAccessToken(accessToken) ?: run {
            return WebUser.Status.NOT_FOUND
        }

        val student = studentService.getByWebUser(webUser) ?: run {
            return WebUser.Status.ADMIN
        }

        return WebUser.Status.WEB_USER
    }
}