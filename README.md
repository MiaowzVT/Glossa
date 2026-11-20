# Glossa - Many Languages, One Companion

**Glossa** is an Android App aiming to support users on their journey to learn new languages.

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
4. Run the app in the emulator or create a clean build and install the APK to your device

### Creating sample content

If you wish to create sample content for the *Official Courses* feature, you can access an Admin Panel under `ip:port/admin`, e.g. `localhost:8096/admin`.

The panel allows you to create official courses as well as lectures for these courses.

## Database Schema

The Database Schema used by the backend can be found in [Schema](./glossa_database_schema.md)