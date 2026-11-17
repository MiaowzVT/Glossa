# AI Usage - Logbook

## October 30th, 2026 (Commit: b4ac917)

### First Prompt

**Prompt**: <br>
The workspace you're in right now is the scaffolding for an Android App. Our goal is to create a full Android App out of this scaffolding. For this, we first want to implement one of    
many features: A "Grammar Library" - The user should be able to create new Grammar Entries for their Library by pressing a "+" button in the application's view. That will send them to   
another view, where they can create a short title, a detailed description and example sentences. For the implementation, we will also need support for RubyTextView, so the user can use  
for example Furigana when creating the Grammar Entries. I also want a Rich Text Editor for this view, so that the user can work with Colors, Text Edits (e.g. Bold, Italic, Underline)    
etc.. Finally, each grammar entry should be able to be categorized by a Language and a Sub-Category inside the Language.

**Changes**: <br>

Claude built a basic Grammar Library as expected. For that, the AI split everything into four parts: a `data` package, housing everything Database-related, the models for Grammar entries (`Language`, `Category`, `ExampleSentence`, `GrammarEntry` and `GrammarEntryDraft`), and the full `GrammarRepository`, which is responsible for any changes to Grammar entries.
For the editor, Claude created a `richtext` and a `ruby` package. The former contains everything related to the RichTextEditor, which is used when creating Grammar Entries for formatting (coloring, text formats, etc.), and `ruby` is responsible for the implementation of `RubyTextView`, which is required to render additional text above a user's text input (this achieves the ability to add e.g. Furigana or Pinyin to a user's text). The entire UI is its own package, living in `ui`, with everything split into per-feature subpackages (detail -> `GrammarDetailScreen`, used for rendering the actual Grammar Entry, `editor` -> used for rendering the Entry Editor, `library` -> used for rendering the Grammar Library Main Screen, `navigation` -> holds the NavHost). The single activity `MainActivity` is only responsible for acccessing the NavHost upon the app's startup.

**Reflection**:

The current architecture looks promising and currently still maintainable. Claude split the project into meaningful subpackages on a per-feature level, which seemingly should lead to a good level of reusability of features for the future.
Claude decided for a local database, which is a fine decision. The end goal is to have full offline compatibility, after all. No server should ever be required, so any changes should be synced to the local database fetched from a backend in the future.

### Second Prompt

<code>Note: This prompt happened in a new Claude Code Session. For some reason, Anthropic doesn't like it when you ask Claude to dump both Prompt and Answers from Claude into a text file, as that will trigger their safeguards. That eventually led to all my prompts being blocked, requiring me to create a new Session.</code>

**Prompt**: <br>
"The current workspace is an Android App. Write an Architecture Document in a style that you can work
with for additions on the code bae."

**Changes**: <br>

Claude added an `ARCHITECTURE.md` in the project's root directory. It describes Tech Stack, Layering, App wiring, the Data layer, Persistent Text Formats (Ruby Markup and RichText), the way the two are implemented, the UI Layer, testing methods, recipes for adding new things as well as Known limitations.

**Reflection**:

A small, yet clear overview of the app's architecture. It also leaves notes on everything, giving concise information about limitations and noteworthy quirks.

### Third Prompt

Based on Claude's question "At the moment I'd only see this file in a later session if I happened to open it. If you'd like it picked up automatically, I can add a short CLAUDE.md that points to it." after finishing the Architecture Document

**Prompt**: <br>
Yes, add a CLAUDE.md

**Changes** <br>
Added a `CLAUDE.md` in the project root, describing the rules for AI Agents to work on the project.

**Reflection**:

The rules seem to be enough for now. They aim to keep the architecture consistent.

## November 17th, 2026

### First Prompt (Commit: 0d16e40)

**Prompt**: <br>
Continue work on this project. Our next goal is to add a Vocabulary Library next to the Grammar Library. The goal is the following: There should be a navbar at the bottom, allowing the user to switch between three different views: A Main Screen, showing nothing except the amount of Vocabulary and Grammar entries for now, a Grammar Library view, showing the current Grammar Library, and a Vocabulary view, acting pretty much the same as the Grammar Library, but for Vocabulary.

**Changes**: <br>

