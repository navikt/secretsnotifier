package no.nav

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class Slack(private val http: HttpClient, private val authToken: String) {

    private val baseUrl = "https://slack.com/api/chat.postMessage"

    suspend fun send(channel: String, heading: String, msg: String): SlackResponse {
        val toSend = Message(channel, listOf(
            markdownBlock(heading),
            dividerBlock(),
            markdownBlock(msg)
        ))

        return http.post(baseUrl) {
            header(HttpHeaders.Authorization, "Bearer $authToken")
            header(HttpHeaders.UserAgent, "NAV IT McBotFace")
            header(HttpHeaders.ContentType, Json)
            setBody(toSend)
        }.body()
    }

}

@Serializable
data class Message(val channel: String, val blocks: List<Block>)

@Serializable
data class Block(val type: String, val text: Text? = null)

@Serializable
data class Text(val type: String, val text: String)

@Serializable
data class SlackResponse(
    val ok: Boolean,
    @SerialName("error") val errorMessage: String?
)

private fun markdownBlock(txt: String) = Block(
    type = "section",
    text = Text(
        "mrkdwn",
        txt
    )
)

private fun dividerBlock() = Block(
    type = "divider"
)
