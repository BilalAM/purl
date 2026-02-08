package purl.url.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.web.bind.annotation.*
import purl.url.service.UserService

data class AuthRequest(val username: String, val password: String)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService,
    private val authenticationManager: AuthenticationManager
) {

    @PostMapping("/register")
    fun register(
        @RequestBody request: AuthRequest,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse
    ): ResponseEntity<Any> {
        return try {
            userService.register(request.username, request.password)
            authenticateAndCreateSession(request.username, request.password, httpRequest, httpResponse)
            ResponseEntity.ok(mapOf("username" to request.username.trim().lowercase()))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to e.message))
        }
    }

    @PostMapping("/login")
    fun login(
        @RequestBody request: AuthRequest,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse
    ): ResponseEntity<Any> {
        return try {
            authenticateAndCreateSession(request.username, request.password, httpRequest, httpResponse)
            ResponseEntity.ok(mapOf("username" to request.username.trim().lowercase()))
        } catch (e: BadCredentialsException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Invalid username or password"))
        }
    }

    @GetMapping("/me")
    fun currentUser(): ResponseEntity<Any> {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated || auth.principal == "anonymousUser") {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Not logged in"))
        }
        return ResponseEntity.ok(mapOf("username" to auth.name))
    }

    private fun authenticateAndCreateSession(
        username: String,
        password: String,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        val authToken = UsernamePasswordAuthenticationToken(username.trim().lowercase(), password)
        val authentication = authenticationManager.authenticate(authToken)
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = authentication
        SecurityContextHolder.setContext(context)
        val repo = HttpSessionSecurityContextRepository()
        repo.saveContext(context, request, response)
    }
}