# Glossa - Many Languages, One Companion

**Glossa** is an Android App aiming to support users on their journey to learn new languages.

## Project Architecture

| Frontend		 |																		  |
|----------------|------------------------------------------------------------------------|
| Language       | Kotlin 2.2, JVM target 11                                              |
| UI             | Jetpack Compose (Material 3, BOM `2026.02.01`), single Activity        |
| Persistence    | Room 2.8 (KSP), schema exported to `app/schemas/`                      |
| Serialization  | kotlinx.serialization JSON 											  |
| Networking     | Ktor client 3.6 (OkHttp engine), bodies decoded as text (§4.6)         |
| SDK            | `minSdk 35`, `targetSdk/compileSdk 37`                                 |
|----------------|------------------------------------------------------------------------|
| Backend		 |																		  |
|----------------|------------------------------------------------------------------------|
| Language		 | JavaScript (node.js)													  |
| Database		 | MariaDB, `knex` for handling migrations								  |

## Features

- Grammar Library: Add and manage grammar entries; add a description and example sentences. Can be filtered by language and category.
- Vocabulary Library: Add and manage vocabulary; add a translation, notes and example sentences. Can be filtered by language and category.
- Spaced Repitition: Revise vocabulary frequently using the Spaced Repition Algorithm. Flashcard-style; see a vocab, translate it in your head and mark your answer as correct or incorrect.
- Official Sets: Using the backend, the app can fetch official courses. These work like Grammar Entries and aim to explain concepts from an official point of view, based on curated contents from the team. Entries are automatically fetched from the server and cached locally in the device's database, so they're available offline. Next time the user goes online, the data will be synced with the server's.

## Setup

1. Clone the repository: `git clone https://github.com/MiaowzVT/Glossa.git`
2. Setup both the Frontend and the Backend

### Backend

`node.js` is required for the backend. You will also need an SQL Database.

1. Open `Glossa/backend`
2. Configure your database's credentials in `.env`
3. Run `npm install` to install all dependencies
4. Run `npm run migrate` to setup the database.
5. Start the server: `npm run start`

The server listens on port `8096` by default. Make sure the port is free, as it may be in use by a different application already (e.g. Jellyfin).
If you wish to expose the server on a different port, change line 13 in `src/index.js`: `const port = 8096;`

### Frontend

1. Open Android Studio
2. File -> Open and select the cloned project
3. Let Android Studio configure the project
4. Set `BASE_URL` in `de.miaowz.glossa.network.HttpClient` to your machine's IP. Make sure the device running the app is in the same network as the server. The default `http://10.0.0.2:8096/` is the default IP for `localhost` on the Device Simulator.
5. Run the app in the emulator or create a clean build and install the APK to your device

### Creating sample content

If you wish to create sample content for the *Official Courses* feature, you can access an Admin Panel under `ip:port/admin`, e.g. `localhost:8096/admin`.

The panel allows you to create official courses as well as lectures for these courses.

## Database Schema

The Database Schema used by the backend can be found in [Schema](./glossa_database_schema.md)

## Known Limitations and deliberate decisions

- The "Community Sets" feature is not yet functional. This is planned for a potential Version 2, as the time was not enough to get it into Version 1.
- The admin panel (`/admin`) is unprotected. This is a deliberate choice, as it is currently mostly used as a little helper to easily create test courses and lectures. There is no authentication attached; That would be a necessity for a production build, but not for a test build.