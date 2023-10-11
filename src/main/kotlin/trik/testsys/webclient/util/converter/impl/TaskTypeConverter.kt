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
            "TRIK" -> Task.TaskType.TRIK
            "EV3" -> Task.TaskType.EV3
            else -> null
        }
    }

    override fun convertToDatabaseColumn(attribute: Task.TaskType?): String? {
        return when (attribute) {
            Task.TaskType.TRIK -> "TRIK"
            Task.TaskType.EV3 -> "EV3"
            else -> null
        }
    }
}