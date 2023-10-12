package trik.testsys.webclient.util.converter.impl

import trik.testsys.webclient.entity.impl.TrikFile
import trik.testsys.webclient.util.converter.TrikEnumConverter

/**
 * @since 1.1.0.14-alpha
 */
class TrikFileTypeConverter : TrikEnumConverter<TrikFile.Type> {
    override fun convertToDatabaseColumn(attribute: TrikFile.Type?): String {
        return when (attribute) {
            TrikFile.Type.BENCHMARK -> TrikFile.Type.BENCHMARK.dbKey
            TrikFile.Type.TRAINING -> TrikFile.Type.TRAINING.dbKey
            TrikFile.Type.TEST -> TrikFile.Type.TEST.dbKey
            else -> throw IllegalArgumentException(String.format(ARGUMENT_ERROR, attribute))
        }
    }

    override fun convertToEntityAttribute(dbData: String?): TrikFile.Type {
        return when (dbData) {
            TrikFile.Type.BENCHMARK.dbKey -> TrikFile.Type.BENCHMARK
            TrikFile.Type.TRAINING.dbKey -> TrikFile.Type.TRAINING
            TrikFile.Type.TEST.dbKey -> TrikFile.Type.TEST
            else -> throw IllegalArgumentException(String.format(ARGUMENT_ERROR, dbData))
        }
    }

    companion object {
        private const val ARGUMENT_ERROR = "Unknown trik file type: %s"
    }
}