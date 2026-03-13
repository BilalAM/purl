package purl.url.controller

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.server.ResponseStatusException
import purl.url.service.PurlService


@Controller
class RedirectController(private val purlService: PurlService) {

    @GetMapping("/purl/{shortCode}")
    fun redirect(@PathVariable shortCode: String): String {
        val mapping = try {
            purlService.getMapping(shortCode)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }

        return if (mapping.linkAnalysis != null) {
            "redirect:/preview/$shortCode"
        } else {
            "redirect:${mapping.longUrl}"
        }
    }
}
