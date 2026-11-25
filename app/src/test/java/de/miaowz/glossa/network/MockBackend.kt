package de.miaowz.glossa.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import java.io.IOException

/**
 * A fake backend for repository/ViewModel tests: [routes] maps a path to a body (sent without a
 * JSON content type, like the real backend). Unknown paths answer 404; [offline] simulates a
 * failed connection.
 */
class MockBackend(routes: Map<String, String> = emptyMap()) {
    val routes = routes.toMutableMap()
    var offline = false
    var status = HttpStatusCode.OK
    val requests = mutableListOf<String>()

    val client = HttpClient(MockEngine { request ->
        val path = request.url.encodedPath
        requests += path
        if (offline) throw IOException("Network is unreachable")
        val body = this@MockBackend.routes[path]
        if (body == null) respond("Not Found", HttpStatusCode.NotFound) else respond(body, status)
    }) { glossaDefaults() }

    val courseApi = OfficialCourseApi(client)
    val lectureApi = OfficialCourseLectureApi(client)
}
