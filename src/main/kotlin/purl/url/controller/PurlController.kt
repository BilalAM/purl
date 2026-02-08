package purl.url.controller

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import purl.url.service.PurlService
import purl.url.service.UserService
import java.net.URI


@RestController
class PurlController(
    private val purlService: PurlService,
    private val userService: UserService
) {

    @PostMapping("/generate")
    fun generateUrl(@RequestBody request: PurlRequest): ResponseEntity<String> {
        if (!validUrl(request.url)) {
            return ResponseEntity.badRequest().body("Invalid url")
        }
        val userId = getAuthenticatedUserId()
        return ResponseEntity.ok(purlService.generatePurl(request.url, userId))
    }

    @GetMapping("/purl/{url}")
    fun redirect(@PathVariable url: String): ResponseEntity<Void> {
        try {
            val originalUrl = purlService.getLongUrl(url)
            val httpHeaders = HttpHeaders()
            httpHeaders.location = URI.create(originalUrl)
            return ResponseEntity(httpHeaders, HttpStatus.FOUND)
        } catch (e: Exception) {
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    fun validUrl(url: String): Boolean = runCatching { URI.create(url).toURL() }.isSuccess

    private fun getAuthenticatedUserId(): Long? {
        val auth = SecurityContextHolder.getContext().authentication ?: return null
        if (!auth.isAuthenticated || auth.principal == "anonymousUser") return null
        return userService.findByUsername(auth.name)?.id
    }
}