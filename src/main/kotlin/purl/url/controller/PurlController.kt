package purl.url.controller

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import purl.url.service.PurlService
import java.net.URI


@RestController
class PurlController(private val purlService: PurlService) {


    @PostMapping("/generate")
    fun generateUrl(@RequestBody request: PurlRequest): ResponseEntity<String> {
        if (!validUrl(request.url)) {
            return ResponseEntity.badRequest().body("Invalid url")
        }
        return ResponseEntity.ok(purlService.generatePurl(request.url))

    }

    @GetMapping("/purl/{url}")
    fun redirect(@PathVariable url: String): ResponseEntity<Void> {
        try {
            val originalUrl = purlService.getLongUrl(url)
            val httpHeaders = HttpHeaders()
            httpHeaders.location = URI.create(originalUrl)
            return ResponseEntity(httpHeaders, HttpStatus.FOUND)
        } catch (e: Exception) {
            // no need to log anything for now, just return not found 404......
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    fun validUrl(url: String): Boolean = runCatching { URI.create(url).toURL() }.isSuccess

}