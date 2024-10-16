package trik.testsys.webclient.view.impl

import trik.testsys.core.entity.user.AccessToken
import trik.testsys.core.view.user.UserView
import trik.testsys.webclient.entity.user.impl.Developer
import trik.testsys.webclient.util.fromTimeZone
import java.time.LocalDateTime
import java.util.*

data class DeveloperView(
    override val id: Long?,
    override val name: String,
    override val accessToken: AccessToken,
    override val lastLoginDate: LocalDateTime?,
    override val creationDate: LocalDateTime?,
    override val additionalInfo: String,
    val contests: List<ContestView>,
    val tasks: List<TaskView>,
    val polygons: List<TaskFileView>,
    val exercises: List<TaskFileView>,
    val solutions: List<TaskFileView>
) : UserView<Developer> {

    override fun toEntity(timeZone: TimeZone) = Developer(
        name, accessToken
    ).also {
        it.id = id
        it.lastLoginDate = lastLoginDate?.fromTimeZone(timeZone)
        it.additionalInfo = additionalInfo
    }
}
