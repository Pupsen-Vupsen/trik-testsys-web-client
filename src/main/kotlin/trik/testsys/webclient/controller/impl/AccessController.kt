package trik.testsys.webclient.controller.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import trik.testsys.webclient.service.impl.*
import trik.testsys.webclient.util.TrikRedirectView
import trik.testsys.webclient.util.logger.TrikLogger
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.*


@RestController
class AccessController @Autowired constructor(
    private val webUserService: WebUserService,
    private val superUserService: SuperUserService,
    private val adminService: AdminService,
    private val studentService: StudentService,
    private val groupService: GroupService,
    private val developerService: DeveloperService,
    private val viewerService: ViewerService,

    private val apiClient: GSApiClient
) {

    @GetMapping("/access")
    fun getAccess(
        @RequestParam accessToken: String,
        redirectAttributes: RedirectAttributes,
        model: Model
    ): Any {
        val lowerCasedToken = accessToken.lowercase(Locale.getDefault()).replace(" ", "")
        logger.info(lowerCasedToken, "Client trying to access service.")

        groupService.getGroupByAccessToken(lowerCasedToken)?.let {
            logger.info(lowerCasedToken, "Client is a new student.")
            if (!it.isRegistrationOpen) {
                // TODO: Add message about closed registration.
                logger.info(lowerCasedToken, "Registration is closed.")
                return@let
            }
            redirectAttributes.addAttribute("groupAccessToken", lowerCasedToken)
            return TrikRedirectView("/student/registration")
        }
        redirectAttributes.addAttribute("accessToken", lowerCasedToken)

        viewerService.getByAdminRegToken(lowerCasedToken)?.let {
            logger.info(lowerCasedToken, "Client is a new admin.")
            return TrikRedirectView("/admin/registration")
        }

        val webUser = webUserService.getWebUserByAccessToken(lowerCasedToken) ?: run {
            logger.info(lowerCasedToken, "Client is not authorized.")
            return model
        }
        webUser.lastLoginDate = LocalDateTime.now(UTC).plusHours(3)
        webUserService.saveWebUser(webUser)

        superUserService.getSuperUserByWebUser(webUser)?.let {
            logger.info(lowerCasedToken, "Client is a super user.")
            return TrikRedirectView("/superuser")
        }

        adminService.getAdminByWebUser(webUser)?.let {
            logger.info(lowerCasedToken, "Client is an admin.")
            return TrikRedirectView("/admin")
        }

        developerService.getByWebUser(webUser)?.let {
            logger.info(lowerCasedToken, "Client is a developer.")
            return TrikRedirectView("/developer")
        }

        studentService.getByWebUser(webUser)?.let {
            logger.info(lowerCasedToken, "Client is a student.")
            val group = it.group
            if (!group.isAccessible) {
                //TODO: Add message about group is not accessible.
                logger.info(lowerCasedToken, "Group is not accessible.")
                return@let
            }
            return TrikRedirectView("/student")
        }

        viewerService.getByWebUser(webUser)?.let {
            logger.info(lowerCasedToken, "Client is a viewer.")

            return TrikRedirectView("/viewer")
        }

        return model
    }

    @GetMapping
    fun get(model: Model): Model {
        val bytes = apiClient.getSubmissionFile(1)

        return model
    }

    companion object {
        private val logger = TrikLogger(this::class.java)
    }
}