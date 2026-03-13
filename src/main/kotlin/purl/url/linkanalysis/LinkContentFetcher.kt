package purl.url.linkanalysis

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Service


data class LinkPreview(
    val title: String?,
    val description: String?,
    val siteName: String?,
    val imageUrl: String?,
    val type: String?,
    val bodySnippet: String?
)

@Service
class LinkContentFetcher {

    fun fetch(url: String): LinkPreview {
        val document = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .followRedirects(true)
            .timeout(10_000)
            .get()

        return LinkPreview(
            title = document.meta("og:title", "twitter:title") ?: document.title().takeIf { it.isNotBlank() },
            description = document.meta("og:description", "twitter:description", "description"),
            siteName = document.meta("og:site_name"),
            imageUrl = document.meta("og:image"),
            type = document.meta("og:type"),
            bodySnippet = document.extractBodySnippet()
        )
    }

    private fun Document.meta(vararg keys: String): String? {
        for (key in keys) {
            val value = selectFirst("meta[property=$key]")?.attr("content")
                ?: selectFirst("meta[name=$key]")?.attr("content")
            if (!value.isNullOrBlank()) return value
        }
        return null
    }

    private fun Document.extractBodySnippet(): String? {
        select("script, style, iframe, form, svg, video, audio").remove()
        return buildString {
            for (el in select("h1, h2, h3, h4, h5, h6")) appendLine("Heading: ${el.text()}")
            for (el in select("p")) appendLine("Paragraph: ${el.text()}")
            for (el in select("a")) appendLine("Link: ${el.text()}")
        }.take(4000).takeIf { it.isNotBlank() }
    }
}
