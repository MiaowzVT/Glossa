# Glossa

Android grammar-library app (Kotlin, Jetpack Compose, Room). Single module `:app`, package
`de.miaowz.glossa`.

@ARCHITECTURE.md

## Commands

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest          # JVM tests
./gradlew :app:connectedDebugAndroidTest  # needs a device/emulator
```

## Rules

- Read `ARCHITECTURE.md` §10 (Recipes) before adding a screen, field, format or entity type.
- Keep `ARCHITECTURE.md` up to date when a change affects layering, schema, stored formats,
  navigation or conventions.
- Never rename a `SpanFormat` `@SerialName`, the `CategoryType` / `Rating` enum constants, or
  change the `{base|reading}` ruby markup: all are stored in the database.
- Any entity change needs a DB version bump, a migration, and the generated schema JSON in
  `app/schemas/` committed.
- UI code goes through the repositories (`GrammarRepository`, `VocabularyRepository`,
  `OfficialCourseRepository`, `OfficialCourseLectureRepository`), never the DAOs, entities, DTOs
  or the Ktor client.
- Display user text with `RubyText`, not `Text`.
- No DI framework: new dependencies go in `GlossaApplication` + a `CreationExtras` accessor in
  `ui/GlossaViewModelFactory.kt`.
- UI strings live in `res/values/strings.xml` (feature-prefixed keys) and are read with
  `stringResource`; new or touched screens must use them, and an existing screen is migrated as a
  whole (ARCHITECTURE §8.4). ViewModels expose errors/messages as state (`@StringRes`/enum), never
  as finished strings.
- Backend responses are decoded from text (`OfficialCourseJson`), never via the `Content-Type`.
- Match the existing style: sparse KDoc on non-obvious behavior only.
- After AI-assisted changes, append an entry (prompt + summary of changes) to `app/LOGBOOK.MD`.
