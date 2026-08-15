# Firebase Authentication Implementation & Setup

## 🔧 Firebase Console & Google Cloud Setup

### Step 1: Enable Google Sign-In in Firebase Console

**Path:**
```
https://console.firebase.google.com/
  → Select "Humraahi" project (top)
  → Authentication (left sidebar)
  → Sign-in method (tab)
  → Click "Google"
  → Toggle Enable ON
  → Click Save
```

---

### Step 2: Get Web Client ID

**Path:**
```
https://console.cloud.google.com/
  → Select "Humraahi" project (top dropdown)
  → APIs & Services (left sidebar)
  → Credentials
```

**Find Web Client:**
- Look for "OAuth 2.0 Client IDs"
- Find entry labeled "Web client" or "Web application"
- Click it
- Copy the "Client ID" field

**Format looks like:**
```
123456789-abcdefghij1234567890klmno.apps.googleusercontent.com
```

**If Web Client doesn't exist:**
1. Click "+ Create Credentials"
2. Select "OAuth client ID"
3. Application type → "Web application"
4. Name → "Humraahi Web Client"
5. Authorized JavaScript origins: Leave default
6. Click "Create"
7. Copy the Client ID from popup

---

### Step 3: Configure OAuth Consent Screen

**YOU ARE HERE - THIS IS THE ISSUE**

You're seeing **Metrics/Analytics** page, not the configuration page.

**Go to this URL directly:**
```
https://console.cloud.google.com/apis/credentials/consent
```

Copy/paste this entire URL into your browser.

**When you're on the right page, you'll see:**
- A form to configure the consent screen
- OR a button "CREATE CONSENT SCREEN" or "CONFIGURE CONSENT SCREEN"

**Fill in:**
```
User Type: External (select)
Click: CREATE

Then fill form:
App name: Humraahi ✈️
User support email: your-email@gmail.com
App logo: (skip, leave empty)
Developer contact email: your-email@gmail.com

Click: SAVE AND CONTINUE
```

**Add Test Users:**
- Look for "Test users" section
- Click "+ ADD USERS"
- Enter your Gmail address
- Click "Add"

**Status should show:**
```
Publishing status: Development ✓
Test users: your-email@gmail.com ✓
```

---

### Step 4: Add the Firebase configuration locally

Download `google-services.json` from Firebase Console and place it at:

```text
app/google-services.json
```

Create a root-level `secrets.properties` file:

```properties
GOOGLE_WEB_CLIENT_ID=your-web-client-id.apps.googleusercontent.com
```

Both files are intentionally excluded from Git. Gradle exposes the local client ID as a
generated string resource used by the login screen.
---

### Step 5: Test

```bash
cd /Users/gayathripittala/AndroidStudioProjects/Humraahi
./gradlew clean build
```

Run app → LoginScreen → Tap "Sign in with Google" → Should work!

---

## 📝 Code Overview

### 1. AuthRepository.kt
```kotlin
class AuthRepository {
    private val auth: FirebaseAuth = Firebase.auth
    private val _authState = MutableStateFlow<AuthState>(...)
    
    suspend fun signInWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).await()
        // Update authState to Authenticated
    }
    
    suspend fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }
}
```

**What it does:**
- Exchanges Google token for Firebase credential
- Signs user into Firebase
- Updates auth state

### 2. AuthViewModel.kt
```kotlin
class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    val authState: StateFlow<AuthState> = repository.authState.stateIn(...)
    
    fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            repository.signInWithGoogle(account)
        }
    }
}
```

**What it does:**
- Provides auth state to UI
- Calls repository methods

### 3. LoginScreen.kt
```kotlin
@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel) {
    val authState by viewModel.authState.collectAsState()
    
    // Auto-navigate on success
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            }
        }
    }
    
    // Show Google Sign-In button
    GoogleSignInButton(onClick = {
        googleSignInLauncher.launch(client.signInIntent)
    })
}
```

**What it does:**
- Shows login UI
- Launches Google Sign-In dialog
- Auto-navigates to Home on success

### 4. AppNavGraph.kt
```kotlin
@Composable
fun AppNavGraph(navController: NavHostController) {
    val authState by authViewModel.authState.collectAsState()
    
    val startDestination = when (authState) {
        is AuthState.Authenticated -> Routes.HOME
        else -> Routes.LOGIN
    }
    
    NavHost(navController, startDestination) {
        composable(Routes.LOGIN) { LoginScreen(...) }
        composable(Routes.HOME) { HomeScreen(...) }
        // ... other routes
    }
}
```

**What it does:**
- Routes user to LOGIN if not authenticated
- Routes to HOME if authenticated

---

## 🔄 Auth Flow

```
User opens app
    ↓
AppNavGraph checks authState
    ↓
Is authenticated? NO → Show LoginScreen
    ↓
User taps "Sign in with Google"
    ↓
Google Sign-In dialog appears
    ↓
User signs in with Gmail
    ↓
AuthViewModel gets GoogleSignInAccount
    ↓
Repository exchanges token for Firebase credential
    ↓
Firebase authenticates user ✅
    ↓
authState changes to Authenticated(user)
    ↓
LoginScreen detects change → navigates to HOME
    ↓
User sees HomeScreen ✅
    ↓
Close & reopen app → Goes directly to HOME (persisted)
```

---

## ✅ Checklist

Before testing:

- [ ] Consent screen configured (use direct URL)
- [ ] `app/google-services.json` downloaded locally
- [ ] `secrets.properties` contains `GOOGLE_WEB_CLIENT_ID`
- [ ] Build successful
- [ ] App runs

---

## 🐛 Troubleshooting

**Problem: "Sign in button doesn't appear"**
- Build failed? Run: `./gradlew clean build`
- Check `google-services.json` is in app/ folder

**Problem: "Google popup doesn't appear"**
- Web Client ID is wrong or missing
- Check Logcat: `adb logcat | grep -i "google\|auth"`

**Problem: "Sign-in fails with error"**
- OAuth Consent Screen not configured
- Your test email not added as Test User
- Check Logcat for error codes

**Problem: "Signs in but doesn't navigate to HomeScreen"**
- Check `AppNavGraph.kt` — is it checking authState?
- Check `LaunchedEffect` in LoginScreen — correct logic?

---

## 🎯 When Done

- ✅ LoginScreen appears
- ✅ Sign in with Google works
- ✅ Auto-navigates to HomeScreen
- ✅ Reopening app skips login
- ✅ User data available (name, email)

---

## 📚 Files Reference

| File | Purpose |
|------|---------|
| `data/AuthRepository.kt` | Firebase auth logic |
| `ui/auth/AuthViewModel.kt` | Auth ViewModel |
| `ui/auth/LoginScreen.kt` | Login UI |
| `navigation/Routes.kt` | Routes constants |
| `navigation/AppNavGraph.kt` | Navigation with auth |

---

## 💡 Quick Links

- Firebase Console: https://console.firebase.google.com/
- Google Cloud Console: https://console.cloud.google.com/
- **OAuth Consent Screen (DIRECT):** https://console.cloud.google.com/apis/credentials/consent
- Credentials: https://console.cloud.google.com/apis/credentials
