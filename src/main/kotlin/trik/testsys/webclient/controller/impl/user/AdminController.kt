package trik.testsys.webclient.controller.impl.user

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import trik.testsys.webclient.controller.impl.main.LoginController
import trik.testsys.webclient.controller.user.WebUserController
import trik.testsys.webclient.entity.user.impl.Admin
import trik.testsys.webclient.service.entity.impl.GroupService
import trik.testsys.webclient.service.entity.user.impl.AdminService
import trik.testsys.webclient.service.security.login.impl.LoginData
import trik.testsys.webclient.service.token.reg.RegTokenGenerator
import trik.testsys.webclient.util.addPopupMessage
import trik.testsys.webclient.util.atTimeZone
import trik.testsys.webclient.view.impl.AdminView
import trik.testsys.webclient.view.impl.GroupCreationView
import trik.testsys.webclient.view.impl.GroupView
import trik.testsys.webclient.view.impl.GroupView.Companion.toView
import java.util.*
import javax.servlet.http.HttpServletRequest

@Controller
@RequestMapping(AdminController.ADMIN_PATH)
class AdminController(
    loginData: LoginData,

    private val groupService: GroupService,
    @Qualifier("groupRegTokenGenerator") private val groupRegTokenGenerator: RegTokenGenerator
) : WebUserController<Admin, AdminView, AdminService>(loginData) {

    override val MAIN_PATH = ADMIN_PATH

    override val MAIN_PAGE = ADMIN_PAGE

    override fun Admin.toView(timeZone: TimeZone) = AdminView(
        id = this.id,
        name = this.name,
        accessToken = this.accessToken,
        creationDate = this.creationDate?.atTimeZone(timeZone),
        lastLoginDate = this.lastLoginDate?.atTimeZone(timeZone),
        viewer = this.viewer,
        additionalInfo = this.additionalInfo,
        groups = this.groups.map { it.toView(timeZone) }.sortedBy { it.id }
    )

    @GetMapping(GROUPS_PATH)
    fun groupsGet(
        timeZone: TimeZone,
        redirectAttributes: RedirectAttributes,
        model: Model
    ): String {
        val webUser = validate(redirectAttributes) ?: return "redirect:${LoginController.LOGIN_PATH}"

        model.addAttribute(GROUP_ATTR, GroupCreationView.empty())
        model.addAttribute(WEB_USER_ATTR, webUser.toView(timeZone))

        return GROUPS_PAGE
    }

    @PostMapping("$GROUP_PATH/create")
    fun groupPost(
        @ModelAttribute("group") groupView: GroupCreationView,
        timeZone: TimeZone,
        request: HttpServletRequest,
        redirectAttributes: RedirectAttributes
    ): String {
        val webUser = validate(redirectAttributes) ?: return "redirect:${LoginController.LOGIN_PATH}"

        val regToken = groupRegTokenGenerator.generate(groupView.name)
        val group = groupView.toEntity(regToken, webUser)

        if (!groupService.validateName(group)) {
            redirectAttributes.addPopupMessage("Название Группы не должно содержать Код-доступа.")
            return "redirect:$ADMIN_PATH$GROUPS_PATH"
        }

        if (!groupService.validateAdditionalInfo(group)) {
            redirectAttributes.addPopupMessage("Дополнительная информация не должна содержать Код-доступа.")
            return "redirect:$ADMIN_PATH$GROUPS_PATH"
        }

        groupService.save(group)

        redirectAttributes.addPopupMessage("Группа ${group.name} успешно создана.")

        return "redirect:$ADMIN_PATH$GROUPS_PATH"
    }

    @GetMapping("$GROUP_PATH/{groupId}")
    fun groupGet(
        @PathVariable("groupId") id: Long,
        timeZone: TimeZone,
        redirectAttributes: RedirectAttributes,
        model: Model
    ): String {
        val webUser = validate(redirectAttributes) ?: return "redirect:${LoginController.LOGIN_PATH}"

        if (!webUser.validateGroupExistence(id)) {
            redirectAttributes.addPopupMessage("Группа с ID $id не найдена.")
            return "redirect:$ADMIN_PATH$GROUPS_PATH"
        }

        val group = groupService.find(id) ?: run {
            redirectAttributes.addPopupMessage("Группа с ID $id не найдена.")
            return "redirect:$ADMIN_PATH$GROUPS_PATH"
        }

        val groupView = group.toView(timeZone)
        model.addAttribute(GROUP_ATTR, groupView)

        return GROUP_PAGE
    }

    @PostMapping("$GROUP_PATH/update/{groupId}")
    fun groupUpdate(
        @PathVariable("groupId") groupId: Long,
        @ModelAttribute("group") groupView: GroupView,
        timeZone: TimeZone,
        redirectAttributes: RedirectAttributes,
        model: Model
    ): String {
        val webUser = validate(redirectAttributes) ?: return "redirect:${LoginController.LOGIN_PATH}"

        if (!webUser.validateGroupExistence(groupId)) {
            redirectAttributes.addPopupMessage("Группа с ID $groupId не найдена.")
            return "redirect:$ADMIN_PATH$GROUPS_PATH"
        }

        val group = groupView.toEntity(timeZone)
        group.admin = webUser

        if (!groupService.validateName(group)) {
            redirectAttributes.addPopupMessage("Название группы не должно содержать Код-доступа.")
            return "redirect:$ADMIN_PATH$GROUP_PATH/$groupId"
        }

        if (!groupService.validateAdditionalInfo(group)) {
            redirectAttributes.addPopupMessage("Дополнительная информация не должна содержать Код-доступа.")
            return "redirect:$ADMIN_PATH$GROUP_PATH/$groupId"
        }

        val updatedGroup = groupService.save(group)

        model.addAttribute(GROUP_ATTR, updatedGroup.toView(timeZone))
        redirectAttributes.addPopupMessage("Данные успешно изменены.")

        return "redirect:$ADMIN_PATH$GROUP_PATH/$groupId"
    }

    companion object {

        const val ADMIN_PATH = "/admin"
        const val ADMIN_PAGE = "admin"

        const val GROUPS_PATH = "/groups"
        const val GROUPS_PAGE = "admin/groups"

        const val GROUP_ATTR = "group"

        const val GROUP_PATH = "$GROUPS_PATH/group"
        const val GROUP_PAGE = "admin/group"

        fun Admin.validateGroupExistence(groupId: Long?) = groups.any{ it.id == groupId }
    }
}

