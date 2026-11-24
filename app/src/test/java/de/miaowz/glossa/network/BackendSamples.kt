package de.miaowz.glossa.network

/** Response bodies as the Glossa backend (Express + mysql2) sends them. */
object BackendSamples {
    const val COURSES = """[
      {"id":2,"course_name":"{日本語|にほんご} N5","course_description":"Hiragana, Katakana and your first sentences.","course_level":"N5","course_language":"Japanese","created_at":"2026-11-20T10:15:00.000Z","updated_at":"2026-11-21T08:00:00.000Z"},
      {"id":1,"course_name":"Español básico","course_description":"Greetings & numbers","course_level":"A1","course_language":"Spanish","created_at":"2026-11-17T14:09:42.000Z","updated_at":"2026-11-17T14:09:42.000Z"}
    ]"""

    /** `GET /api/course/2`: the backend wraps the single course in an array. */
    const val COURSE_AS_ARRAY = """[{"id":2,"course_name":"{日本語|にほんご} N5","course_description":"Hiragana, Katakana and your first sentences.","course_level":"N5","course_language":"Japanese","created_at":"2026-11-20T10:15:00.000Z","updated_at":"2026-11-21T08:00:00.000Z"}]"""

    const val COURSE_AS_OBJECT = """{"id":2,"course_name":"{日本語|にほんご} N5","course_description":"Hiragana, Katakana and your first sentences.","course_level":"N5","course_language":"Japanese","created_at":"2026-11-20T10:15:00.000Z","updated_at":"2026-11-21T08:00:00.000Z"}"""

    /** `lecture_contents` is RichText JSON stored as a string, so it arrives escaped. */
    const val LECTURES = """[
      {"id":5,"course_id":2,"lecture_name":"{挨拶|あいさつ}","lecture_contents":"{\"text\":\"{今日|こんにち}は means hello.\",\"spans\":[{\"start\":0,\"end\":10,\"format\":{\"type\":\"bold\"}},{\"start\":11,\"end\":16,\"format\":{\"type\":\"color\",\"argb\":-1739980}}]}","created_at":"2026-11-20T10:20:00.000Z","updated_at":"2026-11-22T19:01:12.000Z"},
      {"id":6,"course_id":2,"lecture_name":"Numbers","lecture_contents":"{\"text\":\"一、二、三\"}","created_at":null,"updated_at":null}
    ]"""
}
