package purl.url.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.hibernate.annotations.CreationTimestamp
import java.time.OffsetDateTime


@Entity
data class Mapping(
    @Column(name = "short_url", nullable = false)
    val shortUrl: String = "",

    @Column(name = "long_url", nullable = false)
    var longUrl: String = "",

    @Column(name = "long_url_hashed", nullable = false)
    var longUrlHashed: String = "",

    @Id
    @Column(name = "id", nullable = false)
    var id: Long? = null
) {
    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    var createdAt: OffsetDateTime? = null

}