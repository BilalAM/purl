package purl.url.model

import org.springframework.data.repository.CrudRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service

@Repository
interface UserRepository : CrudRepository<User, Long> {
    fun findByUsername(username: String): User?
}



@Service
class UserDetailsService(private val userRepository: UserRepository) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User not found: $username")

        return org.springframework.security.core.userdetails.User(
            user.username,
            user.password,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
    }
}