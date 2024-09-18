package trik.testsys.webclient.controller.impl.main

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.View
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import trik.testsys.core.entity.user.AccessToken
import trik.testsys.core.entity.user.UserEntity
import trik.testsys.webclient.entity.impl.user.Admin
import trik.testsys.webclient.entity.impl.user.Student
import trik.testsys.webclient.entity.impl.user.WebUser
import trik.testsys.webclient.service.impl.GroupService
import trik.testsys.webclient.service.impl.user.AdminService
import trik.testsys.webclient.service.impl.user.StudentService
import trik.testsys.webclient.service.impl.user.ViewerService
import trik.testsys.webclient.service.impl.user.WebUserService
import trik.testsys.webclient.service.token.access.AccessTokenGenerator
import javax.servlet.http.HttpServletRequest

/**
 * @author Roman Shishkin
 * @since 2.0.0
 */
@Controller
@RequestMapping(RegistrationController.REGISTRATION_PATH)
class RegistrationController(
    private val webUserService: WebUserService,
    private val studentService: StudentService,
    private val adminService: AdminService,
    private val groupService: GroupService,
    private val viewerService: ViewerService,

    @Qualifier("studentAccessTokenGenerator") private val studentAccessTokenGenerator: AccessTokenGenerator,
    @Qualifier("webUserAccessTokenGenerator") private val webUserAccessTokenGenerator: AccessTokenGenerator
) {

    @GetMapping
    fun registrationGet() = REGISTRATION_PAGE

    @PostMapping
    fun registrationPost(
        @RequestParam(required = true) regToken: String,
        @RequestParam(required = true) name: String,
        redirectAttributes: RedirectAttributes,
        request: HttpServletRequest
    ): String {
        if (name == regToken) {
            redirectAttributes.addFlashAttribute(
                "message",
                "Имя не должно совпадать с Кодом-регистрации. Попробуйте другой вариант."
            )
            return "redirect:$REGISTRATION_PATH"
        }

        groupService.getGroupByAccessToken(regToken)?.let {
            val accessToken = studentAccessTokenGenerator.generate(name)
            val webUser = WebUser(name, accessToken)
            val student = Student(webUser, it)

            webUserService.save(webUser)
            studentService.save(student)

            return getLoginRedirection(accessToken, redirectAttributes, request)
        }

        viewerService.getByAdminRegToken(regToken)?.let {
            val accessToken = webUserAccessTokenGenerator.generate(name)
            val webUser = WebUser(name, accessToken)
            val admin = Admin(webUser, it)

            webUserService.save(webUser)
            adminService.save(admin)

            return getLoginRedirection(accessToken, redirectAttributes, request)
        }

        redirectAttributes.addFlashAttribute("message", "Некорретный Код-доступа. Попробуйте еще раз.")
        return "redirect:$REGISTRATION_PATH"
    }

    private fun getLoginRedirection(
        accessToken: AccessToken,
        redirectAttributes: RedirectAttributes,
        request: HttpServletRequest
    ): String {
        redirectAttributes.addAttribute(UserEntity::accessToken.name, accessToken)
        request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.TEMPORARY_REDIRECT)

        return "redirect:${LoginController.LOGIN_PATH}"
    }

    companion object {

        internal const val REGISTRATION_PAGE = "registration"
        internal const val REGISTRATION_PATH = "/registration"
    }
}