package trik.testsys.webclient.util

import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.InputStream

class TrikMultiPartFile (
    val id: Long,
    private val bytes: ByteArray,
) : MultipartFile {


    override fun getInputStream(): InputStream {
        return bytes.inputStream()
    }

    override fun getName(): String {
        return id.toString()
    }

    override fun getOriginalFilename(): String? {
        return null
    }

    override fun getContentType(): String {
        return "multipart/form-data"
    }

    override fun isEmpty(): Boolean {
        return bytes.isEmpty()
    }

    override fun getSize(): Long {
        return bytes.size.toLong()
    }

    override fun getBytes(): ByteArray {
        return bytes
    }

    override fun transferTo(dest: File) {
        dest.writeBytes(bytes)
    }
}