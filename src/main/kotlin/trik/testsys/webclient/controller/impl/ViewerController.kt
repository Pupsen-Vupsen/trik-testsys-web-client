package trik.testsys.webclient.controller.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.ModelAndView
import trik.testsys.webclient.controller.TrikUserController
import trik.testsys.webclient.entity.impl.*

import trik.testsys.webclient.model.impl.ViewerModel
import trik.testsys.webclient.service.impl.*
import trik.testsys.webclient.util.fp.Either
import trik.testsys.webclient.util.logger.TrikLogger

/**
 * @author Roman Shishkin
 * @since 1.1.0
 */
@RestController
@RequestMapping("\${app.testsys.api.prefix}/viewer")
class ViewerController @Autowired constructor(
    private val webUserService: WebUserService,
    private val groupService: GroupService,
    private val viewerService: ViewerService,
    private val adminService: AdminService
) : TrikUserController {

    @GetMapping
    override fun getAccess(
        @RequestParam accessToken: String,
        modelAndView: ModelAndView
    ): ModelAndView {
        logger.info(accessToken, "Client trying to access viewer page.")

        val eitherViewer = validateViewer(accessToken)
        if (eitherViewer.isLeft()) {
            return eitherViewer.getLeft()
        }
        val viewer = eitherViewer.getRight()

        val groups = mutableSetOf<Group>()
        viewer.admins.forEach { admin ->
            groups.addAll(admin.groups)
        }

        val groupsResult = mutableMapOf<Long, Table>()
        groups.forEach { group ->
            val groupResult = generateGroupResult(group)
            groupsResult[group.id!!] = groupResult
        }

        val adminsResult = mutableMapOf<Long, Table>()
        viewer.admins.forEach { admin ->
            val adminResult = generateAdminResult(admin)
            adminsResult[admin.id!!] = adminResult
        }

        val viewerModel = ViewerModel.Builder()
            .accessToken(accessToken)
            .username(viewer.webUser.username)
            .adminRegToken(viewer.adminRegToken)
            .admins(viewer.admins)
            .groups(groups)
            .groupsResult(groupsResult)
            .adminsResult(adminsResult)
            .build()
        modelAndView.viewName = VIEWER_VIEW_NAME
        modelAndView.addAllObjects(viewerModel.asMap())

        return modelAndView
    }

    private fun generateAdminResult(admin: Admin): Table {
        val tasks = admin.tasks.toList().sortedBy { it.id }

        val students = mutableSetOf<Student>()
        val groups = admin.groups
        groups.forEach { group ->
            students.addAll(group.students)
        }

        val table = generateTable(tasks, students.toList().sortedBy { it.id })

        return table
    }

    private fun generateGroupResult(group: Group): Table {
        val tasks = group.tasks.toList().sortedBy { it.id }
        val students = group.students.toList().sortedBy { it.id }
        val table = generateTable(tasks, students)

        return table
    }

    private fun generateTable(tasks: List<Task>, students: List<Student>): Table {
        val header = mutableListOf<String>()
        tasks.forEach { task ->
            val taskNameWithId = "${task.id}: ${task.name}"
            header.add(taskNameWithId)
        }

        val rows = mutableListOf<TableRow>()
        students.forEach { student ->
            val tasksInfo = mutableListOf<Long>()
            tasks.forEach { task ->
                val failedSolution =
                    student.solutions.find { it.task.id == task.id && (it.status == Solution.Status.FAILED || it.status == Solution.Status.ERROR) }
                val passedSolution =
                    student.solutions.find { it.task == task && it.status == Solution.Status.PASSED }
                val inProgressSolution =
                    student.solutions.find { it.task == task && (it.status == Solution.Status.IN_PROGRESS || it.status == Solution.Status.NOT_STARTED) }

                if (failedSolution != null) {
                    tasksInfo.add(0)
                } else if (passedSolution != null) {
                    tasksInfo.add(1)
                } else if (inProgressSolution != null) {
                    tasksInfo.add(2)
                } else {
                    tasksInfo.add(-1)
                }
            }
            rows.add(TableRow(student.id!!, student.webUser.username, tasksInfo))
        }

        return Table(header, rows)
    }

    data class Table(
        val header: List<String>,
        val rows: List<TableRow>
    )

    data class TableRow(
        val username: String,
        val tasksInfo: List<Long>
    ) {
        constructor(
            studentId: Long, studentName: String,
            tasksInfo: List<Long>
        ) : this(
            "$studentId: $studentName",
            tasksInfo
        )
    }

    fun validateViewer(accessToken: String): Either<ModelAndView, Viewer> {
        val modelAndView = ModelAndView("error")
        val webUser = webUserService.getWebUserByAccessToken(accessToken) ?: run {
            logger.info(accessToken, "Client is not found.")
            modelAndView.addObject("message", "Client not found.")

            return Either.left(modelAndView)
        }

        val viewer = viewerService.getByWebUser(webUser) ?: run {
            logger.info(accessToken, "Client is not a viewer.")
            modelAndView.addObject("message", "Client is not a viewer.")

            return Either.left(modelAndView)
        }

        logger.info(accessToken, "Client is a viewer.")
        return Either.right(viewer)
    }

    companion object {
        private val logger = TrikLogger(this::class.java)

        private const val VIEWER_VIEW_NAME = "viewer"
    }
}