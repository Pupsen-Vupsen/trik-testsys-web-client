package trik.testsys.webclient.service.entity.impl

import org.springframework.stereotype.Service
import trik.testsys.core.service.named.AbstractNamedEntityService
import trik.testsys.webclient.entity.impl.TaskFile
import trik.testsys.webclient.entity.user.impl.Developer
import trik.testsys.webclient.repository.TaskFileRepository

/**
 * @author Roman Shishkin
 * @since 2.0.0
 **/
@Service
class TaskFileService : AbstractNamedEntityService<TaskFile, TaskFileRepository>() {

    override fun validateName(entity: TaskFile) =
        super.validateName(entity) && !entity.name.contains(entity.developer.accessToken)

    override fun validateAdditionalInfo(entity: TaskFile) =
        super.validateAdditionalInfo(entity) && !entity.additionalInfo.contains(entity.developer.accessToken)

    fun findByDeveloper(developer: Developer) = repository.findByDeveloper(developer)

    fun findByDeveloper(developer: Developer, type: TaskFile.TaskFileType) = repository.findByDeveloperAndType(developer, type)
}