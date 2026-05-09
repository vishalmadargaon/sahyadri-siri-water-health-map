# Sahyadri-Siri Android App

Native Android implementation of the Sahyadri-Siri water health mapper.

## What is included

- Local email/password auth with session persistence.
- Free map provider using OpenStreetMap through osmdroid, not Google Maps.
- Map markers for community water quality reports.
- Search on the map by place, status, or report summary.
- Bottom navigation: Map, Reports, Community, Profile.
- Image upload from device gallery.
- Gemini-powered water image analysis with AQI label, status, summary, and suggested action.
- Local report persistence, including copied report photos.
- Profile screen for saving a Gemini API key on device.

## Open in Android Studio

1. Open this folder in Android Studio:
   `C:\Users\user\Documents\Codex\2026-05-09\files-mentioned-by-the-user-code`
2. Let Gradle sync.
3. Add your Gemini key either:
   - In `gradle.properties` as `GEMINI_API_KEY=your_key`, then rebuild.
   - Inside the app on `Profile > Gemini API key`.
4. Run on an emulator or Android phone.

## Notes

- Auth and reports are local-device storage so the whole app works immediately without a backend.
- When you are ready for production, replace `LocalRepository` with Firebase Auth/Firestore, Supabase, or your own API.
- The map uses OpenStreetMap tiles. Respect OSM tile usage policies for production scale; for many users, use a hosted OSM-compatible tile provider.
