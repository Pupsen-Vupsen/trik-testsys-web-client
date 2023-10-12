package trik.testsys.webclient.entity.impl

import javax.persistence.*

/**
 * @author Roman Shishkin
 * @since 1.1.0
 */
@Entity
@Table(name = "TRIK_FILES")
class TrikFile (
    @ManyToOne
    @JoinColumn(
        name = "task_id", referencedColumnName = "id",
        nullable = false
    ) var task: Task,

    @Column(nullable = false, columnDefinition = "VARCHAR(200) DEFAULT ''")
    var name: String,

    @Column(nullable = false)
    var type: Type,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, unique = true)
    val id: Long? = null

    enum class Type(val dbKey: String) {
        TEST("T"),
        BENCHMARK("B"),
        TRAINING("TR")
    }
}