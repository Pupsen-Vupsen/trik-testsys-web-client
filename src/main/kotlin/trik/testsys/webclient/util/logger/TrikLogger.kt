package trik.testsys.webclient.util.logger

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import trik.testsys.webclient.util.exception.impl.TrikIllegalStateException

/**
 * @author Roman Shishkin
 * @since 1.1.0
 *
 * Implementation of custom logger from Trik TestSys components. Uses [Logger].
 */
class TrikLogger<T>(clazz: Class<T>): Logger by LoggerFactory.getLogger(clazz) {

    fun info(userAccessToken: String, message: String) {
        val fullMessage = createFullMessage(userAccessToken, message)

        this.info(fullMessage)
    }

    fun warn(userAccessToken: String, message: String) {
        val fullMessage = createFullMessage(userAccessToken, message)

        this.warn(fullMessage)
    }

    fun error(userAccessToken: String, message: String) {
        val fullMessage = createFullMessage(userAccessToken, message)

        this.error(fullMessage)
    }

    fun errorAndThrow(userAccessToken: String, message: String): Nothing {
        val fullMessage = createFullMessage(userAccessToken, message)

        this.error(fullMessage)
        throw TrikIllegalStateException("Error: $fullMessage")
    }

    /**
     * Creates new full message using user access token as prefix in pattern:
     *
     * "[ {([MAX_TOKEN_LENGTH] - [userAccessToken].length) * {" "}} {[userAccessToken]} ]: {[message]}"
     */
    private fun createFullMessage(userAccessToken: String, message: String): String {
        val accessTokenWithBrackets = "[ ${userAccessToken.padStart(MAX_TOKEN_LENGTH)} ]"

        return "$accessTokenWithBrackets: $message"
    }

    companion object {
        private const val MAX_TOKEN_LENGTH = 70
    }
}