//    /**
//     * @author Roman Shishkin
//     * @since 1.1.0
//     */
//    @PostMapping("/group/edit")
//    fun editGroup(
//        @RequestParam accessToken: String,
//        @RequestParam groupAccessToken: String,
//        @RequestParam name: String,
//        @RequestParam additionalInfo: String?,
//        modelAndView: ModelAndView
//    ): ModelAndView {
//        logger.info(accessToken, "Client trying to edit a group.")
//
//        val isAdmin = isAdminAccessToken(accessToken)
//        if (!isAdmin) {
//            logger.info(accessToken, "Client is not an admin.")
//            modelAndView.viewName = "error"
//            modelAndView.addObject("message", "You are not an admin!")
//
//            return modelAndView
//        }
//
//        logger.info(accessToken, "Client is an admin.")
//        val webUser = webUserService.getWebUserByAccessToken(accessToken)!!
//        val admin = adminService.getAdminByWebUser(webUser)!!
//
//        val group = groupService.getGroupByAccessToken(groupAccessToken)!!
//        group.name = name
//        group.additionalInfo = additionalInfo
//        groupService.save(group)
//        logger.info(accessToken, "Group edited: $group")
//
//        val adminModel = getModel(admin)
//
//        modelAndView.addAllObjects(adminModel.asMap())
//        modelAndView.view = REDIRECT_VIEW
//
//        return modelAndView
//    }
//
//    /**
//     * @author Roman Shishkin
//     * @since 1.1.0
//     */
//    @PostMapping("/group/delete")
//    fun deleteGroup(
//        @RequestParam accessToken: String,
//        @RequestParam groupAccessToken: String,
//        modelAndView: ModelAndView
//    ): ModelAndView {
//        logger.info(accessToken, "Client trying to delete a group.")
//
//        val isAdmin = isAdminAccessToken(accessToken)
//        if (!isAdmin) {
//            logger.info(accessToken, "Client is not an admin.")
//            modelAndView.viewName = "error"
//            modelAndView.addObject("message", "You are not an admin!")
//
//            return modelAndView
//        }
//
//        logger.info(accessToken, "Client is an admin.")
//        val webUser = webUserService.getWebUserByAccessToken(accessToken)!!
//        val admin = adminService.getAdminByWebUser(webUser)!!
//
//        val group = groupService.getGroupByAccessToken(groupAccessToken)!!
//        groupService.delete(group)
//        logger.info(accessToken, "Group deleted: $group")
//
//        val adminModel = getModel(admin)
//
//        modelAndView.addAllObjects(adminModel.asMap())
//        modelAndView.view = REDIRECT_VIEW
//
//        return modelAndView
//    }
//
//    /**
//     * @author Roman Shishkin
//     * @since 1.1.0
//     */
//    @PostMapping("/task/groups/add")
//    fun addGroupsToTask(
//        @RequestParam accessToken: String,
//        @RequestParam taskId: Long,
//        @RequestParam groupIds: List<Long>,
//        modelAndView: ModelAndView
//    ): ModelAndView {
//        logger.info(accessToken, "Client trying to add groups to task.")
//
//        val isAdmin = isAdminAccessToken(accessToken)
//        if (!isAdmin) {
//            logger.info(accessToken, "Client is not an admin.")
//            modelAndView.viewName = "error"
//            modelAndView.addObject("message", "You are not an admin!")
//
//            return modelAndView
//        }
//
//        logger.info(accessToken, "Client is an admin.")
//
//        val webUser = webUserService.getWebUserByAccessToken(accessToken)!!
//        val admin = adminService.getAdminByWebUser(webUser)!!
//
//        val task = taskService.getTaskById(taskId)!!
//        val groups = groupService.getAllByIds(groupIds)
//        task.groups.addAll(groups)
//        groups.forEach { it.tasks.add(task) }
//        groupService.saveAll(groups)
//        taskService.saveTask(task)
//
//        logger.info(accessToken, "Groups added to task: $task")
//
//        val adminModel = getModel(admin)
//
//        modelAndView.addAllObjects(adminModel.asMap())
//        modelAndView.view = REDIRECT_VIEW
//
//        return modelAndView
//    }
//
//    /**
//     * @author Roman Shishkin
//     * @since 1.1.0
//     */
//    @PostMapping("/task/groups/remove")
//    fun removeGroupsFromTask(
//        @RequestParam accessToken: String,
//        @RequestParam taskId: Long,
//        @RequestParam groupIds: List<Long>,
//        modelAndView: ModelAndView
//    ): ModelAndView {
//        logger.info(accessToken, "Client trying to remove groups from task.")
//
//        val isAdmin = isAdminAccessToken(accessToken)
//        if (!isAdmin) {
//            logger.info(accessToken, "Client is not an admin.")
//            modelAndView.viewName = "error"
//            modelAndView.addObject("message", "You are not an admin!")
//
//            return modelAndView
//        }
//
//        logger.info(accessToken, "Client is an admin.")
//
//        val webUser = webUserService.getWebUserByAccessToken(accessToken)!!
//        val admin = adminService.getAdminByWebUser(webUser)!!
//
//        val task = taskService.getTaskById(taskId)!!
//        val groups = groupService.getAllByIds(groupIds)
//        task.groups.removeAll(groups.toSet())
//        groups.forEach { it.tasks.remove(task) }
//        groupService.saveAll(groups)
//        taskService.saveTask(task)
//
//        logger.info(accessToken, "Groups removed from task: $task")
//
//        val adminModel = getModel(admin)
//
//        modelAndView.addAllObjects(adminModel.asMap())
//        modelAndView.view = REDIRECT_VIEW
//
//        return modelAndView
//    }
//
//    @RequestMapping("/results/download")
//    fun getAllResults(
//        @RequestParam accessToken: String,
//        modelAndView: ModelAndView
//    ): Any {
//        val isAdmin = isAdminAccessToken(accessToken)
//        if (!isAdmin) {
//            logger.info(accessToken, "Client is not an admin.")
//            modelAndView.viewName = "error"
//            modelAndView.addObject("message", "You are not an admin!")
//
//            return modelAndView
//        }
//
//        logger.info(accessToken, "Client is an admin.")
//
//        val webUser = webUserService.getWebUserByAccessToken(accessToken)!!
//        val admin = adminService.getAdminByWebUser(webUser)!!
//        val viewer = admin.viewer
//
//        val groups = admin.groups
//        logger.info(accessToken, "Groups (${groups.size}): $groups")
//
//        val students = groups.flatMap { it.students }.sortedBy { it.id }.filter { solutionService.countByStudent(it) > 0 }
//        logger.info(accessToken, "Students (${students.size}): $students")
//
//        val tasks = students.flatMap { it.solutions }.map { it.task }.distinct().sortedBy { it.id }
//
//        val csvDelimiter = ";"
//        val tasksString = tasks.joinToString(csvDelimiter) { String.format("\"%d: %s\"", it.id, it.name) } + ";"
//
//        val csvHeader =
//            "\"student_id\";\"student_name\";\"district_id\";\"district_name\";\"school_id\";\"school_name\";\"group_id\";\"group_name\";$tasksString\"best_score\"\n"
//
//        val studentsResults = mutableMapOf<Long, List<Long>>()
//        students.forEach { student ->
//            val studentScores = mutableListOf<Long>()
//            tasks.forEach { task ->
//                val bestSolution = solutionService.getBestSolutionByTaskAndStudent(task, student)
//                val score = bestSolution?.score ?: 0
//
//                studentScores.add(score)
//            }
//            val maxScore = studentScores.maxOrNull() ?: 0
//            studentScores.add(maxScore)
//            studentsResults[student.id!!] = studentScores
//        }
//
//        val csvBody = mutableListOf<String>()
//        students.forEach { student ->
//            val studentScores = studentsResults[student.id!!]!!
//            val studentScoresString = studentScores.joinToString(csvDelimiter)
//
//            val studentWebUser = student.webUser
//            val group = student.group
//
//            val studentInfo = "\"${student.id}\";\"${studentWebUser.username}\";\"${viewer.id}\";\"${viewer.webUser.username}\";\"${admin.id}\";\"${webUser.username}\";\"${group.id}\";\"${group.name}\";$studentScoresString"
//
//            csvBody.add(studentInfo)
//        }
//
//        val csv = csvHeader + csvBody.joinToString("\n")
//        val bytes = csv.toByteArray()
//
//        val headers = HttpHeaders()
//        headers.contentType = MediaType.APPLICATION_OCTET_STREAM
//        headers.contentDisposition = ContentDisposition.builder("attachment")
//            .filename("results-${LocalDateTime.now(ZoneOffset.UTC).plusHours(3)}.csv")
//            .build()
//
//        headers.acceptLanguage = Locale.LanguageRange.parse("ru-RU, en-US")
//        headers.acceptCharset = listOf(Charsets.UTF_8, Charsets.ISO_8859_1, Charsets.US_ASCII)
//
//        headers.acceptCharset.add(Charset.forName("windows-1251"))
//        headers.acceptCharset.add(Charset.forName("windows-1252"))
//        headers.acceptCharset.add(Charset.forName("windows-1254"))
//        headers.acceptCharset.add(Charset.forName("windows-1257"))
//        headers.acceptCharset.add(Charset.forName("windows-1258"))
//        headers.acceptCharset.add(Charset.forName("windows-874"))
//        headers.acceptCharset.add(Charset.forName("windows-949"))
//        headers.acceptCharset.add(Charset.forName("windows-950"))
//        headers.acceptCharset.add(Charset.forName("ANSI_X3.4-1968"))
//
//        val responseEntity = ResponseEntity.ok()
//            .headers(headers)
//            .body(bytes)
//
//        return responseEntity
//    }
//
//    @Deprecated("")
//    @GetMapping("/task")
//    fun getTaskSolutions(
//        @RequestParam accessToken: String,
//        @RequestParam groupAccessToken: String,
//        @RequestParam taskId: Long,
//        model: Model
//    ): Any {
//        logger.info("[${accessToken.padStart(80)}]: Client trying to get task solutions.")
//
//        val eitherEntities = getAdminEntities(accessToken, groupAccessToken)
//        if (eitherEntities.isLeft()) {
//            return eitherEntities.getLeft()
//        }
//        val (webUser, _, group) = eitherEntities.getRight()
//
//        model.addAttribute("accessToken", webUser.accessToken)
//        model.addAttribute("groupAccessToken", group.accessToken)
//
//        val task = taskService.getTaskById(taskId)
//        if (task == null) {
//            logger.info("[${accessToken.padStart(80)}]: Task not found.")
//
//            model.addAttribute("isFound", false)
//            model.addAttribute("message", "Задание не найдено.")
//
//            return model
//        }
//
//        if (!task.groups.contains(group)) {
//            logger.info("[${accessToken.padStart(80)}]: Task not found.")
//
//            model.addAttribute("isFound", false)
//            model.addAttribute("message", "Задание не найдено.")
//
//            return model
//        }
//
//        logger.info("[${accessToken.padStart(80)}]: Task found.")
//        model.addAttribute("taskId", task.id)
//        model.addAttribute("taskName", task.name)
//
//        model.addAttribute("isFound", true)
//        model.addAttribute("name", task.name)
//        model.addAttribute("groupName", group.name)
//        model.addAttribute("solutions", task.solutions.sortedBy { it.id })
//
//        return model
//    }
//
//    @Deprecated("")
//    @GetMapping("/student")
//    fun getStudentSubmissions(
//        @RequestParam accessToken: String,
//        @RequestParam groupAccessToken: String,
//        @RequestParam studentId: Long,
//        model: Model
//    ): Any {
//        logger.info("[${accessToken.padStart(80)}]: Client trying to get student submissions.")
//
//        val eitherEntities = getAdminEntities(accessToken, groupAccessToken)
//        if (eitherEntities.isLeft()) {
//            return eitherEntities.getLeft()
//        }
//        val (webUser, _, group) = eitherEntities.getRight()
//
//        model.addAttribute("accessToken", webUser.accessToken)
//        model.addAttribute("groupAccessToken", group.accessToken)
//
//        val student = studentService.getById(studentId)
//        if (student == null) {
//            logger.info("[${accessToken.padStart(80)}]: Student not found.")
//
//            model.addAttribute("isFound", false)
//            model.addAttribute("message", "Студент не найден.")
//
//            return model
//        }
//
//        if (student.group != group) {
//            logger.info("[${accessToken.padStart(80)}]: Student not found.")
//
//            model.addAttribute("isFound", false)
//            model.addAttribute("message", "Студент не найден.")
//
//            return model
//        }
//
//        logger.info("[${accessToken.padStart(80)}]: Student found.")
//        model.addAttribute("studentId", student.id)
//        model.addAttribute("studentName", student.webUser.username)
//
//        model.addAttribute("isFound", true)
//        model.addAttribute("groupName", group.name)
//        model.addAttribute("solutions", student.solutions.sortedBy { it.id })
//
//        return model
//    }
//
//    @Deprecated("")
//    @GetMapping("/group/table")
//    fun getGroupTable(
//        @RequestParam accessToken: String,
//        @RequestParam groupAccessToken: String,
//        model: Model
//    ): Any {
//        logger.info("[${accessToken.padStart(80)}]: Client trying to get group table.")
//
//        val eitherEntities = getAdminEntities(accessToken, groupAccessToken)
//        if (eitherEntities.isLeft()) {
//            return eitherEntities.getLeft()
//        }
//        val (webUser, _, group) = eitherEntities.getRight()
//
//        model.addAttribute("accessToken", webUser.accessToken)
//        model.addAttribute("groupAccessToken", group.accessToken)
//
//        model.addAttribute("isFound", true)
//        model.addAttribute("tasks", group.tasks.sortedBy { it.id })
//        model.addAttribute("groupName", group.name)
//        model.addAttribute("students", group.students.sortedBy { it.id })
//
//        if (group.tasks.isEmpty()) {
//            model.addAttribute("table", emptyList<Int>())
//            return model
//        }
//
//        val table = MutableList<MutableList<Int>>(
//            (group.students.sortedByDescending { it.id }.firstOrNull()?.id?.toInt()?.plus(1)) ?: 0
//        ) { mutableListOf() }
//
//        group.students.sortedByDescending { it.id }.forEach { student ->
//            val taskList = MutableList(
//                group.tasks.sortedByDescending { it.id }.firstOrNull()?.id?.toInt()?.plus(1) ?: 0
//            ) { -1 }
//
//            group.tasks.sortedByDescending { it.id }.forEach { task ->
//                logger.warn("Task: $task")
//
//                val failedSolution =
//                    student.solutions.find { it.task == task && (it.status == Solution.Status.FAILED || it.status == Solution.Status.ERROR) }
//                val passedSolution =
//                    student.solutions.find { it.task == task && it.status == Solution.Status.PASSED }
//                val inProgressSolution =
//                    student.solutions.find { it.task == task && (it.status == Solution.Status.IN_PROGRESS || it.status == Solution.Status.NOT_STARTED) }
//
//                if (failedSolution != null) {
//                    taskList[task.id!!.toInt()] = 0
//                } else if (passedSolution != null) {
//                    taskList[task.id!!.toInt()] = 1
//                } else if (inProgressSolution != null) {
//                    taskList[task.id!!.toInt()] = 2
//                } else {
//                    taskList[task.id!!.toInt()] = -1
//                }
//
//                table[student.id!!.toInt()] = taskList
//            }
//        }
//        model.addAttribute("table", table.toList())
//
//        return model
//    }
//
//    @Deprecated("")
//    @GetMapping("/student/create")
//    @ResponseBody
//    fun createStudents(
//        @RequestParam accessToken: String,
//        @RequestParam groupAccessToken: String,
//        @RequestParam count: Long,
//        @RequestParam studentAccessTokenPrefix: String,
//        @RequestParam namePrefix: String,
//        model: Model
//    ): ResponseEntity<out Any> {
//        logger.info(accessToken, "Client trying to create many students.")
//
//        val eitherEntities = getAdminEntities(accessToken, groupAccessToken)
//        if (eitherEntities.isLeft()) {
//            return eitherEntities.getLeft()
//        }
//        val (_, _, group) = eitherEntities.getRight()
//
//        val students = studentService.generateStudents(count, studentAccessTokenPrefix, namePrefix, group)
//        logger.info(accessToken, "$count students created.")
//
//        val csv = studentService.convertToCsv(students)
//        logger.info(accessToken, "CSV with created students created.")
//        val headers = HttpHeaders()
//        headers.contentType = MediaType.APPLICATION_OCTET_STREAM
//        headers.contentDisposition = ContentDisposition.builder("attachment")
//            .filename("${group.name}-студенты.csv")
//            .build()
//
//        headers.acceptLanguage = Locale.LanguageRange.parse("ru-RU, en-US")
//        headers.acceptCharset = listOf(Charsets.UTF_8, Charsets.ISO_8859_1, Charsets.US_ASCII)
//        val bytes = csv.toString().toByteArray()
//
//        return ResponseEntity
//            .ok()
//            .headers(headers)
//            .contentLength(bytes.size.toLong())
//            .body(bytes)
//    }
//
//}