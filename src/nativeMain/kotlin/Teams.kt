import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable

class Teams(private val http: HttpClient, private val authToken: String) {
    private val baseUrl = "https://teams.nav.cloud.nais.io/query"

    suspend fun adminsFor(repoFullName: String): List<Team> {
        val queryString = """query { teamsWithPermissionInGitHubRepo(repoName: "$repoFullName", permissionName: "admin") { slug slackChannel } }"""
        val reqBody = RequestBody(queryString)
        return http.post(baseUrl) {
            header(HttpHeaders.Authorization, "Bearer $authToken")
            header(HttpHeaders.UserAgent, "NAV IT McBotFace")
            header(HttpHeaders.ContentType, Json)
            setBody(reqBody)
        }.body<GqlResponse>().data.teamsWithPermissionInGitHubRepo
    }
}

@Serializable
data class RequestBody(val query: String)

@Serializable
data class GqlResponse(val data: GqlResponseData)

@Serializable
data class GqlResponseData(val teamsWithPermissionInGitHubRepo: List<Team>)

@Serializable
data class Team(val slug: String, val slackChannel: String)