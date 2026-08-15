# Humraahi

Humraahi is an Android group-travel companion where friends plan trips together through
real-time chat. The long-term goal is to let AI understand the conversation and build a
shared itinerary automatically.

## Current features

- Google sign-in with Firebase Authentication
- Trip list and trip creation flow
- Navigation with trip deep links
- Real-time group chat backed by Cloud Firestore
- Chat and itinerary tab layout
- Shareable trip invitations
- Locally persisted profile name with DataStore

The itinerary screen is currently a placeholder. AI itinerary generation, offline chat
caching, and push notifications are planned next.

## Tech stack

- Kotlin
- Jetpack Compose and Material 3
- MVVM with ViewModel, StateFlow, and sealed UI states
- Navigation Compose
- Kotlin coroutines and Flow
- Firebase Authentication and Cloud Firestore
- Preferences DataStore

## Local setup

1. Open the project in Android Studio.
2. Add your Firebase Android configuration at `app/google-services.json`.
3. Create `secrets.properties` in the project root:

   ```properties
   GOOGLE_WEB_CLIENT_ID=your-web-client-id.apps.googleusercontent.com
   ```

4. Build the app:

   ```bash
   ./gradlew :app:assembleDebug
   ```

See [FIREBASE_AUTH_SETUP.md](FIREBASE_AUTH_SETUP.md) for Firebase configuration details.
