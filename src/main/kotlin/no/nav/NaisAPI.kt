package no.nav

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.HttpHeaders.UserAgent
import kotlinx.serialization.Serializable

class NaisAPI(private val http: HttpClient, private val authToken: String) {
    private val baseUrl = "https://console.nav.cloud.nais.io/graphql"

    suspend fun allTeams(): List<Team> {
        val allTeams = mutableListOf<Team>()
        var teamsOffset = ""
        val repoOffset = ""
        do {
            log.info("Querying for teams at offset '$teamsOffset'")
            val gqlResponse = performGqlRequest(teamsOffset, repoOffset)
            allTeams += gqlResponse.data.teams.nodes
            teamsOffset = gqlResponse.data.teams.pageInfo.endCursor ?: ""
        } while (gqlResponse.data.teams.pageInfo.hasNextPage)

        return allTeams
    }

    private suspend fun performGqlRequest(teamsOffset: String, repoOffset: String): PaginatedGqlResponse {
        val queryString = """query getTeamsAndRepos {
                                teams(first:100 after:"$teamsOffset") {
                                    pageInfo {
                                        totalCount
                                        hasNextPage
                                        endCursor
                                    }
                                    nodes {
                                        slug
                                        slackChannel
                                        repositories(first:100 after:"$repoOffset") {
                                            pageInfo {
                                                totalCount
                                                hasNextPage
                                                endCursor
                                            }
                                            nodes {
                                                name
                                            }
                                        }
                                    }
                                }
                            } """
        val reqBody = RequestBody(queryString.replace("\n", " "))
        return http.post(baseUrl) {
            header(Authorization, "Bearer $authToken")
            header(UserAgent, "NAV IT McBotFace")
            header(ContentType, Json)
            setBody(reqBody)
        }.body<PaginatedGqlResponse>()
    }
}

@Serializable
data class RequestBody(val query: String)

@Serializable
data class PaginatedGqlResponse(val data: GqlResponseData)

@Serializable
data class GqlResponseData(val teams: GqlResponseTeams)

@Serializable
data class GqlResponseTeams(val nodes: List<Team>, val pageInfo: PageInfo)

@Serializable
data class Team(val slug: String, val slackChannel: String, val repositories: NaisApiRepositories)

@Serializable
data class NaisApiRepositories(val nodes: List<NaisApiRepository>, val pageInfo: PageInfo)

@Serializable
data class NaisApiRepository(val name: String)

@Serializable
data class PageInfo(val hasNextPage: Boolean, val endCursor: String?)


