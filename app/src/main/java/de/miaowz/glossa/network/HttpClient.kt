package de.miaowz.glossa.network

import de.miaowz.glossa.data.dto.OfficialCourseDto
import de.miaowz.glossa.data.dto.OfficialCourseLectureDto
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import timber.log.Timber

const val BASE_URL = "http://10.0.2.2:8096/"

val httpClient = HttpClient(OkHttp) {
    glossaDefaults()
    install(Logging) {
        level = LogLevel.INFO          // request line + response status, no bodies
        // Debug level: failures are logged by the ViewModels with their severity (see SyncError.log).
        logger = object : Logger {
            override fun log(message: String) = Timber.tag("Ktor").d(message)
        }
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 15_000
    }
}

/** Settings shared by the app client and test clients (MockEngine). */
fun HttpClientConfig<*>.glossaDefaults() {
    defaultRequest { url(BASE_URL) }
    expectSuccess = true               // throw on 4xx/5xx instead of failing to parse
}

// Bodies are read as text and decoded by OfficialCourseJson: the backend's Content-Type is not
// reliable, so Ktor's content negotiation would refuse some responses.

class OfficialCourseApi(private val client: HttpClient) {
    suspend fun getCourses(): List<OfficialCourseDto> =
        OfficialCourseJson.decodeCourses(client.get("api/course").bodyAsText())

    /**
     * `null` when the backend no longer has the course: today it answers `[]`, once its
     * array bug is fixed the controller's 404 branch applies.
     */
    suspend fun getCourse(courseId: Long): OfficialCourseDto? {
        val body = try {
            client.get("api/course/$courseId").bodyAsText()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) {
                Timber.i("Course %d not found on the backend (HTTP 404)", courseId)
                return null
            }
            throw e
        }
        return OfficialCourseJson.decodeCourse(body).also {
            if (it == null) Timber.i("Course %d not found on the backend (empty body)", courseId)
        }
    }
}

class OfficialCourseLectureApi(private val client: HttpClient) {
    suspend fun getLecturesForCourse(courseId: Long): List<OfficialCourseLectureDto> =
        OfficialCourseJson.decodeLectures(client.get("api/course/$courseId/lectures").bodyAsText())
}
