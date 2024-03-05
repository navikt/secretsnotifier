package no.nav

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

fun main() = runBlocking {
    println("Looking for repos with secrets alerts...")
    val gitHub = GitHub(httpClient, envOrDie("GITHUB_TOKEN"))
    val teams = Teams(httpClient, envOrDie("TEAMS_TOKEN"))
    val slack = Slack(httpClient, envOrDie("SLACK_TOKEN"))

    val repos = gitHub.reposWithSecretAlerts("navikt")
    println("Found ${repos.size} repos with secrets alerts")
    if (repos.isNotEmpty()) {
        println("Notifying owners")
    }

    mutableMapOf<Team, List<RepoWithSecret>>().apply {
        repos.forEach { repo ->
            val admins = teams.adminsFor(repo.fullName)
            if (admins.isEmpty()) {
                println("Unable to find owners for repo '${repo.name()}' 🤷")
            }
            admins.forEach { team ->
                this[team] = getOrElse(team) { emptyList() } + repo
            }
        }
    }.forEach { (team, repos) ->
        val heading = ":wave: *Hei, ${team.slug}* :github2:"
        val msg = "GitHub har oppdaget hemmeligheter i repo som dere er admin i:\n\n ${linksTo(repos)}\n\n Dersom hemmelighetene er aktive må de *roteres* så fort som mulig, og videre varsling og steg for å avdekke evt. misbruk må iverksettes. \n\n :warning: Husk at Git aldri glemmer, så kun fjerning fra koden er IKKE tilstrekkelig.\n\nNår dette er gjort (eller dersom dette er falske positiver) lukkes varselet ved å velge i nedtrekksmenyen `Close as`.\n\nDu kan også lese mer om håndtering av hemmeligheter i vår <https://sikkerhet.nav.no/docs/sikker-utvikling/hemmeligheter|Security Playbook>"
        val result = slack.send(team.slackChannel, heading, msg)
        println("""Notifying ${team.slug} in ${team.slackChannel}: ${if (result.ok) "✅" else "❌ - " + result.errorMessage}""")
    }
    println("Done!")
}

private fun envOrDie(name: String) = System.getProperty(name)
    ?:System.getenv(name)
    ?: throw RuntimeException("Unable to find env var $name, I'm useless without it")

@OptIn(ExperimentalSerializationApi::class)
private val httpClient = HttpClient(CIO) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        })
    }
}

private fun linksTo(repos: List<RepoWithSecret>) =
    repos.joinToString(separator = "\n• ", prefix = "• ") { repo ->
        "<https://github.com/${repo.fullName}/security/secret-scanning|${repo.name()} (${repo.secretType})>"
    }