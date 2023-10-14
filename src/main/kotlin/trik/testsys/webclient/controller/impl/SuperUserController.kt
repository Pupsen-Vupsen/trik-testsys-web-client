package trik.testsys.webclient.controller.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.ModelAndView
import trik.testsys.webclient.controller.TrikUserController
import trik.testsys.webclient.entity.impl.Viewer
import trik.testsys.webclient.entity.impl.WebUser

import trik.testsys.webclient.service.impl.SuperUserService
import trik.testsys.webclient.service.impl.WebUserService
import trik.testsys.webclient.models.ResponseMessage
import trik.testsys.webclient.service.impl.AdminService
import trik.testsys.webclient.service.impl.ViewerService
import trik.testsys.webclient.util.logger.TrikLogger


@RequestMapping("/superuser")
@RestController
class SuperUserController @Autowired constructor(
    private val superUserService: SuperUserService,
    private val webUserService: WebUserService,
    private val adminService: AdminService,
    private val viewerService: ViewerService
) : TrikUserController {

    @GetMapping
    override fun getAccess(
        @RequestParam accessToken: String,
        modelAndView: ModelAndView
    ): ModelAndView {
        TODO("Not yet implemented")
    }

    @PostMapping("/viewer/create")
    fun createViewers(
        @RequestParam accessToken: String,
        @RequestParam count: Long
    ): ResponseEntity<Any> {
        logger.info(accessToken, "Client trying to create viewers.")

        val status = validateSuperUser(accessToken)
        if (status != WebUser.Status.SUPER_USER) {
            logger.info(accessToken, "Client is not a super user.")
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ResponseMessage(403, "You are not a superuser!"))
        }

        val strings = mutableListOf<String>()

        for (i in 1..count) {
            val username = "$i"
            val webUser = webUserService.saveWebUser(username)
            val viewer = viewerService.save(webUser)

            val viewerString = "${webUser.id}, ${webUser.accessToken}, ${viewer.adminRegToken}"
            strings.add(viewerString)
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(strings)
    }

    @PostMapping("/webUser/create", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createWebUser(
        @RequestParam accessToken: String,
        @RequestParam username: String,
        model: Model
    ): Any {
        logger.info("[${accessToken.padStart(80)}]: Client trying to create web user.")

        val status = validateSuperUser(accessToken)
        if (status != WebUser.Status.SUPER_USER) {
            logger.info("[${accessToken.padStart(80)}]: Client is not a super user.")
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ResponseMessage(403, "You are not a superuser!"))
        }

        val webUser = webUserService.saveWebUser(username)
        logger.info("[${accessToken.padStart(80)}]: Web user created.")

        model.addAttribute("accessToken", accessToken)
        model.addAttribute("username", webUser.username)
        model.addAttribute("id", webUser.id!!)
        model.addAttribute("webUserAccessToken", webUser.accessToken)

        return model
    }

    @PostMapping("/webUser/raise", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun raiseWebUserToAdmin(
        @RequestParam accessToken: String,
        @RequestParam webUserId: Long,
        model: Model
    ): Any {
        logger.info("[${accessToken.padStart(80)}]: Client trying to raise web user to admin.")

        val status = validateSuperUser(accessToken)
        if (status != WebUser.Status.SUPER_USER) {
            logger.info("[${accessToken.padStart(80)}]: Client is not a super user.")
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ResponseMessage(403, "You are not a superuser!"))
        }

        webUserService.getWebUserById(webUserId)
            ?: run {
                logger.info("[${accessToken.padStart(80)}]: Web user with id $webUserId not found.")

                model.addAttribute("isRaised", false)
                model.addAttribute("accessToken", accessToken)
                model.addAttribute("message", "Пользователь с ID $webUserId не найден!")

                return model
            }

        adminService.getAdminByWebUserId(webUserId)
            ?: run {
                logger.info("[${accessToken.padStart(80)}]: Raising web user to admin.")
                val admin = adminService.saveAdmin(webUserId)
                    ?: return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ResponseMessage(500, "Internal server error!"))

                logger.info("[${accessToken.padStart(80)}]: Web user successfully raised to admin.")

                model.addAttribute("isRaised", true)
                model.addAttribute("accessToken", accessToken)
                model.addAttribute("id", admin.id!!)
                model.addAttribute("webUserId", admin.webUser.id!!)
                model.addAttribute("webUserAccessToken", admin.webUser.accessToken)

                return model
            }

        logger.info("[${accessToken.padStart(80)}]: Web user is already admin.")

        model.addAttribute("isRaised", false)
        model.addAttribute("accessToken", accessToken)
        model.addAttribute("message", "Пользователь уже администратор!")

        return model
    }

    @PostMapping("/admin/create", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createAdmin(
        @RequestParam accessToken: String,
        @RequestParam username: String,
        model: Model
    ): Any {
        logger.info("[${accessToken.padStart(80)}]: Client trying to create admin.")

        val status = validateSuperUser(accessToken)
        if (status != WebUser.Status.SUPER_USER) {
            logger.info("[${accessToken.padStart(80)}]: Client is not a super user.")
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ResponseMessage(403, "You are not a superuser!"))
        }

        val webUser = webUserService.saveWebUser(username)
        val admin = adminService.saveAdmin(webUser)
            ?: return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseMessage(500, "Internal server error!"))

        logger.info("[${accessToken.padStart(80)}]: Admin created.")

        model.addAttribute("accessToken", accessToken)
        model.addAttribute("id", admin.id!!)
        model.addAttribute("webUserId", admin.webUser.id!!)
        model.addAttribute("webUserAccessToken", admin.webUser.accessToken)

        return model
    }


    private fun validateSuperUser(accessToken: String): Enum<*> {
        val webUser = webUserService.getWebUserByAccessToken(accessToken) ?: return WebUser.Status.NOT_FOUND
        superUserService.getSuperUserByWebUserId(webUser.id!!) ?: return WebUser.Status.WEB_USER

        return WebUser.Status.SUPER_USER
    }

    companion object {
        private val logger = TrikLogger(this::class.java)
    }
}