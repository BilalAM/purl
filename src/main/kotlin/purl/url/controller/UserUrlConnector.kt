package purl.url.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import purl.url.model.MappingRepository
import purl.url.service.UserService

data class UrlEntry(val id: Long?, val shortUrl: String, val longUrl: String, val createdAt: String?)
data class UpdateUrlRequest(val longUrl: String)

@RestController
@RequestMapping("/api/urls")
class UserUrlController(
    private val mappingRepository: MappingRepository,
    private val userService: UserService,
    @Value("\${app.url}") private val appUrl: String
) {

    @GetMapping
    fun getUserUrls(): ResponseEntity<Any> {
        val userId = getAuthenticatedUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val urls = mappingRepository.findAllByUserIdOrderByCreatedAtDesc(userId).map {
            UrlEntry(it.id, appUrl + it.shortUrl, it.longUrl, it.createdAt?.toString())
        }
        return ResponseEntity.ok(urls)
    }

    @PutMapping("/{id}")
    fun updateUrl(@PathVariable id: String, @RequestBody request: UpdateUrlRequest): ResponseEntity<Any> {
        val userId = getAuthenticatedUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val mapping = mappingRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        if (mapping.userId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "Not your URL"))
        }

        mapping.longUrl = request.longUrl
        mappingRepository.save(mapping)
        return ResponseEntity.ok(UrlEntry(mapping.id, appUrl + mapping.shortUrl, mapping.longUrl, mapping.createdAt?.toString()))
    }

    @DeleteMapping("/{id}")
    fun deleteUrl(@PathVariable id: String): ResponseEntity<Any> {
        val userId = getAuthenticatedUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val mapping = mappingRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        if (mapping.userId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "Not your URL"))
        }

        mappingRepository.delete(mapping)
        return ResponseEntity.ok(mapOf("message" to "Deleted"))
    }

    private fun getAuthenticatedUserId(): Long? {
        val auth = SecurityContextHolder.getContext().authentication ?: return null
        if (!auth.isAuthenticated || auth.principal == "anonymousUser") return null
        return userService.findByUsername(auth.name)?.id
    }
}