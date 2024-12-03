package no.nav

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders
import java.lang.RuntimeException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class Slack(private val http: HttpClient, private val authToken: String) {

    private val baseUrl = "https://slack.com/api/chat.postMessage"

    suspend fun sendAlert(repo: RepoWithSecret, owner: Team) {
        log.info("Alerting ${owner.slug} in ${owner.slackChannel}")
        val heading = ":wave: *Hei, ${owner.slug}* :github2:"
        val msg =
            "GitHub har oppdaget hemmeligheter i repo som dere eier:\n\n ${linkTo(repo)}\n\n Dersom hemmelighetene er aktive må de *roteres* så fort som mulig, og videre varsling og steg for å avdekke evt. misbruk må iverksettes. \n\n :warning: Husk at Git aldri glemmer, så kun fjerning fra koden er IKKE tilstrekkelig.\n\nNår dette er gjort (eller dersom dette er falske positiver) lukkes varselet ved å velge i nedtrekksmenyen `Close as`.\n\nDu kan også lese mer om håndtering av hemmeligheter i vår <https://sikkerhet.nav.no/docs/sikker-utvikling/hemmeligheter|Security Playbook>"
        owner.slackChannel?.let {
            channel -> postTheMessage(channel, heading, msg)
        }?: log.warn("${owner.slug} doesn't have a slack channel")
    }

    private suspend fun postTheMessage(channel: String, heading: String, msg: String): SlackResponse {
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

private fun linkTo(repo: RepoWithSecret) =
    "<https://github.com/${repo.fullName}/security/secret-scanning|${repo.name()} (${repo.secretType})>"

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
