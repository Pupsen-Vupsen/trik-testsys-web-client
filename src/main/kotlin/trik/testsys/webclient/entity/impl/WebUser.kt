package trik.testsys.webclient.entity.impl

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import javax.persistence.*

@Entity
@Table(name = "WEB_USERS")
class WebUser(
    @Column(
        nullable = false, length = 50,
        columnDefinition = "VARCHAR(300) DEFAULT ''"
    ) var username: String,

    @Column(
        nullable = false, unique = true, length = 50,
        columnDefinition = "VARCHAR(100) DEFAULT ''"
    ) val accessToken: String
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, unique = true)
    val id: Long? = null

    @Column(columnDefinition = "VARCHAR(1000) DEFAULT ''", nullable = true)
    var additionalInfo: String? = null

    @Column(nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    var registrationDate: LocalDateTime = LocalDateTime.now(UTC).plusHours(3)

    @OneToOne(mappedBy = "webUser", cascade = [CascadeType.ALL])
    lateinit var admin: Admin

    @OneToOne(mappedBy = "webUser", cascade = [CascadeType.ALL])
    lateinit var superUser: SuperUser

    @OneToOne(mappedBy = "webUser", cascade = [CascadeType.ALL])
    lateinit var developer: Developer

    @OneToMany(mappedBy = "webUser", cascade = [CascadeType.ALL])
    val students: MutableSet<Student> = mutableSetOf()

    /**
     * @author Roman Shishkin
     * @since 1.1.0
     */
    @OneToOne(mappedBy = "webUser", cascade = [CascadeType.ALL])
    lateinit var viewer: Viewer

    /**
     * @author Roman Shishkin
     * @since 1.1.0
     */
    @Column(nullable = true, columnDefinition = "DATETIME")
    var lastLoginDate: LocalDateTime? = null

    /**
     * @author Roman Shishkin
     * @since 1.1.0
     */
    enum class Status {
        NOT_FOUND,
        ADMIN,
        SUPER_USER,
        WEB_USER,
        DEVELOPER
    }
}