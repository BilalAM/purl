package purl.url.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import purl.url.model.Mapping
import purl.url.model.MappingRepository
import java.util.*

class PurlServiceTest {

    private lateinit var mappingRepository: MappingRepository
    private lateinit var purlService: PurlService

    @BeforeEach
    fun setUp() {
        mappingRepository = mock(MappingRepository::class.java)
        purlService = PurlService(mappingRepository)
    }

    @Test
    fun `generatePurl returns existing short url when mapping exists`() {
        val longUrl = "https://example.com/very/long/url"
        val existingMapping = Mapping(
            id = 100001L,
            shortUrl = "q0T",
            longUrl = longUrl,
            longUrlHashed = "somehash"
        )

        `when`(mappingRepository.findMappingByLongUrlHashed(anyString()))
            .thenReturn(listOf(existingMapping))

        val result = purlService.generatePurl(longUrl)

        assertEquals("http://localhost:8080/purl/q0T", result)
        verify(mappingRepository, never()).save(any())
    }

    @Test
    fun `generatePurl creates new mapping when none exists`() {
        val longUrl = "https://example.com/new/url"

        `when`(mappingRepository.findMappingByLongUrlHashed(anyString()))
            .thenReturn(emptyList())
        `when`(mappingRepository.getNextCounterSeed())
            .thenReturn("100001")

        val result = purlService.generatePurl(longUrl)

        assertEquals("http://localhost:8080/purl/q0V", result)
        verify(mappingRepository).save(any())
    }

    @Test
    fun `getLongUrl returns original url for valid purl`() {
        val shortCode = "q0T"
        val expectedLongUrl = "https://example.com/original"
        val mapping = Mapping(
            id = 99999L,
            shortUrl = shortCode,
            longUrl = expectedLongUrl,
            longUrlHashed = "hash"
        )

        `when`(mappingRepository.findById("99999"))
            .thenReturn(Optional.of(mapping))

        val result = purlService.getLongUrl(shortCode)

        assertEquals(expectedLongUrl, result)
    }
}