- **Navigation:** `GlossaNavHost` now wraps the `NavHost` in a `Scaffold` with a Material 3 `NavigationBar` (Home, Grammar, Vocabulary). The bar is only visible on the three top-level screens; detail and editor screens stay full-screen. Each tab keeps its state when switching. Routes were renamed per library (`GrammarLibraryRoute`, `GrammarDetailRoute`, `GrammarEditorRoute`, `VocabularyLibraryRoute`, …); `HomeRoute` is the new start destination.
- **Home screen** (`ui/home/HomeScreen.kt`): shows the number of vocabulary and grammar entries, backed by new `entryCount` flows on both repositories.
- **Vocabulary data layer:** new `vocabulary_entries` / `vocabulary_examples` tables, `VocabularyDao`, `VocabularyEntry` / `VocabularyEntryDraft` models and `VocabularyRepository`. A vocabulary entry has a term (with furigana), a meaning (with furigana, optional), rich-text notes, language, category and example sentences.
- **Shared languages, separate categories:** languages are shared between both libraries, while categories are now scoped per library via a new `type` column (`CategoryType.GRAMMAR` / `VOCABULARY`). Language/category access moved from `GrammarDao` into a new `TaxonomyDao`, and both repositories implement a shared `Taxonomy` interface.
- **Database version 2:** `@AutoMigration(1 → 2)`; existing categories become grammar categories. Schema `app/schemas/.../2.json` generated.
- **UI restructuring:** grammar screens moved from `ui/library`, `ui/detail`, `ui/editor` into `ui/grammar`; the vocabulary screens (library, detail, editor) live in `ui/vocabulary`. Code used by both libraries was extracted into `ui/common` (library filtering/grouping logic, the library list with search and filter chips, category pickers, example sentence editor/display), so both libraries behave the same.
- **Tests:** new JVM test `LibraryStateTest` for the extracted filter/search/grouping logic, new instrumented `VocabularyRepositoryTest`, `GrammarRepositoryTest` adapted.
- `ARCHITECTURE.md` and `CLAUDE.md` updated accordingly.

**Reflection**:

- The updated NavHost works as expected; the state is being kept when switching, so e.g. chosen Categories or the Search are kept.
- Home Screen is a placeholder for now, so the current implementation is fine.
- Vocabulary Data Layer follows the same principles as the Grammar Data Layer; we're staying consistent that way
- UI was restructured in a readable and usable way: Per-feature subpackages make it easy to work with

### Second Prompt

**Prompt**: <br>
The next step is to implement Spaced Repitition: Implement FSRS for vocabulary. The user should see on the Home screen how many vocabulary cards are open for repitition. When clicking "Start Repitition, a new view opens: A card shows the vocab, and by clicking on it, the user can flip it to see the meaning. There's also the option to press "X" and "✓" buttons. "X" marks the card as wrongly solved, while "✓" marks it as correctly solved. Cards marked as "X" should be asked a second time, but no more than that during the same session. At the end of the session, an overview of time spent, correctly and incorrectly solved cards as well as the percentage of correct cards is shown. Additionally, based on the percentage, the user should be praised for their effort or encouraged to not give up.

**Changes**: <br>

- **FSRS algorithm** (new package `srs/`, pure Kotlin): `Fsrs` implements FSRS-5 with its default parameters and 90 % desired retention; `Fsrs.schedule` turns a review into a new `CardState` (stability, difficulty, due date, reps, lapses). Days are calendar days in the device's time zone, and cards become due at the start of the day. "✗" is graded as *Again*, "✓" as *Good*.
- **Database version 3:** new tables `vocabulary_review_states` (FSRS state per vocabulary card; no row = new card, always due) and `vocabulary_review_logs` (every answer, for statistics and a possible future FSRS parameter optimization). `@AutoMigration(2 → 3)`, schema `3.json` generated.
- **Repository:** `VocabularyRepository` got `dueCount` (refreshes every minute, since cards become due by time passing), `getDueEntries()` and `review(entryId, rating)`.
- **Home screen:** new "Repetition" card showing the number of due vocabulary cards and a "Start repetition" button (disabled when nothing is due).
- **Review screen** (new package `ui/review/`): flashcard that flips with an animation on tap. The front shows the word without furigana, the back the word with furigana, its meaning and notes. "✗ Wrong" / "✓ Correct" buttons. Wrong cards are asked once more at the end of the session, but not a third time. Each answer is saved immediately, so leaving the session early keeps the progress. The end-of-session summary shows time spent, correct/incorrect answers and the percentage, with a praise or encouragement message depending on the percentage.
- `RubyText` now also respects `TextAlign` (needed to center text on the flashcard).
- **Tests:** new JVM tests `FsrsTest`, `SchedulerTest`, `ReviewSessionTest`; `VocabularyRepositoryTest` extended with due counts, ordering and rescheduling.
- `ARCHITECTURE.md` (new §4.6 Spaced repetition, schema, screens, tests, known gaps) and `CLAUDE.md` updated.

**Reflection**:

- FSRS lies in its own package. The implementation looks clean.
- Reviews are their own UI package. The code also looks implemented well and maintainable. It works as expected.
- One manual change: `ReviewSession.kt:41`: Claude implemented the calculation of the correct percentage of cards using the Java `Math.round()` method. To keep it as clean Kotlin code, it was replaced with the built-in Kotlin function `roundToInt()`:
  `Math.round(correct * 100f / (correct + incorrect)` -> `(correct * 100f / (correct + incorrect)).roundToInt()`
