package trik.testsys.webclient.util.converter

import javax.persistence.AttributeConverter
import javax.persistence.Converter

/**
 * @since 1.1.0.14-alpha
 * @author Roman Shishkin
 */
interface TrikEnumConverter<T: Enum<T>> : AttributeConverter<T, String>