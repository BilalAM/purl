package purl.url.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler

@Configuration
@EnableWebSecurity
class SecurityConfiguration {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val csrfTokenHandler = CsrfTokenRequestAttributeHandler()

        http
            .csrf { csrf ->
                csrf
                    .ignoringRequestMatchers("/api/auth/login", "/api/auth/register", "/generate")
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(csrfTokenHandler)
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/favicon.ico").permitAll()
                    .requestMatchers("/generate", "/purl/**").permitAll()
                    .requestMatchers("/api/auth/**").permitAll()
                    // Protected endpoints — only authenticated users
                    .requestMatchers("/api/urls/**").authenticated()
                    .anyRequest().permitAll()
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            }
            .logout { logout ->
                logout
                    .logoutUrl("/api/auth/logout")
                    .logoutSuccessHandler { _, response, _ ->
                        response.status = HttpStatus.OK.value()
                        response.contentType = "application/json"
                        response.writer.write("""{"message":"Logged out"}""")
                    }
            }

        return http.build()
    }
}
