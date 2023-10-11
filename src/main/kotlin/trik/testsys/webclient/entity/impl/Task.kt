package trik.testsys.webclient.entity.impl

import trik.testsys.webclient.util.converter.impl.TaskRoleConverter
import trik.testsys.webclient.util.converter.impl.TaskTypeConverter
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "TASKS")
class Task(
    @Column(nullable = false, columnDefinition = "VARCHAR(1000) DEFAULT ''")
    var name: String,

    @Column(nullable = false, columnDefinition = "VARCHAR(1000) DEFAULT ''")
    var description: String,

    /**
     * @author Roman Shishkin
     * @since 1.1.0
     */
    @ManyToOne
    @JoinColumn(
        name = "developer_id", referencedColumnName = "id",
        nullable = false
    ) val developer: Developer
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, unique = true)
    val id: Long? = null

    @ManyToMany(mappedBy = "tasks")
    val groups: MutableSet<Group> = mutableSetOf()

    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    var countOfTests: Long = 0L

    @OneToMany(mappedBy = "task", cascade = [CascadeType.ALL])
    val solutions: MutableSet<Solution> = mutableSetOf()

    /**
     * @since 1.1.0
     */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    var hasBenchmark: Boolean = false

    /**
     * @since 1.1.0
     */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    var hasTraining: Boolean = false

    /**
     * @since 1.1.0
     */
    @OneToMany(mappedBy = "task", cascade = [CascadeType.ALL])
    lateinit var trikFiles: MutableSet<TrikFile>

    /**
     * @since 1.1.0
     */
    @ManyToMany
    @JoinTable(
        name = "TASKS_BY_ADMINS",
        joinColumns = [JoinColumn(name = "task_id")],
        inverseJoinColumns = [JoinColumn(name = "admin_id")]
    )
    lateinit var admins: MutableSet<Admin>

    /**
     * @since 1.1.0
     */
    @Column(nullable = true)
    var deadline: LocalDateTime? = null

    /**
     * @since 1.1.0
     */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    var isPublic: Boolean = false

    /**
     * @since 1.1.0.14-alpha
     */
    @Column(nullable = true, columnDefinition = "ENUM('TRIK', 'EV3') DEFAULT null")
    @Convert(converter = TaskTypeConverter::class)
    var taskType: TaskType? = null

    /**
     * @since 1.1.0.14-alpha
     */
    @Column(nullable = false, columnDefinition = "ENUM('PARENT', 'CHILD') DEFAULT 'PARENT'")
    @Convert(converter = TaskRoleConverter::class)
    var taskRole: TaskRole = TaskRole.PARENT

    /**
     * @since 1.1.0.14-alpha
     */
    @ManyToOne
    @JoinColumn(
        name = "parent_task_id", referencedColumnName = "id",
        nullable = true
    )
    var parentTask: Task? = null

    /**
     * @since 1.1.0.14-alpha
     */
    @OneToMany(mappedBy = "parentTask", cascade = [CascadeType.ALL])
    val childTasks: MutableSet<Task> = mutableSetOf()

    /**
     * @since 1.1.0
     */
    fun getFullName() = "$id: $name"

    /**
     * @since 1.1.0.14-alpha
     */
    enum class TaskType {
        TRIK, EV3
    }

    /**
     * @since 1.1.0.14-alpha
     */
    enum class TaskRole {
        PARENT, CHILD
    }
}