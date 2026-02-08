package purl.url.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.OffsetDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val username: String = "",

    @Column(nullable = false)
    val password: String = "",

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    val createdAt: OffsetDateTime? = null
)