package purl.linkanalysis

import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service


@Service
class LinkAnalysisLLMService(
    private val contentFetcher: LinkContentFetcher,
    chatClientBuilder: ChatClient.Builder
) {
    private val chatClient = chatClientBuilder.build()

    companion object {

        private val SYSTEM_PROMPT = """
    You are a link preview assistant for a URL shortener service.
    Your job is to help users understand what a shortened link leads to
    BEFORE they click on it, so they can make an informed decision.

    Write in plain language as if you're telling a friend "here's what this link is."
    Be honest — if the metadata is sparse, say what you can and note that
    details are limited. Never fabricate information that isn't in the metadata.

    Keep your response to 2 small sentences max. No markdown, no bullet points,
    just a natural paragraph which is small, concise and makes sense to what the url is about."
""".trimIndent()

        private val METADATA_PROMPT = """
    Here is the metadata I extracted from a webpage. Based on this,
    write a short preview description that tells someone what they're
    about to open:

    URL: {url}
    Page title: {title}
    Page description: {description}
    Website name: {siteName}
    Content type: {type}
    Page content snippet: {bodySnippet}

    Cover these points where the information is available:
    - What website/service is this from?
    - What is the page about? (e.g. a product, article, video, profile, booking)
    - Is there anything noteworthy the user should know before clicking?
    But again, keep it very small, concise and to the point.
""".trimIndent()

        private val URL_ONLY_PROMPT = """
    I could not fetch any metadata from this webpage, but I still need you to
    describe what this link likely leads to based ONLY on the URL structure.

    URL: {url}

    Break down the URL and use clues from:
    - The domain name (e.g. medium.com = blogging platform, booking.com = hotel booking)
    - The path segments (e.g. /hotel/pt/ = a hotel in Portugal, /@TheCodemonkey/ = a user profile)
    - Slugs and keywords in the path (e.g. "semantic-search-with-embeddings" = an article about semantic search)
    - Query parameters if they hint at content (e.g. lang=en, category=tech)

    Be upfront that this is a best guess based on the URL alone since the page
    content could not be loaded.
""".trimIndent()
    }

    fun doAnalysis(longUrl: String, shortUrl: String): LinkAnalysisResult {
        val preview = contentFetcher.fetch(longUrl)

        val userPrompt = if (preview != null) {
            METADATA_PROMPT
                .replace("{url}", longUrl)
                .replace("{title}", preview.title ?: "N/A")
                .replace("{description}", preview.description ?: "N/A")
                .replace("{siteName}", preview.siteName ?: "N/A")
                .replace("{type}", preview.type ?: "N/A")
                .replace("{bodySnippet}", preview.bodySnippet ?: "N/A")
        } else {
            URL_ONLY_PROMPT.replace("{url}", longUrl)
        }

        val response = chatClient.prompt()
            .system(SYSTEM_PROMPT)
            .user(userPrompt)
            .call()
            .content() ?: ""

        return LinkAnalysisResult(
            originalUrl = longUrl,
            shortUrl = shortUrl,
            previewDescription = response.trim(),
            previewImageUrl = preview?.imageUrl ?: ""
        )
    }
}

data class LinkAnalysisResult(
    val originalUrl: String,
    val shortUrl: String,
    val previewDescription: String,
    val previewImageUrl: String
)
