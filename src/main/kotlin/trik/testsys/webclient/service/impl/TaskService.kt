package trik.testsys.webclient.service.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

import trik.testsys.webclient.entity.impl.Developer
import trik.testsys.webclient.entity.impl.Task
import trik.testsys.webclient.entity.impl.TrikFile
import trik.testsys.webclient.repository.impl.TaskRepository
import trik.testsys.webclient.service.TrikService

@Service
class TaskService @Autowired constructor(
    private val taskRepository: TaskRepository,
    private val groupService: GroupService,
    private val trikFileService: TrikFileService,
) : TrikService {

    /**
     * @author Roman Shishkin
     * @since 1.1.0
     */
    fun save(task: Task): Task {
        return taskRepository.save(task)
    }

    /**
     * @return [Boolean.true] if task was deleted, [Boolean.false] otherwise
     * @author Roman Shishkin
     * @since 1.1.0
     */
    fun deleteTask(taskId: Long): Boolean {
        val task = taskRepository.findTaskById(taskId) ?: return false
        taskRepository.delete(task)

        return true
    }

    /**
     * @author Roman Shishkin
     * @since 1.1.0
     */
    fun update(
        taskId: Long,
        name: String,
        description: String,
        tests: List<MultipartFile>,
        training: MultipartFile?,
        benchmark: MultipartFile?,
    ): Task? {
        val task = taskRepository.findTaskById(taskId) ?: return null

        task.name = name
        task.description = description
        task.countOfTests = tests.size.toLong()
        task.hasBenchmark = benchmark != null
        task.hasTraining = training != null
        task.countOfTests = tests.size.toLong()

//        trikFileService.deleteAllByTaskId(taskId)
        val allFiles = tests.map { TrikFile(task, it.originalFilename!!, TrikFile.Type.TEST) }.toMutableSet()

        benchmark?.let {
            val benchmarkFile = TrikFile(task, benchmark.originalFilename!!, TrikFile.Type.BENCHMARK)
            allFiles.add(benchmarkFile)
        }

        training?.let {
            val trainingFile = TrikFile(task, training.originalFilename!!, TrikFile.Type.TRAINING)
            allFiles.add(trainingFile)
        }

        task.trikFiles = allFiles
        trikFileService.saveAll(allFiles)

        return taskRepository.save(task)
    }

    fun getAllGroupTasks(groupId: Long): Set<Task>? {
        val group = groupService.getGroupById(groupId) ?: return null

        return group.tasks
    }

    fun getAllPublic(): Set<Task> {
        val allTasks = taskRepository.findAll()
        return allTasks.filter { it.isPublic }.toSet()
    }

    fun getTaskById(taskId: Long): Task? {
        return taskRepository.findTaskById(taskId)
    }

    /**
     * @since 1.1.0.14-alpha
     */
    fun saveParentTask(name: String, description: String, developer: Developer): Task {
        val task = Task(name, description, developer)
        return taskRepository.save(task)
    }

    /**
     * @since 1.1.0.14-alpha
     */
    fun saveChildTask(
        task: Task,
        tests: List<TrikFile>,
        training: TrikFile?,
        benchmark: TrikFile?
    ): Task? {
        val trikFiles = mutableSetOf<TrikFile>()

        trikFiles.addAll(tests)
        task.countOfTests = tests.size.toLong()

        benchmark?.let {
            trikFiles.add(it)
            task.hasBenchmark = true
        }
        training?.let {
            trikFiles.add(it)
            task.hasTraining = true
        }

        trikFileService.saveAll(trikFiles)

        return taskRepository.save(task)
    }
}