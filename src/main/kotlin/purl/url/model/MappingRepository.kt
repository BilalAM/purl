package purl.url.model

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface MappingRepository : CrudRepository<Mapping, String> {

    @Query("select nextval('mapping_id_seq')", nativeQuery = true)
    fun getNextCounterSeed(): String

    fun findMappingByShortUrl(shortUrl: String): Mapping?

    fun findMappingByLongUrlHashed(longUrlHashed: String): List<Mapping?>

    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<Mapping>

    fun countMappingByUserId(userId: Long): Long

}