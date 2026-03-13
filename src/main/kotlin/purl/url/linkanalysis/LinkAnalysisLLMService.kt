package purl.url.linkanalysis

import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service


@Service
class LinkAnalysisLLMService(
    private val contentFetcher: LinkContentFetcher,
    chatClientBuilder: ChatClient.Builder
) {
    private val chatClient = chatClientBuilder.build()

    fun doAnalysis(shortUrl: String): LinkAnalysisResult {
        val preview = contentFetcher.fetch(shortUrl)

        val response = chatClient.prompt()
            .system("""
                You are a link preview assistant for a URL shortener service.
                Your job is to help users understand what a shortened link leads to
                BEFORE they click on it, so they can make an informed decision.

                Write in plain language as if you're telling a friend "here's what this link is."
                Be honest — if the metadata is sparse, say what you can and note that
                details are limited. Never fabricate information that isn't in the metadata.

                Keep your response to 2-4 sentences max. No markdown, no bullet points,
                just a natural paragraph.
            """.trimIndent())
            .user("""
                Here is the metadata I extracted from a webpage. Based on this,
                write a short preview description that tells someone what they're
                about to open:

                URL: $shortUrl
                Page title: ${preview.title ?: "N/A"}
                Page description: ${preview.description ?: "N/A"}
                Website name: ${preview.siteName ?: "N/A"}
                Content type: ${preview.type ?: "N/A"}
                Page content snippet: ${preview.bodySnippet ?: "N/A"}

                Cover these points where the information is available:
                - What website/service is this from?
                - What is the page about? (e.g. a product, article, video, profile, booking)
                - Is there anything noteworthy the user should know before clicking?
            """.trimIndent())
            .call()
            .content() ?: ""

        return LinkAnalysisResult(
            originalUrl = shortUrl,
            shortUrl = shortUrl,
            previewDescription = response.trim(),
            previewImageUrl = preview.imageUrl ?: ""
        )
    }
}

data class LinkAnalysisResult(
    val originalUrl: String,
    val shortUrl: String,
    val previewDescription: String,
    val previewImageUrl: String
)
