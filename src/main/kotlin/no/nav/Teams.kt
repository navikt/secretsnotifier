package no.nav

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.HttpHeaders.UserAgent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class Teams(private val http: HttpClient, private val authToken: String) {
    private val baseUrl = "https://console.nav.cloud.nais.io/query"

    suspend fun adminsFor(repoFullName: String): List<Team> {
        val teams = mutableListOf<Team>()
        var offset = 0
        do {
            val response = performGqlRequest(repoFullName, offset)
            teams += response.data.nodes
            offset += response.data.pageInfo.totalCount
        } while (response.data.pageInfo.hasNextPage)

        return teams
    }

    private suspend fun performGqlRequest(repoFullName: String, offset: Int): GqlResponse {
        val queryString = """query(${"$"}filter: TeamsFilter, ${"$"}offset: Int, ${"$"}limit: Int) { 
                      teams(filter: ${"$"}filter, offset: ${"$"}offset, limit: ${"$"}limit) { 
                          nodes { 
                              slug 
                              slackChannel 
                          } 
                          pageInfo{ 
                              hasNextPage 
                          } 
                      } 
                  } """
        val reqBody = RequestBody(queryString.replace("\n", " "), Variables(Filter(GitHubFilter(repoFullName, "admin")), 0, 0))
        return http.post(baseUrl) {
            header(Authorization, "Bearer $authToken")
            header(UserAgent, "NAV IT McBotFace")
            header(ContentType, Json)
            setBody(reqBody)
        }.body<GqlResponse>()
    }
}

@Serializable
data class Variables(val filter: Filter, val offset: Int, val limit: Int)

@Serializable
data class Filter(val github: GitHubFilter)

@Serializable
data class GitHubFilter(val repoName: String, val permissionName: String)

@Serializable
data class RequestBody(val query: String, val variables: Variables)

@Serializable
data class GqlResponse(val data: GqlResponseData)

@Serializable
data class GqlResponseData(val teams: GqlResponseTeams)

@Serializable
data class GqlResponseTeams(val nodes: List<Team>, val pageInfo: PageInfo)

@Serializable
data class Team(val slug: String, val slackChannel: String)

@Serializable
data class PageInfo(val totalCount: Int, val hasNextPage: Boolean)


