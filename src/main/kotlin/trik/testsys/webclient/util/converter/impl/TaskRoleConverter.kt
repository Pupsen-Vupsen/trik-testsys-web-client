package trik.testsys.webclient.util.converter.impl

import trik.testsys.webclient.entity.impl.Task
import trik.testsys.webclient.util.converter.TrikEnumConverter
import trik.testsys.webclient.util.exception.impl.TrikIllegalArgumentException
import javax.persistence.Converter

/**
 * @since 1.1.0.14-alpha
 */
@Converter(autoApply = true)
class TaskRoleConverter : TrikEnumConverter<Task.TaskRole> {

    override fun convertToDatabaseColumn(attribute: Task.TaskRole?): String {
        return when (attribute) {
            Task.TaskRole.PARENT -> Task.TaskRole.PARENT.dbKey
            Task.TaskRole.CHILD -> Task.TaskRole.CHILD.dbKey
            else -> throw TrikIllegalArgumentException(String.format(ARGUMENT_ERROR, attribute))
        }
    }

    override fun convertToEntityAttribute(dbData: String?): Task.TaskRole {
        return when (dbData) {
            Task.TaskRole.PARENT.dbKey -> Task.TaskRole.PARENT
            Task.TaskRole.CHILD.dbKey -> Task.TaskRole.CHILD
            else -> throw TrikIllegalArgumentException(String.format(ARGUMENT_ERROR, dbData))
        }
    }

    companion object {
        private const val ARGUMENT_ERROR = "Unknown task role: %s"
    }
}