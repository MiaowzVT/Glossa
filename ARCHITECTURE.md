# Glossa — Architecture

Glossa is a single-module Android app for building a personal **grammar library** and
**vocabulary library** for language learning. Grammar entries have a title, a rich-text description
and example sentences; vocabulary entries have a term, a meaning, rich-text notes and example
sentences. Both are categorized by **Language → Category** and support **furigana** (ruby text)
everywhere. A bottom navigation bar switches between Home, Grammar and Vocabulary. Vocabulary is
practiced with flashcards scheduled by **FSRS** (spaced repetition), started from Home.

This document is the working reference for extending the code base. It describes the layers, the
data formats that must stay stable, the invariants each component relies on, and step-by-step
recipes for common additions. Paths are relative to `app/src/main/java/de/miaowz/glossa/` unless
stated otherwise.

---

## 1. Tech stack

| Concern        | Choice                                                                 |
|----------------|------------------------------------------------------------------------|
| Language       | Kotlin 2.2, JVM target 11                                              |
| UI             | Jetpack Compose (Material 3, BOM `2026.02.01`), single Activity        |
| Navigation     | Navigation Compose 2.10 with **type-safe `@Serializable` routes**      |
| State          | `ViewModel` + `StateFlow`, collected with `collectAsStateWithLifecycle` |
| Persistence    | Room 2.8 (KSP), schema exported to `app/schemas/`                      |
| Serialization  | kotlinx.serialization JSON (routes + `RichText` storage)               |
| DI             | **None** — manual container in `GlossaApplication`                    |
| SDK            | `minSdk 35`, `targetSdk/compileSdk 37`                                  |
| Build          | AGP 9.4, version catalog `gradle/libs.versions.toml`, configuration cache on |

Package / application id: `de.miaowz.glossa`.

---

## 2. Layering

```
┌──────────────────────────────────────────────────────────────────────────┐
│ ui/                     Compose screens + ViewModels (one package/feature)│
│   navigation/           NavHost, bottom bar, @Serializable routes         │
│   home/ grammar/ vocabulary/ review/   common/ (shared UI)   theme/        │
├───────────────────────────────┬──────────────────────────────────────────┤
│ richtext/   (reusable widget) │ ruby/     (reusable widget)               │
│ RichText model, editor View,  │ {base|reading} markup, RubySpan,          │
│ toolbar, span operations      │ RubyTextView, RubyText, FuriganaDialog    │
├───────────────────────────────┴──────────────────────────────────────────┤
│ data/        GrammarRepository, VocabularyRepository (+ Taxonomy)  ←→ model│
│   db/                   Room: entities, DAOs, database                    │
├──────────────────────────────────────────────────────────────────────────┤
│ srs/                    FSRS-5 algorithm + day-based scheduling (pure JVM) │
└──────────────────────────────────────────────────────────────────────────┘
```

Dependency rules (keep these when adding code):

- `ui/*` depends on `data` (repository + `data.model`), `richtext`, `ruby`, and `srs.Rating`.
  **UI never touches `data.db` entities or the DAO.**
- `data` depends on `richtext.RichText` (as a value type, for encode/decode),
  `ruby.RubyMarkup` (for plain-text extraction) and `srs` (scheduling). It does not depend on
  Android Views.
- `srs` is pure Kotlin (no Android, no Room) and depends on nothing else in the app.
- `richtext` and `ruby` are feature-agnostic widgets. They know nothing about grammar entries.
  `richtext` uses `ruby.RubyMarkup` (for `insertRuby`), `ruby` uses `richtext.RichText`
  (for rendering). That mutual dependency is intentional and limited to these two packages.
- Feature packages under `ui/` (`home`, `grammar`, `vocabulary`, `review`) don't import each other; they
  communicate only via navigation callbacks. Code shared between features lives in `ui/common/`,
  which must not import feature packages.

---

## 3. App wiring

