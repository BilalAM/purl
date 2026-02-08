
package purl.url.service

import com.google.common.hash.Hashing
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value
import purl.url.model.Mapping
import purl.url.model.MappingRepository
import java.util.*


@Service
class PurlService(
    private val mappingRepository: MappingRepository,
    @Value("\${app.url}") private val appUrl: String
) {

    companion object {
        private val UTF_8 = Charsets.UTF_8
        private val logger: Logger = LoggerFactory.getLogger(PurlService::class.java)
    }


    fun generatePurl(longUrl: String, userId: Long? = null): String {
        val longUrlHashed = getLongUrlHashed(longUrl)
        val existingMapping = getExistingMapping(longUrlHashed)
        if (existingMapping != null) {
            logger.info("Found existing mapping for $longUrl, returning short url: ${existingMapping.shortUrl}")
            if (userId != null && existingMapping.userId == null) {
                existingMapping.userId = userId
                mappingRepository.save(existingMapping)
            }
            return constructCompleteShortUrl(existingMapping.shortUrl)
        }


        val nextCounter = mappingRepository.getNextCounterSeed()
        val shortUrlHashed = getShortUrlHashed(nextCounter)
        logger.info("Generated new short url: $shortUrlHashed for $longUrl")

        mappingRepository.save(
            Mapping(
                id = nextCounter.toLong(),
                shortUrl = shortUrlHashed,
                longUrl = longUrl,
                longUrlHashed = longUrlHashed,
                userId = userId
            )
        )
        return constructCompleteShortUrl(shortUrlHashed)
    }

    fun getLongUrl(purl: String): String {
        val decodedId = Base64.getDecoder().decode(purl).toString(UTF_8)
        return mappingRepository.findById(decodedId)
            .orElseThrow { error("Mapping not found for $purl") }
            .longUrl
    }


    private fun constructCompleteShortUrl(shortUrl: String): String =
        appUrl + shortUrl

    private fun getShortUrlHashed(nextCounter: String): String =
        Base64.getEncoder().encode(nextCounter.toByteArray()).toString(UTF_8)

    private fun getLongUrlHashed(longUrl: String): String = Hashing.sha256().hashString(longUrl, UTF_8).toString()


    private fun getExistingMapping(longUrlHashed: String): Mapping? =
        mappingRepository.findMappingByLongUrlHashed(longUrlHashed)
            .firstOrNull()

}