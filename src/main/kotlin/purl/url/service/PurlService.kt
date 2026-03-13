package purl.url.service

import com.google.common.hash.Hashing
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import purl.url.exceptions.UserCreatedMaxUrlsException
import purl.url.linkanalysis.LinkAnalysisLLMService
import purl.url.model.Mapping
import purl.url.model.MappingRepository
import java.util.*

@Service
class PurlService(
    private val mappingRepository: MappingRepository,
    private val linkAnalysisLLMService: LinkAnalysisLLMService,
    @Value("\${app.url}") private val appUrl: String
) {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(PurlService::class.java)
    }

    fun generatePurl(longUrl: String, userId: Long? = null): String {
        validateMaxUrlsOfUser(userId)
        val longUrlHashed = hashLongUrl(longUrl)
        val existingMapping = findExistingMapping(longUrlHashed)

        return if (existingMapping != null) {
            returnExistingMapping(existingMapping, longUrl, userId)
        } else {
            createNewMapping(longUrl, longUrlHashed, userId)
        }
    }

    private fun returnExistingMapping(mapping: Mapping, longUrl: String, userId: Long?): String {
        logger.info("Found existing mapping for $longUrl, returning short url: ${mapping.shortUrl}")
        if (userId != null && mapping.userId == null) {
            mapping.userId = userId
            mappingRepository.save(mapping)
        }
        return constructCompleteShortUrl(mapping.shortUrl)
    }

    private fun createNewMapping(longUrl: String, longUrlHashed: String, userId: Long?): String {
        val nextCounter = mappingRepository.getNextCounterSeed()
        val shortUrlEncoded = encodeCounter(nextCounter)
        logger.info("Generated new short url: $shortUrlEncoded for $longUrl")

        val analysis = try {
            val result = linkAnalysisLLMService.doAnalysis(longUrl)
            mapOf(
                "originalUrl" to result.originalUrl,
                "previewDescription" to result.previewDescription,
                "previewImageUrl" to result.previewImageUrl
            )
        } catch (e: Exception) {
            logger.warn("Link analysis failed for {}: {}", longUrl, e.message)
            null
        }

        mappingRepository.save(
            Mapping(
                id = nextCounter.toLong(),
                shortUrl = shortUrlEncoded,
                longUrl = longUrl,
                longUrlHashed = longUrlHashed,
                userId = userId,
                linkAnalysis = analysis
            )
        )
        return constructCompleteShortUrl(shortUrlEncoded)
    }

    private fun validateMaxUrlsOfUser(userId: Long?) {
        userId ?: return
        if (mappingRepository.countMappingByUserId(userId) > 3) {
            throw UserCreatedMaxUrlsException()
        }
    }

    fun getMapping(purl: String): Mapping {
        val decodedId = Base64.getUrlDecoder().decode(purl).toString(Charsets.UTF_8)
        return mappingRepository.findById(decodedId)
            .orElseThrow { NoSuchElementException("Mapping not found for $purl") }
    }

    fun getLongUrl(purl: String): String = getMapping(purl).longUrl

    private fun constructCompleteShortUrl(shortUrl: String): String =
        appUrl + shortUrl

    private fun encodeCounter(counter: String): String =
        Base64.getUrlEncoder().withoutPadding().encode(counter.toByteArray()).toString(Charsets.UTF_8)

    private fun hashLongUrl(longUrl: String): String =
        Hashing.sha256().hashString(longUrl, Charsets.UTF_8).toString()

    private fun findExistingMapping(longUrlHashed: String): Mapping? =
        mappingRepository.findMappingByLongUrlHashed(longUrlHashed).firstOrNull()
}