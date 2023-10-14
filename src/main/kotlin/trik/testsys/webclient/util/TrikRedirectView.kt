package trik.testsys.webclient.util

import org.springframework.web.servlet.view.RedirectView


/**
 * @author Roman Shishkin
 * @since 1.1.0
 */
class TrikRedirectView(path: String) : RedirectView(path, true)