| File | Role |
|------|------|
| `GlossaApplication.kt` | Manual dependency container. Holds one `database` and `grammarRepository` / `vocabularyRepository`, all `lazy` (so tests/previews don't open the DB). |
| `MainActivity.kt` | `enableEdgeToEdge()`, `GlossaTheme { GlossaNavHost() }`. `windowSoftInputMode="adjustResize"` in the manifest. |
| `ui/GlossaViewModelFactory.kt` | `CreationExtras.grammarRepository` / `.vocabularyRepository` extensions — the *only* bridge from ViewModels to the container. |

ViewModels are created inline in the screen's default parameter:

```kotlin
viewModel: GrammarEditorViewModel = viewModel {
    GrammarEditorViewModel(createSavedStateHandle(), grammarRepository)
}
```

**Adding a new app-wide dependency:** add a `lazy` property to `GlossaApplication` and a matching
`CreationExtras.<name>` extension in `ui/GlossaViewModelFactory.kt`. Do not introduce Hilt/Koin
without a deliberate decision — the pattern is meant to stay this small.

---

## 4. Data layer

### 4.1 Schema (Room, `data/db/Entities.kt`, DB version **3**, file `glossa.db`)

```
languages            categories                    grammar_entries                     example_sentences
─────────            ──────────                    ───────────────                     ─────────────────
id PK                id PK                         id PK                               id PK
name NOCASE UNIQUE   languageId FK→languages       title          (String, ruby markup) entryId FK→grammar_entries
                       ON DELETE CASCADE           description    (RichText JSON)        ON DELETE CASCADE
                     type (CategoryType,           descriptionPlain (search text)      position
                       default 'GRAMMAR')          languageId FK  ON DELETE RESTRICT   sentence     (RichText JSON)
                     name NOCASE                   categoryId? FK ON DELETE SET_NULL   translation  (RichText JSON)
                     UNIQUE(languageId,type,name)  createdAt, updatedAt (epoch ms)

                                                   vocabulary_entries                  vocabulary_examples
                                                   ──────────────────                  ───────────────────
                                                   id PK                               id PK
                                                   term     (String, ruby markup)      entryId FK→vocabulary_entries
                                                   meaning  (String, ruby markup, "")    ON DELETE CASCADE
                                                   notes    (RichText JSON)            position
                                                   notesPlain (search text)            sentence     (RichText JSON)
                                                   languageId FK  ON DELETE RESTRICT   translation  (RichText JSON)
                                                   categoryId? FK ON DELETE SET_NULL
                                                   createdAt, updatedAt (epoch ms)

vocabulary_review_states                           vocabulary_review_logs
────────────────────────                           ──────────────────────
entryId PK, FK→vocabulary_entries CASCADE          id PK
stability, difficulty (REAL)                       entryId FK→vocabulary_entries CASCADE
due (epoch ms, indexed), lastReview (epoch ms)     rating (Rating enum name)
reps, lapses                                       reviewedAt (epoch ms)
```

- **Languages are shared** by both libraries; **categories are scoped per library** by
  `CategoryType` (`GRAMMAR`, `VOCABULARY`; stored by enum name — never rename the constants).
  A grammar "Verbs" and a vocabulary "Verbs" are different rows.
- `GrammarEntryWithDetails` / `VocabularyEntryWithDetails` are the `@Relation` aggregates
  (entry + language + category + examples) returned by all entry queries; those queries are
  `@Transaction`.
- **Review state lives in its own table**, not on `vocabulary_entries`, so saving an entry in the
  editor never touches it. A vocabulary entry **without** a review-state row is a *new* card and
  is always due. Every answer is also appended to `vocabulary_review_logs` (not read yet; kept for
  statistics and future FSRS parameter optimization). `Rating` is stored by enum name — never
  rename the constants.
- Names are unique **case-insensitively** (`COLLATE NOCASE`) — `getOrCreate*` relies on this.
- Deleting a language with entries fails (`RESTRICT`); deleting a category nulls the entries'
  `categoryId`. There is currently no UI to delete languages/categories.

### 4.2 DAOs (`data/db/`)

- `TaxonomyDao`: languages and categories; category queries take a `CategoryType`.
- `GrammarDao` / `VocabularyDao`: same shape. Observations return `Flow` (`observeEntries`,
  `observeEntry`, `observeCount`); one-shot reads/writes are `suspend`.
- `VocabularyDao` review queries: `observeDueDates()` (`COALESCE(due, 0)` per entry, so new cards
  count as due), `getDueEntries(now)` (reviewed cards by oldest due first, then new cards by
  creation), `getReviewState`, and `saveReview(state, log)` (upsert state + insert log, one
  transaction).
- `saveEntry(entry, examples)` is the single write path for entries: insert-or-update the entry,
  **delete all its examples and re-insert** them with `position = index`. Examples have no stable
  identity across saves.

### 4.3 Repositories (`data/`)

The only public API of the data layer: `GrammarRepository` and `VocabularyRepository`, which share
their structure. Responsibilities:

- Both implement `Taxonomy` (`data/Taxonomy.kt`: `languages`, `categories`,
  `getOrCreateLanguage`, `getOrCreateCategory`) by delegating to `RoomTaxonomy` with their
  `CategoryType`, so `categories` and `getOrCreateCategory` only see their own library.
- Map entities ⇄ domain models via private `toModel()` extensions (`Language`/`Category` mappers
  are `internal` in `Taxonomy.kt`).
- Encode/decode `RichText` columns (`RichText.encode()` / `RichText.decode()`).
- `save(draft)`: trims title/term (and meaning), preserves `createdAt` on update, sets
  `updatedAt = clock()`, computes the `*Plain` search column, and **drops examples where both
  sentence and translation are blank**.
- `entryCount: Flow<Int>` for the home screen.
- Vocabulary repetition (`VocabularyRepository`): `dueCount: Flow<Int>` (combined with a
  one-minute ticker, because cards become due by time passing, not by DB changes),
  `getDueEntries()` and `review(entryId, rating)`, which loads the card's state, runs
  `Fsrs.schedule` and saves state + log. `zone` and `fsrs` are injectable like `clock`.
- `getOrCreateLanguage(name)` / `getOrCreateCategory(languageId, name)`: trimmed, case-insensitive
  reuse.
- `clock` is injectable for tests.

### 4.4 Domain models (`data/model/Grammar.kt`)

`Language`, `Category` (has `languageId`), `ExampleSentence(sentence: RichText, translation: RichText)`,
`GrammarEntry` (read model, with resolved `language`/`category`), `GrammarEntryDraft` (write model,
ids only; `id = 0` means new). `data/model/Vocabulary.kt`: `VocabularyEntry` / `VocabularyEntryDraft`
with the same conventions (`meaning` is `""` when not set).

### 4.5 Migrations

`room { schemaDirectory("$projectDir/schemas") }` exports every schema version. When changing any
entity:

1. Bump `version` in `GlossaDatabase`.
2. Add a `Migration` (or `@AutoMigration` in `@Database(autoMigrations = …)`) — there is no
   `fallbackToDestructiveMigration`, so a missing migration crashes on upgrade.
3. Build once so `app/schemas/de.miaowz.glossa.data.db.GlossaDatabase/<version>.json` is
   generated, and commit it.

History: `1 → 2` is an `@AutoMigration` (new vocabulary tables; `categories` gets `type` with
default `'GRAMMAR'` and a new unique index, which Room does by rebuilding the table). Room enables
`PRAGMA foreign_keys` only in `onOpen`, after migrations, so table rebuilds don't fire
`ON DELETE` actions. `2 → 3` is an `@AutoMigration` adding the two vocabulary review tables
(existing vocabulary becomes new cards).

### 4.6 Spaced repetition (`srs/`)

| File | Role |
|------|------|
| `Fsrs.kt` | `Rating` (Again/Hard/Good/Easy), `MemoryState(stability, difficulty)`, and `Fsrs`: FSRS-5 with its 19 default parameters, `desiredRetention = 0.9`, `maximumInterval = 36500` days. `initialState`, `nextState(state, elapsedDays, rating)`, `retrievability`, `nextInterval`. |
| `Scheduler.kt` | `CardState` (stability, difficulty, due, lastReview, reps, lapses) and `Fsrs.schedule(previous, rating, now, zone)`. |

- Formulas follow the FSRS-5 reference (linear-damped difficulty with mean reversion to
  D₀(Easy), short-term stability for same-day reviews, post-lapse stability capped by the current
  one). Stability is in days; at 90 % retention the interval equals the stability.
- **Days are calendar days in the device's time zone:** `elapsedDays` is the number of local date
  changes since the last review (0 ⇒ same-day/short-term formula), and a card becomes due at the
  **start of the local day** its interval ends on. So a card answered Good at 23:00 for a 3-day
  interval is due from midnight three days later.
- Intervals are whole days ≥ 1: there are no minute-level learning steps. A card failed today is
  due tomorrow; within a session, the session itself repeats it once (§8.3).
- `lapses` counts Again on already-reviewed cards; the first answer on a new card is not a lapse.
- The review UI only uses `Again` (✗) and `Good` (✓); `Hard`/`Easy` are implemented for
  completeness.

---

## 5. Persistent text formats (stable contracts)

These two formats are stored in the database. Changing them requires a migration or a
backwards-compatible decoder.

### 5.1 Ruby markup — `ruby/RubyMarkup.kt`

```
{base|reading}        e.g.  {漢字|かんじ}を{書|か}く
```

- Neither part may contain `{`, `}` or `|`, and neither may be blank
  (`RubyMarkup.isValidPart`). Malformed markup is rendered as literal text.
- Furigana is **inline text, not a span**: it survives copy/paste, plain-text fields and search.
- Used in: grammar titles, vocabulary terms and meanings (plain `String`), and inside the `text` of
  every `RichText`.
- `stripToBase(text)` removes readings — use it for sorting, search and plain-text previews.

### 5.2 RichText — `richtext/RichText.kt`

```kotlin
RichText(text: String, spans: List<RichSpan(start, end, format: SpanFormat)>)
```

Serialized as JSON via kotlinx.serialization (not HTML — Android's `Html` round-trip is lossy).
Example:

```json
{"text":"{雨|あめ}が降る","spans":[{"start":0,"end":6,"format":{"type":"bold"}}]}
```

- `SpanFormat` is a sealed hierarchy with `@SerialName`s: `bold`, `italic`, `underline`,
  `strikethrough`, `color(argb)`, `highlight(argb)`, `size(proportion)`. **Never rename a
  `@SerialName`** — it is the stored discriminator.
- Span offsets index into `text` **including ruby markup characters**.
- `decode()` is forgiving: non-JSON → plain text; unknown keys ignored; out-of-range or empty
  spans dropped.
- `FormatKind` is the "slot" of a format: at most one format per kind per character
  (e.g. one text color).

---

## 6. Rich text editor (`richtext/`)

A Compose-facing API wrapping a classic `EditText` (Compose text fields can't host custom
`Spannable` editing well enough).

| File | Role |
|------|------|
| `RichText.kt` | Model + JSON codec + `SpanFormat`/`FormatKind`. |
| `RichTextSpans.kt` | Android span classes, each implementing marker interface `RichFormatSpan`. `RichText.toSpannable()` / `Spanned.toRichText()`. |
| `SpanOps.kt` | Pure span editing on a `Spannable`: `apply`, `remove`, `clear`, `formatsIn`, `formatsAt`. |
| `RichTextEditText.kt` | The `EditText` subclass: typing formats, programmatic edits, change notifications, `insertRuby`. |
| `RichTextEditor.kt` | `RichTextEditorState` (handle) + `RichTextField` composable (`AndroidView`). |
| `RichTextToolbar.kt` | Toolbar composable operating on a `RichTextEditorState`; color/highlight palettes, size options. |

Key invariants:

- **Only `RichFormatSpan`s are persisted.** IME composing spans, spell-check spans, etc. are
  ignored by `toRichText()`. Paste is forced to plain text.
- **Per `FormatKind`, spans never overlap**, and adjacent equal spans are merged (`SpanOps.apply`).
  Any new editing operation must go through `SpanOps` to preserve this.
- **Typing formats:** with a collapsed cursor, toolbar actions change `typingFormats`; the
  `FormattingWatcher` re-applies them to newly typed text. Typing formats are reset from the
  preceding character on every selection change (`SpanOps.formatsAt`).
- Wrap programmatic text mutations in `editProgrammatically { }` so the watcher ignores them, then
  call `notifyChanged()`.
- `setRichText(value)` is a no-op when `value == lastRichText` — this is what keeps the cursor and
  IME state stable across Compose recompositions. In `RichTextField.update`, only set properties
  that actually changed (e.g. `setHint` relayouts and kills the selection action mode).
- Data flow: `EditText` → `onRichTextChange(RichText)` → ViewModel → `value` → `setRichText`
  (no-op on echo).

Multiple fields + one toolbar: each `RichTextField` gets its own `RichTextEditorState`. The screen
tracks them in a `SnapshotStateList` (`rememberRegisteredEditor` in `ui/common/Examples.kt`) and
shows `RichTextToolbar` for the one with `isFocused`.

---

## 7. Ruby rendering (`ruby/`)

| File | Role |
|------|------|
| `RubyMarkup.kt` | Parse/format/validate/strip `{base|reading}` (pure Kotlin, unit-tested). |
| `RubySpan.kt` | `ReplacementSpan` drawing base text with the reading centered above; `RubySpan.applyTo(Spannable)` converts all markup. |
| `RubyTextView.kt` | Read-only `TextView`: `setRichText(RichText)` → `toSpannable()` + `RubySpan.applyTo`. |
| `RubyText.kt` | Compose wrappers `RubyText(RichText)` and `RubyText(String)`; maps `TextStyle` (size, weight, italic, color, text alignment as gravity) and `maxLines` onto the view. |
| `FuriganaDialog.kt` | Dialog asking for base + reading with live preview. |

Notes:

- `ReplacementSpan`s don't receive non-metric styles from `TextLine`, so `applyTo` cuts overlapping
  `CharacterStyle`s out of the markup range and passes the ones touching the base into the
  `RubySpan` (`baseStyles`). A style that spans across markup is split; the trailing half is
  `CharacterStyle.wrap`ped because a span object can be attached only once.
- `RubySpan.getSize` increases the line ascent by the reading height, so lines with furigana are
  taller.
- **The editor shows raw markup** (`{漢字|かんじ}`); rendering happens only in read-only views
  (library cards, detail screen, editor preview mode).
- **Any place that displays user text should use `RubyText`, not `Text`.**

---

## 8. UI layer

### 8.1 Navigation (`ui/navigation/GlossaNavHost.kt`)

| Route | Screen | Args |
|-------|--------|------|
| `HomeRoute` (start, tab) | `HomeScreen` | — |
| `ReviewRoute` | `ReviewScreen` | — (loads due vocabulary itself) |
| `GrammarLibraryRoute` (tab) | `GrammarLibraryScreen` | — |
| `GrammarDetailRoute(entryId)` | `GrammarDetailScreen` | entry id |
| `GrammarEditorRoute(entryId = 0)` | `GrammarEditorScreen` | `0` = create new |
| `VocabularyLibraryRoute` (tab) | `VocabularyLibraryScreen` | — |
| `VocabularyDetailRoute(entryId)` | `VocabularyDetailScreen` | entry id |
| `VocabularyEditorRoute(entryId = 0)` | `VocabularyEditorScreen` | `0` = create new |

- **Bottom navigation:** `GlossaNavHost` wraps the `NavHost` in an outer `Scaffold` whose
  `NavigationBar` lists `TopLevelDestination` (Home, Grammar, Vocabulary). The bar is shown only
  while a top-level route is current; detail and editor screens are full-screen.
  Tab switches use `popUpTo(start) { saveState = true }` + `launchSingleTop` + `restoreState`, so
  each tab keeps its state (scroll, filters).
- Insets: the outer `Scaffold` uses `contentWindowInsets = WindowInsets(0)` and the `NavHost` gets
  `padding(padding).consumeWindowInsets(padding)` — screens keep handling the status bar
  themselves, and their bottom inset is consumed by the bar when it is visible.
- Screens receive navigation as lambdas (`onAddEntry`, `onOpenEntry`, `onEdit`, `onBack`,
  `onSaved`); they never get the `NavController`.
- ViewModels read args with `savedStateHandle.toRoute<XRoute>()`.
- After saving a **new** entry, the editor is replaced by its detail screen
  (`popUpTo<XEditorRoute> { inclusive = true }`); after editing, it just pops.

### 8.2 Screen / ViewModel pattern

Every feature package contains `XScreen.kt` and (unless trivial) `XViewModel.kt`:

- **UI state** is one immutable `data class XUiState` (or a `sealed interface` for
  Loading/NotFound/Loaded, see `DetailUiState`), exposed as `StateFlow`.
- **Derived/read-only state** is built with `combine(...)` + `stateIn(viewModelScope,
  SharingStarted.WhileSubscribed(5_000), initial)` (library, detail, category options).
- **Editable state** is a private `MutableStateFlow` mutated via `update { }` (editor).
- **Events → navigation** are modeled as state, not channels: e.g. `EditorUiState.saved:
  SavedEntry?` or `GrammarDetailViewModel.deleted`, consumed with `LaunchedEffect`.
- Validation errors are computed properties gated by `showErrors` (set on first failed save).
- Screen-local, transient UI state (dialogs open, preview toggle) lives in `rememberSaveable` in the
  composable, not the ViewModel.
- Private sub-composables live in the same file as the screen. Pieces used by more than one
  feature live in `ui/common/`:
  - `Library.kt` — `LibraryFilter`, generic `LibraryUiState<T>`, `LibraryKeys<T>` and the pure
    `libraryState(entries, filter, keys)` that filters, searches and groups (JVM-tested).
  - `LibraryList.kt` — generic `LibraryList<T>` body (loading, empty, search, chips, language
    sections) and `EmptyLibrary`.
  - `CategoryPickers.kt` — `CategoryOptions` + language/category pickers with inline creation.
  - `Examples.kt` — `ExampleDraft`, `SavedEntry`, `rememberRegisteredEditor`, `ExampleEditor`,
    read-only `ExamplesSection`, `TextFieldValue.replaceSelection` / `selectedText`.

### 8.3 Screens

**Home** (`ui/home/HomeScreen.kt`, `HomeViewModel` in the same file) — a repetition card with the
number of due vocabulary cards (`dueCount`) and a "Start repetition" button (disabled when nothing is
due), plus the number of vocabulary and grammar entries (`entryCount`). Counts are `null` while
loading.

**Review** (`ui/review/`) — a vocabulary repetition session:
- `ReviewSession<T>` (pure, JVM-tested) is the queue: a card answered wrong is appended to the
  end **once** (`ReviewCard.isRetry`); a wrong second attempt is not re-queued. It counts
  correct/incorrect **answers** (a card wrong then right counts once on each side).
- `ReviewViewModel` loads `getDueEntries()` once (a snapshot; cards that become due during the
  session wait for the next one), exposes `ReviewUiState` (Loading / NothingDue / Reviewing /
  Finished) and measures the duration with `TimeSource.Monotonic`. Every answer is written
  immediately via `repository.review(id, Good|Again)`, serialized with a `Mutex` — leaving early
  keeps the progress. The retry is a real (same-day) FSRS review too.
- `ReviewScreen`: a flashcard flipped by tapping (`graphicsLayer.rotationY`, the back face is
  counter-rotated). The front shows the term **without furigana** (`stripToBase`), the back the
  term with furigana, meaning and notes. The flashcard is wrapped in `key(id, isRetry)` so the
  flip state/animation resets per card and never reveals the next card's back. ✗ / ✓ buttons are
  available before flipping too. The summary shows time spent, correct, incorrect and percentage,
  with a praise/encouragement message picked by percentage (`feedbackFor`).

**Libraries** (`ui/grammar/GrammarLibraryScreen.kt`, `ui/vocabulary/VocabularyLibraryScreen.kt`) —
each ViewModel combines `repository.entries` with a `LibraryFilter(languageId, categoryId, query)`
via `libraryState` and a feature-specific `LibraryKeys` (what is the title, which texts are
searched). All filtering, grouping (sections per language, sorted case-insensitively; entries by
category then stripped title) and search happen **in memory**. Search matches raw text (so readings
match) and `stripToBase` text. A language filter whose last entry disappeared is dropped
automatically. FAB → editor. Only the card composable differs per library.

**Detail** (`GrammarDetailScreen.kt` / `VocabularyDetailScreen.kt`, ViewModel in the same file) —
observes one entry; edit and delete (with confirmation) actions.

**Editors** (`GrammarEditorScreen` + `GrammarEditorViewModel`, `VocabularyEditorScreen` +
`VocabularyEditorViewModel`) — same structure:
- Title (grammar) / term and meaning (vocabulary) are `TextFieldValue`s (needed for the furigana
  button); selection-only changes do not mark the state dirty. Each has a ふ button that opens
  `FuriganaDialog` for that field (`FuriganaTarget`).
- Examples are `ExampleDraft(key, sentence, translation)`; `key` is a ViewModel-local counter used
  with Compose `key()` so editor states follow the right example on add/remove.
- New entries default to the language of the most recently updated entry of the same library.
- Changing language clears the category. `CategoryPickers` supports inline creation of languages
  and categories (`getOrCreate*`, scoped to the library).
- `isDirty` drives the discard-confirmation dialog (`BackHandler` + close button).
- Preview mode renders the draft with `RubyText`.
- Required: title/term and language.

### 8.4 Theme & strings

- `ui/theme/` is still the Android Studio template (purple scheme, dynamic color). `Build.VERSION`
  check in `Theme.kt` is always true given `minSdk 35`.
- **User-facing strings are currently hard-coded in Compose**, not in `res/values/strings.xml`.
  Follow the existing convention unless a localization pass is planned; if so, migrate whole
  screens at once.

---

## 9. Testing

| Kind | Location | Covers |
|------|----------|--------|
| JVM unit | `app/src/test/…/richtext/RichTextTest.kt` | JSON round-trip, decode fallbacks |
| JVM unit | `app/src/test/…/ruby/RubyMarkupTest.kt` | parsing, malformed markup, strip, validation |
| JVM unit | `app/src/test/…/ui/common/LibraryStateTest.kt` | library grouping, sorting, filters, search |
| JVM unit | `app/src/test/…/srs/FsrsTest.kt` | FSRS-5 initial state, retrievability, intervals, stability/difficulty updates |
| JVM unit | `app/src/test/…/srs/SchedulerTest.kt` | due dates at start of local day, lapses, same-day and calendar-day handling |
| JVM unit | `app/src/test/…/ui/review/ReviewSessionTest.kt` | session queue (retry once), counts, percentage |
| Instrumented | `app/src/androidTest/…/richtext/SpanOpsTest.kt` | span merge/split/coverage (needs Android `Spannable`) |
| Instrumented | `app/src/androidTest/…/ruby/RubySpanTest.kt` | markup → spans, style splitting, metrics |
| Instrumented | `app/src/androidTest/…/data/GrammarRepositoryTest.kt` | in-memory Room: get-or-create, save/update with examples |
| Instrumented | `app/src/androidTest/…/data/VocabularyRepositoryTest.kt` | save/update/count, shared languages vs. scoped categories, due count/order, review rescheduling |

Commands (from the repo root):

```bash
./gradlew :app:testDebugUnitTest          # JVM tests
./gradlew :app:connectedDebugAndroidTest  # device/emulator tests
./gradlew :app:assembleDebug
```

Guidelines: pure Kotlin logic (parsing, filtering, mapping) → JVM tests; anything using
`android.text.*` or Room → `androidTest`. Repository tests use
`Room.inMemoryDatabaseBuilder` and the injectable `clock`. There are no ViewModel, Compose UI or
migration tests yet (a `MigrationTestHelper` test would need the `room-testing` dependency and the
schemas as androidTest assets).

---

## 10. Recipes

### Add a new screen / feature
1. Create `ui/<feature>/<Feature>Screen.kt` and `<Feature>ViewModel.kt` following §8.2.
2. Add a `@Serializable` route (data object or data class) in `GlossaNavHost.kt` and a
   `composable<Route> { … }` entry; pass navigation as lambdas. For a new bottom-bar tab, add a
   `TopLevelDestination` entry as well.
3. Construct the ViewModel with `viewModel { XViewModel(createSavedStateHandle(), grammarRepository) }`.
4. Render user text with `RubyText`; edit formatted text with `RichTextField` + `RichTextToolbar`.

### Add a new persisted field to grammar (or vocabulary) entries
1. Add the column to `GrammarEntryEntity`; bump DB version + migration (§4.5).
2. Add the field to `GrammarEntry` and `GrammarEntryDraft`; map it in `GrammarRepository`
   (`toModel()` and `save()`).
3. Add it to `EditorUiState`, load it in `GrammarEditorViewModel.load()`, add a setter that goes
   through `edit { }` (so `isDirty` updates), and pass it in `save()`.
4. Show it in the editor, `EntryPreview`, the detail screen and, if relevant, `EntryCard`.
5. Extend `GrammarRepositoryTest`.
   Formatted fields are stored as `RichText.encode()` strings — never as raw `Spannable`/HTML.

### Add a new rich-text format (e.g. monospace, superscript)
1. Add a `SpanFormat` subtype with a **new, unique** `@SerialName` and a `FormatKind` entry; extend
   the `kind` `when`.
2. Add a private span class implementing `RichFormatSpan` in `RichTextSpans.kt` and a branch in
   `createSpan()`.
3. If the format affects rendering under furigana, check `RubySpan.basePaint` handles it
   (metric-affecting spans are applied via `updateMeasureState`).
4. Add a toolbar button (`ToggleFormatButton` for on/off, or a `Panel` for value formats).
5. Extend `RichTextTest` round-trip and `SpanOpsTest`.
   Old data stays valid; old app versions would fail to decode the new discriminator and fall back
   to plain text, which is acceptable.

### Add another entity type (e.g. kanji, phrases)
Mirror the vocabulary stack, which was built this way: entities + DAO in `data/db/` (add to
`@Database(entities)`, bump version), domain models in `data/model/`, a `XRepository` in `data/`
implementing `Taxonomy by RoomTaxonomy(taxonomyDao, CategoryType.X)` (add the `CategoryType`
constant), a `lazy` instance in `GlossaApplication`, a `CreationExtras` accessor, then UI per the
screen recipe using `ui/common` (`libraryState` + `LibraryKeys`, `LibraryList`, `CategoryPickers`,
`ExampleEditor`/`ExamplesSection`). Add its count to `HomeScreen` if relevant.

### Use furigana in a plain `String` field
Store the string with markup, render with `RubyText(text: String)`, offer `FuriganaDialog` and
insert `RubyMarkup.format(base, reading)` at the selection (see the title field handling in
`GrammarEditorScreen.kt` and `TextFieldValue.replaceSelection` in `ui/common/Examples.kt`). Use `RubyMarkup.stripToBase` for
sorting and search.

---

## 11. Known gaps & notes

- `descriptionPlain` / `notesPlain` are written but not read: search currently runs in memory over all entries.
  If the library grows large, move search into SQL (`LIKE` on `title`/`descriptionPlain`, or FTS4)
  — the column already exists for that purpose. Example sentences are not searched.
- No UI for renaming/deleting languages or categories; orphaned categories persist. Languages
  created from either library show up in both.
- `rememberRichTextEditorState()` and `RubyTextView.setMarkupText()` are currently unused public
  helpers.
- `RichTextToolbar`'s color palettes store absolute ARGB values; they're mid-tones chosen to read
  on both light and dark themes.
- Theme is the template default; strings are not externalized (see §8.4).
- Spaced repetition: no daily limit for new cards (all new vocabulary is due at once), FSRS
  parameters are the defaults (not optimized from `vocabulary_review_logs`), and the session is not
  restored after process death (answers already given are saved). Grammar has no repetition yet;
  the review UI and `ReviewSession` are generic enough to reuse.
- `app/LOGBOOK.MD` is the AI-usage logbook (prompt + summary of changes per session). Append an
  entry there when making AI-assisted changes.
