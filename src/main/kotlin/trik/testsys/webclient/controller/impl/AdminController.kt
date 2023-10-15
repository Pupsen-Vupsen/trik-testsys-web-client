package trik.testsys.webclient.controller.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import trik.testsys.webclient.controller.TrikUserController
import trik.testsys.webclient.entity.impl.*
import trik.testsys.webclient.model.impl.AdminModel
import trik.testsys.webclient.models.ResponseMessage
import trik.testsys.webclient.service.impl.*
import trik.testsys.webclient.util.TrikRedirectView
import trik.testsys.webclient.util.fp.Either
import trik.testsys.webclient.util.logger.TrikLogger
import java.time.LocalDateTime

@RestController
@RequestMapping("/admin")
@Suppress("UnnecessaryVariable", "DuplicatedCode")
class AdminController @Autowired constructor(
    @Value("\${app.grading-system.url}")
    private val gradingSystemUrl: String,

    private val adminService: AdminService,
    private val webUserService: WebUserService,
    private val groupService: GroupService,
    private val taskService: TaskService,
    private val studentService: StudentService,
    private val labelService: LabelService,
    private val viewerService: ViewerService,
) : TrikUserController {

    /**
     * @author Roman Shishkin
     * @since 1.1.0
     */
    @GetMapping
    override fun getAccess(
        @RequestParam accessToken: String,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Client trying to access admin page.")

        val isAdmin = isAdminAccessToken(accessToken)
        if (!isAdmin) {
            logger.info(accessToken, "Client is not an admin.")

            modelAndView.viewName = "error"
            modelAndView.addObject("message", "You are not an admin!")

            return modelAndView
        }

        val admin = adminService.getByAccessToken(accessToken)!!

        modelAndView.viewName = ADMIN_VIEW_NAME
        val adminModel = getModel(admin)

        modelAndView.addAllObjects(adminModel.asMap())

        return modelAndView
    }

    /**
     * @author Roman Shishkin
     * @since 1.1.0
     */
    @PostMapping("/group/create")
    fun createGroup(
        @RequestParam accessToken: String,
        @RequestParam name: String,
        @RequestParam additionalInfo: String?,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Client trying to create a group.")

        val isAdmin = isAdminAccessToken(accessToken)
        if (!isAdmin) {
            logger.info(accessToken, "Client is not an admin.")
            modelAndView.viewName = "error"
            modelAndView.addObject("message", "You are not an admin!")

            return modelAndView
        }

        logger.info(accessToken, "Client is an admin.")
        val webUser = webUserService.getWebUserByAccessToken(accessToken)!!
        val admin = adminService.getAdminByWebUser(webUser)!!

        val group = groupService.createGroup(admin, name, additionalInfo)
        logger.info(accessToken, "Group created: $group")

        val adminModel = getModel(admin)

        modelAndView.addAllObjects(adminModel.asMap())
        modelAndView.view = REDIRECT_VIEW

        return modelAndView
    }

    /**
     * @author Roman Shishkin
     * @since 1.1.0
     */
    @PostMapping("/group/edit")
    fun editGroup(
        @RequestParam accessToken: String,
        @RequestParam groupAccessToken: String,
        @RequestParam name: String,
        @RequestParam additionalInfo: String?,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Client trying to edit a group.")

        val isAdmin = isAdminAccessToken(accessToken)
        if (!isAdmin) {
            logger.info(accessToken, "Client is not an admin.")
            modelAndView.viewName = "error"
            modelAndView.addObject("message", "You are not an admin!")

            return modelAndView
        }

        logger.info(accessToken, "Client is an admin.")
        val webUser = webUserService.getWebUserByAccessToken(accessToken)!!
        val admin = adminService.getAdminByWebUser(webUser)!!

        val group = groupService.getGroupByAccessToken(groupAccessToken)!!
        group.name = name
        group.additionalInfo = additionalInfo
        groupService.save(group)
        logger.info(accessToken, "Group edited: $group")

        val adminModel = getModel(admin)

        modelAndView.addAllObjects(adminModel.asMap())
        modelAndView.view = REDIRECT_VIEW

        return modelAndView
    }

    /**
     * @author Roman Shishkin
     * @since 1.1.0
     */
    @PostMapping("/group/delete")
    fun deleteGroup(
        @RequestParam accessToken: String,
        @RequestParam groupAccessToken: String,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Client trying to delete a group.")

        val isAdmin = isAdminAccessToken(accessToken)
        if (!isAdmin) {
            logger.info(accessToken, "Client is not an admin.")
            modelAndView.viewName = "error"
            modelAndView.addObject("message", "You are not an admin!")

            return modelAndView
        }

        logger.info(accessToken, "Client is an admin.")
        val webUser = webUserService.getWebUserByAccessToken(accessToken)!!
        val admin = adminService.getAdminByWebUser(webUser)!!

        val group = groupService.getGroupByAccessToken(groupAccessToken)!!
        groupService.delete(group)
        logger.info(accessToken, "Group deleted: $group")

        val adminModel = getModel(admin)

        modelAndView.addAllObjects(adminModel.asMap())
        modelAndView.view = REDIRECT_VIEW

        return modelAndView
    }

    /**
     * @author Roman Shishkin
     * @since 1.1.0
     */
    @PostMapping("/group/labels/add")
    fun addLabelsToGroup(
        @RequestParam accessToken: String,
        @RequestParam groupAccessToken: String,
        @RequestParam newLabel: String?,
        @RequestParam labels: List<String>?,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Client trying to add labels to group.")

        val isAdmin = isAdminAccessToken(accessToken)
        if (!isAdmin) {
            logger.info(accessToken, "Client is not an admin.")
            modelAndView.viewName = "error"
            modelAndView.addObject("message", "You are not an admin!")

            return modelAndView
        }

        logger.info(accessToken, "Client is an admin.")

        val labelsToAdd = mutableListOf<String>()
        if (!newLabel.isNullOrBlank()) {
            labelsToAdd.add(newLabel)
        }

        if (labels != null) {
            labelsToAdd.addAll(labels)
        }

        val webUser = webUserService.getWebUserByAccessToken(accessToken)!!
        val admin = adminService.getAdminByWebUser(webUser)!!

        val group = groupService.getGroupByAccessToken(groupAccessToken)!!
        val labelsToCreate = labelsToAdd.filter { labelService.getByName(it) == null }.map { Label(it) }
        labelService.saveAll(labelsToCreate)

        val allLabels = labelService.getAll().filter { it.name in labelsToAdd }

        group.labels.addAll(allLabels)
        groupService.save(group)
        logger.info(accessToken, "Labels added to group: $group")

        val adminModel = getModel(admin)

        modelAndView.addAllObjects(adminModel.asMap())
        modelAndView.view = REDIRECT_VIEW

        return modelAndView
    }

    /**
     * @author Roman Shishkin
     * @since 1.1.0
     */
    @PostMapping("/group/labels/delete")
    fun deleteLabelsFromGroup(
        @RequestParam accessToken: String,
        @RequestParam groupAccessToken: String,
        @RequestParam labels: List<String>,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Client trying to delete labels from group.")

        val isAdmin = isAdminAccessToken(accessToken)
        if (!isAdmin) {
            logger.info(accessToken, "Client is not an admin.")
            modelAndView.viewName = "error"
            modelAndView.addObject("message", "You are not an admin!")

            return modelAndView
        }

        logger.info(accessToken, "Client is an admin.")

        val webUser = webUserService.getWebUserByAccessToken(accessToken)!!
        val admin = adminService.getAdminByWebUser(webUser)!!

        val group = groupService.getGroupByAccessToken(groupAccessToken)!!
        val labelsToDelete = labelService.getAll().filter { it.name in labels }
        group.labels.removeAll(labelsToDelete.toSet())
        groupService.save(group)
        logger.info(accessToken, "Labels deleted from group: $group")

        val adminModel = getModel(admin)

        modelAndView.addAllObjects(adminModel.asMap())
        modelAndView.view = REDIRECT_VIEW

        return modelAndView
    }

    /**
     * @author Roman Shishkin
     * @since 1.1.0
     */
    @GetMapping("/registration")
    fun registration(
        @RequestParam accessToken: String,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Admin trying to register.")

        val viewer = viewerService.getByAdminRegToken(accessToken) ?: run {
            logger.info(accessToken, "No viewer with such regToken.")
            modelAndView.viewName = "error"
            modelAndView.addObject("message", "No viewer with such regToken!")

            return modelAndView
        }

        val adminModel = AdminModel.Builder()
            .accessToken(accessToken)
            .username("")
            .labels(labelService.getAll())
            .groups(emptySet())
            .tasks(emptySet())
            .webUserId(viewer.id!!)
            .registrationDate(LocalDateTime.now())
            .build()

        modelAndView.addAllObjects(adminModel.asMap())
        modelAndView.viewName = "registration"

        return modelAndView
    }

    @PostMapping("/create")
    fun create(
        @RequestParam accessToken: String,
        @RequestParam username: String,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Admin trying to create.")

        val viewer = viewerService.getByAdminRegToken(accessToken) ?: run {
            logger.info(accessToken, "No viewer with such regToken.")
            modelAndView.viewName = "error"
            modelAndView.addObject("message", "No viewer with such regToken!")

            return modelAndView
        }

        val webUser = webUserService.saveWebUser(username)
        webUser.lastLoginDate = LocalDateTime.now()
        webUserService.saveWebUser(webUser)

        val admin = adminService.save(webUser, viewer)
        val publicTasks = taskService.getAllPublic()
        publicTasks.forEach { it.admins.add(admin) }
        admin.tasks.addAll(publicTasks)
        adminService.save(admin)
        publicTasks.forEach { taskService.save(it) }

        val adminModel = getModel(admin)
        modelAndView.addAllObjects(adminModel.asMap())
        modelAndView.viewName = "create"

        return modelAndView
    }

    /**
     * @author Roman Shishkin
     * @since 1.1.0
     */
    @PostMapping("/task/groups/add")
    fun addGroupsToTask(
        @RequestParam accessToken: String,
        @RequestParam taskId: Long,
        @RequestParam groupIds: List<Long>,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Client trying to add groups to task.")

        val isAdmin = isAdminAccessToken(accessToken)
        if (!isAdmin) {
            logger.info(accessToken, "Client is not an admin.")
            modelAndView.viewName = "error"
            modelAndView.addObject("message", "You are not an admin!")

            return modelAndView
        }

        logger.info(accessToken, "Client is an admin.")

        val webUser = webUserService.getWebUserByAccessToken(accessToken)!!
        val admin = adminService.getAdminByWebUser(webUser)!!

        val task = taskService.getTaskById(taskId)!!
        val groups = groupService.getAllByIds(groupIds)
        task.groups.addAll(groups)
        groups.forEach { it.tasks.add(task) }
        groupService.saveAll(groups)
        taskService.save(task)

        logger.info(accessToken, "Groups added to task: $task")

        val adminModel = getModel(admin)

        modelAndView.addAllObjects(adminModel.asMap())
        modelAndView.view = REDIRECT_VIEW

        return modelAndView
    }

    /**
     * @author Roman Shishkin
     * @since 1.1.0
     */
    @PostMapping("/task/groups/remove")
    fun removeGroupsFromTask(
        @RequestParam accessToken: String,
        @RequestParam taskId: Long,
        @RequestParam groupIds: List<Long>,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Client trying to remove groups from task.")

        val isAdmin = isAdminAccessToken(accessToken)
        if (!isAdmin) {
            logger.info(accessToken, "Client is not an admin.")
            modelAndView.viewName = "error"
            modelAndView.addObject("message", "You are not an admin!")

            return modelAndView
        }

        logger.info(accessToken, "Client is an admin.")

        val webUser = webUserService.getWebUserByAccessToken(accessToken)!!
        val admin = adminService.getAdminByWebUser(webUser)!!

        val task = taskService.getTaskById(taskId)!!
        val groups = groupService.getAllByIds(groupIds)
        task.groups.removeAll(groups.toSet())
        groups.forEach { it.tasks.remove(task) }
        groupService.saveAll(groups)
        taskService.save(task)

        logger.info(accessToken, "Groups removed from task: $task")

        val adminModel = getModel(admin)

        modelAndView.addAllObjects(adminModel.asMap())
        modelAndView.view = REDIRECT_VIEW

        return modelAndView
    }

    @PostMapping("/info/change")
    fun changeInfo(
        @RequestParam accessToken: String,
        @RequestParam newUsername: String?,
        @RequestParam newAdditionalInfo: String?,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Admin trying to change info.")

        val isAdmin = isAdminAccessToken(accessToken)

        if (!isAdmin) {
            logger.info(accessToken, "Client is not an admin.")
            modelAndView.viewName = "error"
            modelAndView.addObject("message", "You are not an admin!")

            return modelAndView
        }

        logger.info(accessToken, "Client is an admin.")

        val webUser = webUserService.getWebUserByAccessToken(accessToken)!!
        val admin = adminService.getAdminByWebUser(webUser)!!

        if (newUsername != null) {
            webUser.username = newUsername
        }

        if (newAdditionalInfo != null) {
            webUser.additionalInfo = newAdditionalInfo
        }

        webUserService.saveWebUser(webUser)

        logger.info(accessToken, "Info changed: $webUser")

        val adminModel = getModel(admin)

        modelAndView.addAllObjects(adminModel.asMap())
        modelAndView.view = REDIRECT_VIEW

        return modelAndView
    }

    @Deprecated("")
    @GetMapping("/group")
    fun accessToGroup(
        @RequestParam accessToken: String,
        @RequestParam groupAccessToken: String,
        model: Model
    ): Any {
        logger.info("[${accessToken.padStart(80)}]: Client trying to access group.")

        val eitherEntities = getAdminEntities(accessToken, groupAccessToken)
        if (eitherEntities.isLeft()) {
            return eitherEntities.getLeft()
        }
        val (webUser, _, group) = eitherEntities.getRight()

        model.addAttribute("accessToken", webUser.accessToken)
        model.addAttribute("isFound", true)
        model.addAttribute("id", group.id!!)
        model.addAttribute("name", group.name)
        model.addAttribute("groupAccessToken", group.accessToken)
        model.addAttribute("tasks", group.tasks.sortedBy { it.id })
        model.addAttribute("students", group.students.sortedBy { it.id })

        return model
    }

    @Deprecated("")
    @GetMapping("/task")
    fun getTaskSolutions(
        @RequestParam accessToken: String,
        @RequestParam groupAccessToken: String,
        @RequestParam taskId: Long,
        model: Model
    ): Any {
        logger.info("[${accessToken.padStart(80)}]: Client trying to get task solutions.")

        val eitherEntities = getAdminEntities(accessToken, groupAccessToken)
        if (eitherEntities.isLeft()) {
            return eitherEntities.getLeft()
        }
        val (webUser, _, group) = eitherEntities.getRight()

        model.addAttribute("accessToken", webUser.accessToken)
        model.addAttribute("groupAccessToken", group.accessToken)

        val task = taskService.getTaskById(taskId)
        if (task == null) {
            logger.info("[${accessToken.padStart(80)}]: Task not found.")

            model.addAttribute("isFound", false)
            model.addAttribute("message", "Задание не найдено.")

            return model
        }

        if (!task.groups.contains(group)) {
            logger.info("[${accessToken.padStart(80)}]: Task not found.")

            model.addAttribute("isFound", false)
            model.addAttribute("message", "Задание не найдено.")

            return model
        }

        logger.info("[${accessToken.padStart(80)}]: Task found.")
        model.addAttribute("taskId", task.id)
        model.addAttribute("taskName", task.name)

        model.addAttribute("isFound", true)
        model.addAttribute("name", task.name)
        model.addAttribute("groupName", group.name)
        model.addAttribute("solutions", task.solutions.sortedBy { it.id })

        return model
    }

    @Deprecated("")
    @GetMapping("/student")
    fun getStudentSubmissions(
        @RequestParam accessToken: String,
        @RequestParam groupAccessToken: String,
        @RequestParam studentId: Long,
        model: Model
    ): Any {
        logger.info("[${accessToken.padStart(80)}]: Client trying to get student submissions.")

        val eitherEntities = getAdminEntities(accessToken, groupAccessToken)
        if (eitherEntities.isLeft()) {
            return eitherEntities.getLeft()
        }
        val (webUser, _, group) = eitherEntities.getRight()

        model.addAttribute("accessToken", webUser.accessToken)
        model.addAttribute("groupAccessToken", group.accessToken)

        val student = studentService.getById(studentId)
        if (student == null) {
            logger.info("[${accessToken.padStart(80)}]: Student not found.")

            model.addAttribute("isFound", false)
            model.addAttribute("message", "Студент не найден.")

            return model
        }

        if (student.group != group) {
            logger.info("[${accessToken.padStart(80)}]: Student not found.")

            model.addAttribute("isFound", false)
            model.addAttribute("message", "Студент не найден.")

            return model
        }

        logger.info("[${accessToken.padStart(80)}]: Student found.")
        model.addAttribute("studentId", student.id)
        model.addAttribute("studentName", student.webUser.username)

        model.addAttribute("isFound", true)
        model.addAttribute("groupName", group.name)
        model.addAttribute("solutions", student.solutions.sortedBy { it.id })

        return model
    }

    @Deprecated("")
    @GetMapping("/group/table")
    fun getGroupTable(
        @RequestParam accessToken: String,
        @RequestParam groupAccessToken: String,
        model: Model
    ): Any {
        logger.info("[${accessToken.padStart(80)}]: Client trying to get group table.")

        val eitherEntities = getAdminEntities(accessToken, groupAccessToken)
        if (eitherEntities.isLeft()) {
            return eitherEntities.getLeft()
        }
        val (webUser, _, group) = eitherEntities.getRight()

        model.addAttribute("accessToken", webUser.accessToken)
        model.addAttribute("groupAccessToken", group.accessToken)

        model.addAttribute("isFound", true)
        model.addAttribute("tasks", group.tasks.sortedBy { it.id })
        model.addAttribute("groupName", group.name)
        model.addAttribute("students", group.students.sortedBy { it.id })

        if (group.tasks.isEmpty()) {
            model.addAttribute("table", emptyList<Int>())
            return model
        }

        val table = MutableList<MutableList<Int>>(
            (group.students.sortedByDescending { it.id }.firstOrNull()?.id?.toInt()?.plus(1)) ?: 0
        ) { mutableListOf() }

        group.students.sortedByDescending { it.id }.forEach { student ->
            val taskList = MutableList(
                group.tasks.sortedByDescending { it.id }.firstOrNull()?.id?.toInt()?.plus(1) ?: 0
            ) { -1 }

            group.tasks.sortedByDescending { it.id }.forEach { task ->
                logger.warn("Task: $task")

                val failedSolution =
                    student.solutions.find { it.task == task && (it.status == Solution.Status.FAILED || it.status == Solution.Status.ERROR) }
                val passedSolution =
                    student.solutions.find { it.task == task && it.status == Solution.Status.PASSED }
                val inProgressSolution =
                    student.solutions.find { it.task == task && (it.status == Solution.Status.IN_PROGRESS || it.status == Solution.Status.NOT_STARTED) }

                if (failedSolution != null) {
                    taskList[task.id!!.toInt()] = 0
                } else if (passedSolution != null) {
                    taskList[task.id!!.toInt()] = 1
                } else if (inProgressSolution != null) {
                    taskList[task.id!!.toInt()] = 2
                } else {
                    taskList[task.id!!.toInt()] = -1
                }

                table[student.id!!.toInt()] = taskList
            }
        }
        model.addAttribute("table", table.toList())

        return model
    }

    @Deprecated("")
    @GetMapping("/student/create")
    @ResponseBody
    fun createStudents(
        @RequestParam accessToken: String,
        @RequestParam groupAccessToken: String,
        @RequestParam count: Long,
        @RequestParam studentAccessTokenPrefix: String,
        @RequestParam namePrefix: String,
        model: Model
    ): ResponseEntity<out Any> {
        logger.info(accessToken, "Client trying to create many students.")

        val eitherEntities = getAdminEntities(accessToken, groupAccessToken)
        if (eitherEntities.isLeft()) {
            return eitherEntities.getLeft()
        }
        val (_, _, group) = eitherEntities.getRight()

        val students = studentService.generateStudents(count, studentAccessTokenPrefix, namePrefix, group)
        logger.info(accessToken, "$count students created.")

        val csv = studentService.convertToCsv(students)
        logger.info(accessToken, "CSV with created students created.")
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_OCTET_STREAM
        headers.contentDisposition = ContentDisposition.builder("attachment")
            .filename("${group.name}-студенты.csv")
            .build()

        headers.acceptCharset = listOf(Charsets.UTF_8)
        val bytes = csv.toString().toByteArray()

        return ResponseEntity
            .ok()
            .headers(headers)
            .contentLength(bytes.size.toLong())
            .body(bytes)
    }

    @Deprecated("")
    @PostMapping("/group/change/access")
    fun changeAccess(
        @RequestParam accessToken: String,
        @RequestParam groupAccessToken: String,
        @RequestParam isAccessible: Boolean,
        @RequestParam isRegistrationOpen: Boolean,
        model: Model
    ): ResponseEntity<out Any> {
        logger.info(accessToken, "Client trying to change group access.")

        val eitherEntities = getAdminEntities(accessToken, groupAccessToken)
        if (eitherEntities.isLeft()) {
            return eitherEntities.getLeft()
        }
        val (_, _, group) = eitherEntities.getRight()

        group.isAccessible = isAccessible
        group.isRegistrationOpen = isRegistrationOpen
        groupService.save(group)

        logger.info(accessToken, "Group access changed.")

        return ResponseEntity.ok().build()
    }

    /**
     * @author Roman Shishkin
     * @since 1.1.0
     */
    private fun getModel(admin: Admin): AdminModel {
        val webUser = admin.webUser
        val adminModel = AdminModel.Builder()
            .accessToken(webUser.accessToken)
            .username(webUser.username)
            .groups(admin.groups)
            .tasks(admin.tasks)
            .labels(labelService.getAll())
            .webUserId(webUser.id)
            .additionalInfo(webUser.additionalInfo)
            .registrationDate(webUser.registrationDate)
            .lastLoginDate(webUser.lastLoginDate)
            .build()

        return adminModel
    }

    /**
     * @author Roman Shishkin
     * @since 1.1.0
     */
    private fun isAdminAccessToken(accessToken: String): Boolean {
        val webUser = webUserService.getWebUserByAccessToken(accessToken) ?: run {
            logger.info(accessToken, "Client not found.")
            return false
        }
        adminService.getAdminByWebUser(webUser) ?: run {
            logger.info(accessToken, "Client is not an admin.")
            return false
        }

        logger.info(accessToken, "Client is an admin.")
        return true
    }

    /**
     * Validates if [accessToken] belongs to an [Admin].
     * @return null if client is an admin, else [ResponseEntity] with status 403 and message "You are not an admin!".
     * @author Roman Shishkin
     * @since 1.1.0
     */
    private fun validateAdminByAccessToken(accessToken: String): ResponseEntity<ResponseMessage>? {
        if (isAdminAccessToken(accessToken)) return null

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ResponseMessage(403, "You are not an admin!"))
    }

    /**
     * Validates if [accessToken] belongs to a [Group].
     * @return [Either] with [ResponseEntity] with status 404 and message "Group not found!" if group not found,
     * else [Either] with null and [Group] if group found.
     * @see Either
     * @author Roman Shishkin
     * @since 1.1.0
     */
    private fun validateGroupAccessToken(
        accessToken: String,
        admin: Admin
    ): Either<ResponseEntity<ResponseMessage>, Group> {
        val group = groupService.getGroupByAccessToken(accessToken) ?: run {
            logger.info(accessToken, "Group not found.")

            val responseEntity = ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ResponseMessage(404, "Group not found!"))

            return Either.left(responseEntity)
        }

        if (group.admin != admin) {
            logger.info(accessToken, "Group not found.")

            val responseEntity = ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ResponseMessage(404, "Group not found!"))

            return Either.left(responseEntity)
        }

        logger.info(accessToken, "Group found.")
        return Either.right(group)
    }

    /**
     * Validates everything belongs to an [Admin] by [accessToken] and [groupAccessToken].
     * @return [Either] with [ResponseEntity], else [Either] with null and [Entities].
     * @see Either
     * @see Entities
     * @author Roman Shishkin
     */
    private fun getAdminEntities(
        accessToken: String,
        groupAccessToken: String
    ): Either<ResponseEntity<ResponseMessage>, Entities> {
        logger.info(accessToken, "Validating admin access token.")
        validateAdminByAccessToken(accessToken)?.let { responseEntity ->
            return Either.left(responseEntity)
        }

        val webUser = webUserService.getWebUserByAccessToken(accessToken)
            ?: logger.errorAndThrow(accessToken, "Web user not found.")

        val admin = adminService.getAdminByWebUser(webUser)
            ?: logger.errorAndThrow(accessToken, "Admin not found.")

        val eitherGroup = validateGroupAccessToken(groupAccessToken, admin)
        return eitherGroup.bind { group ->
            val entities = Entities(webUser, admin, group)
            Either.right(entities)
        }
    }

    private data class Entities(
        val webUser: WebUser,
        val admin: Admin,
        val group: Group
    )

    companion object {
        private val logger = TrikLogger(this::class.java)

        private const val ADMIN_VIEW_NAME = "admin"
        private val REDIRECT_VIEW = TrikRedirectView("/admin")
    }
}