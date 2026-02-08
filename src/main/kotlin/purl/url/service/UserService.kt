package purl.url.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import purl.url.model.User
import purl.url.model.UserRepository

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun register(username: String, password: String): User {
        if (username.isBlank() || (password.isBlank() ||password.length < 6)) {
            throw IllegalArgumentException("Username must not be blank and password must be at least 6 characters")
        }
        if (userRepository.findByUsername(username) != null) {
            throw IllegalArgumentException("Username already exists")
        }
        val user = User(
            username = username.trim().lowercase(),
            password = passwordEncoder.encode(password)!!
        )
        return userRepository.save(user)
    }

    fun findByUsername(username: String): User? = userRepository.findByUsername(username)
}