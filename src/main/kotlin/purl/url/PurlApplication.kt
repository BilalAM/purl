package purl.url

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PurlApplication

fun main(args: Array<String>) {
    runApplication<PurlApplication>(*args)
}
