package purl.url.controller

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.server.ResponseStatusException
import purl.url.service.PurlService


@Controller
class PreviewController(private val purlService: PurlService) {

    @GetMapping("/preview/{shortCode}")
    fun preview(@PathVariable shortCode: String, model: Model): String {
        val mapping = try {
            purlService.getMapping(shortCode)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }

        val analysis = mapping.linkAnalysis ?: emptyMap()

        model.addAttribute("originalUrl", mapping.longUrl)
        model.addAttribute("description", analysis["previewDescription"] ?: "No preview available for this link.")
        model.addAttribute("imageUrl", analysis["previewImageUrl"] ?: "")
        model.addAttribute("siteName", extractDomain(mapping.longUrl))

        return "preview"
    }

    private fun extractDomain(url: String): String =
        runCatching { java.net.URI(url).host?.removePrefix("www.") }.getOrNull() ?: "Unknown site"
}
