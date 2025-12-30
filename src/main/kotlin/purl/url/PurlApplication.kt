package purl.url

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PurlApplication

fun main(args: Array<String>) {
    System.getenv().forEach { (key, value) ->
        println("$key=$value")
    }
    runApplication<PurlApplication>(*args)
}
