package trik.testsys

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.servlet.MultipartConfigFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.unit.DataSize
import org.springframework.util.unit.DataUnit
import trik.testsys.webclient.util.logger.TrikLogger

import javax.servlet.MultipartConfigElement

@SpringBootApplication
@Configuration
class Application @Autowired constructor(
    @Value("\${app.grading-system.url}") private val gradingSystemUrl: String

) {

    @Bean
    fun multipartConfigElement(): MultipartConfigElement {
        val factory = MultipartConfigFactory()
        factory.setMaxFileSize(DataSize.of(4, DataUnit.MEGABYTES))
        factory.setMaxRequestSize(DataSize.of(4, DataUnit.MEGABYTES))
        return factory.createMultipartConfig()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(Application::class.java, *args)
            logger.info("Application started.")

        }

        private val logger = TrikLogger(Application::class.java)
    }
}
