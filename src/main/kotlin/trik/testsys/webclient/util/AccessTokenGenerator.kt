package trik.testsys.webclient.util

import trik.testsys.webclient.service.impl.WebUserService
import java.security.MessageDigest
import java.util.*

/**
 * @author Roman Shishkin
 * @since 1.1.0
 */
object AccessTokenGenerator {

    fun generateAccessToken(word: String, tokenType: TokenType): String {
        val token = when (tokenType) {
            TokenType.WEB_USER -> generateWebUserAccessToken(word)
            TokenType.GROUP -> generateGroupAccessToken(word)
            TokenType.STUDENT -> generateStudentAccessToken(word)
        }

        return token
    }

    private fun generateWebUserAccessToken(word: String): String {
        val saltedWord = word + Date().time + Random(Date().time).nextInt()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(saltedWord.toByteArray())

        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun generateGroupAccessToken(word: String): String {
        val saltedWord = word + Date().time + Random(Date().time).nextInt()
        val md = MessageDigest.getInstance("SHA-224")
        val digest = md.digest(saltedWord.toByteArray())

        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun generateStudentAccessToken(word: String): String {
        val saltedWord = word + Date().time + Random(Date().time).nextInt()
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(saltedWord.toByteArray())

        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    enum class TokenType {
        WEB_USER, STUDENT, GROUP
    }
}