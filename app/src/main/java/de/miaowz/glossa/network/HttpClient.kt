package de.miaowz.glossa.network

import de.miaowz.glossa.data.dto.OfficialCourseDto
import de.miaowz.glossa.data.dto.OfficialCourseLectureDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

const val BASE_URL = "http://192.168.178.189:8096/"

val httpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(Logging) {
        level = LogLevel.INFO          // logs every request/response in Logcat
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 15_000
    }
    defaultRequest { url(BASE_URL) }
    expectSuccess = true               // throw on 4xx/5xx instead of failing to parse
}

class OfficialCourseApi(private val client: HttpClient) {
    suspend fun getCourses(): List<OfficialCourseDto> =
        client.get("api/course/").body()
}

class OfficialCourseLectureApi(private val client: HttpClient) {
    suspend fun getAllLectures(): List<OfficialCourseLectureDto> =
        client.get("api/course_lecture").body()

    suspend fun getLecturesForCourse(courseId: Long): List<OfficialCourseLectureDto> =
        client.get("api/course/$courseId/lectures").body()
}