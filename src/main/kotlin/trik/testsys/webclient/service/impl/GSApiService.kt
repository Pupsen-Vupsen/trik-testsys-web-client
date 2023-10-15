package trik.testsys.webclient.service.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import trik.testsys.webclient.service.TrikApiService
import trik.testsys.webclient.util.TrikMultiPartFile
import trik.testsys.webclient.util.logger.TrikLogger
import java.io.File

@Component
@Suppress("UnnecessaryVariable")
class GSApiService @Autowired constructor(
    @Value("\${app.trik-studio.version}")
    private val trikStudioVersion: String,

    @Value("\${app.trik-studio.timeout}")
    private val trikStudioTimeout: Int,

    private val apiClient: GSApiClient
) : TrikApiService {

    val trikInfo =
        "{ \"trikStudioVersion\": \"${trikStudioVersion}\", \"timeout\": $trikStudioTimeout }"

    fun getProblemFileIds(problemId: Long): List<Long>? {
        val problemDto = apiClient.getProblem(problemId) ?: run {
            logger.error("Problem with id $problemId not found.")
            return null
        }
        logger.info("Problem with id $problemId found.")

        return problemDto.fileIds
    }

    fun createProblem(files: List<MultipartFile>, info: String = trikInfo): ProblemInfo? {
        val problemDto = apiClient.postProblem(info, files) ?: run {
            logger.error("Problem not created.")
            return null
        }

        val problemInfo = ProblemInfo(problemDto.id, problemDto.fileIds)
        logger.info("Problem created: $problemInfo")

        return problemInfo
    }

//    fun renewProblem(problemId: Long, files: List<MultipartFile>, info: String = trikInfo): List<Long>? {
//        val filesBytes = files.map { file -> file.bytes }
//        val problemDto = apiClient.putProblem(problemId, info, filesBytes) ?: run {
//            logger.error("Problem with id $problemId not renewed.")
//            return null
//        }
//
//        logger.info("Problem with id $problemId renewed.")
//
//        return problemDto.fileIds
//    }

//    fun updateProblem(problemId: Long, files: List<MultipartFile>, info: String = trikInfo): List<Long>? {
//        val prevProblemDto = apiClient.getProblem(problemId) ?: run {
//            logger.error("Problem with id $problemId not found.")
//            return null
//        }
//
//        val prevFileIds = prevProblemDto.fileIds ?: run {
//            logger.error("Problem with id $problemId has no files.")
//            return null
//        }
//
//        val prevFiles = mutableListOf<MultipartFile>()
//        prevFileIds.forEach { fileId ->
//            val fileBytes = apiClient.getProblemFile(fileId) ?: run {
//                logger.error("File with id $fileId not found.")
//                return null
//            }
//            val file = TrikMultiPartFile(fileId, fileBytes)
//
//            prevFiles.add(file)
//        }
//        val allFiles = prevFiles + files
//
//        val allFilesBytes = allFiles.map { file -> file.bytes }
//        val problemDto = apiClient.putProblem(problemId, info, allFilesBytes) ?: run {
//            logger.error("Problem with id $problemId not updated.")
//            return null
//        }
//
//        logger.info("Problem with id $problemId updated.")
//
//        val fileIds = problemDto.fileIds ?: return null
//        val newFilesCount = fileIds.size - prevFileIds.size
//        val newFileIds = fileIds.takeLast(newFilesCount)
//
//        return newFileIds
//    }



    data class ProblemInfo(
        val id: Long,
        val fileIds: List<Long>?
    )

    companion object {
        private val logger = TrikLogger(GSApiService::class.java)
    }
}