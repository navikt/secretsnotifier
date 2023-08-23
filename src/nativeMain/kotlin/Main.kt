import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import platform.posix.getenv

fun main() = runBlocking {
    println("Looking for repos with secrets alerts...")
    val gitHub = GitHub(httpClient, envOrDie("GITHUB_TOKEN"))
    val teams = Teams(httpClient, envOrDie("TEAMS_TOKEN"))
    val slack = Slack(httpClient, envOrDie("SLACK_TOKEN"))

    val repos = gitHub.reposWithSecretAlerts("navikt")
    println("Found ${repos.size} repos with secrets alerts, notifying owners.")

    mutableMapOf<Team, List<RepoWithSecret>>().apply {
        repos.forEach { repo ->
            teams.adminsFor(repo.fullName).forEach { team ->
                this[team] = getOrElse(team) { emptyList() } + repo
            }
        }
    }.forEach { (team, repos) ->
        val heading = ":wave: *Hei, ${team.slug}* :github2:"
        val msg = "GitHub har oppdaget hemmeligheter i repo som dere er admin i:\n\n ${linksTo(repos)}\n\nKlikk på linkene for å se detaljer. Dersom hemmelighetene er aktive må de roteres så fort som mulig, og videre varsling og steg for å avdekke evt. misbruk må iverksettes. Når dette er gjort (eller dersom dette er falske positiver) lukkes varselet ved å velge i nedtrekksmenyen `Close as`.\n\nDu kan også lese mer om håndtering av hemmeligheter i vår <https://sikkerhet.nav.no/docs/sikker-utvikling/hemmeligheter|Security Playbook>"
        val result = slack.send("#jk-tullekanal", heading, msg)
        if (!result.ok) println("Error while notifying ${team.slug} in ${team.slackChannel}: ${result.errorMessage}")
    }
    println("Done!")
}

@OptIn(ExperimentalForeignApi::class)
private fun envOrDie(name: String): String = getenv(name)?.toKString()
    ?: throw RuntimeException("Unable to find env var $name, I'm useless without it")

@OptIn(ExperimentalSerializationApi::class)
private val httpClient = HttpClient(Curl) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        })
    }
}

private fun linksTo(repos: List<RepoWithSecret>) =
    repos.joinToString(separator = "\n• ", prefix = "• ") {
        "<https://github.com/${it.fullName}/security/secret-scanning|${it.name()}>"
    }