package trik.testsys.webclient.util.converter.impl

import trik.testsys.webclient.entity.impl.Task
import trik.testsys.webclient.util.converter.TrikEnumConverter
import javax.persistence.Converter

/**
 * @author Roman Shishkin
 * @since 1.1.0.14-alpha
 */
@Converter(autoApply = true)
class TaskTypeConverter : TrikEnumConverter <Task.TaskType>{

    override fun convertToEntityAttribute(dbData: String?): Task.TaskType? {
        return when (dbData) {
            Task.TaskType.TRIK.dbKey -> Task.TaskType.TRIK
            Task.TaskType.EV3.dbKey -> Task.TaskType.EV3
            else -> null
        }
    }

    override fun convertToDatabaseColumn(attribute: Task.TaskType?): String? {
        return when (attribute) {
            Task.TaskType.TRIK -> Task.TaskType.TRIK.dbKey
            Task.TaskType.EV3 -> Task.TaskType.EV3.dbKey
            else -> null
        }
    }
}