package com.titanbusinesspros.chatlater

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

// Some Firebase/Credential Manager exceptions carry a non-null but empty message
// (observed with certain Google Play services error paths), which `?: fallback` alone
// does not catch since that only substitutes on null - so every place below that shows
// an exception's message to the user falls back through this instead of bare `?:`.
private fun String?.ifBlankUse(fallback: String): String = if (isNullOrBlank()) fallback else this

// This is the "Web client (auto created by Google Service)" OAuth client ID that
// Firebase generates automatically for this project - not a secret, safe to embed.
// (Firebase console: Authentication -> Sign-in method -> Google, already enabled.)
private const val GOOGLE_WEB_CLIENT_ID =
    "193355563840-du0d9il7qufd4cd6reaklt1pqiokcmn1.apps.googleusercontent.com"

// Handles both "create a new account" and "log back in" with one screen.
@Composable
fun LoginScreen(
    auth: FirebaseAuth,
    onLoggedIn: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var justConfirmedUpToDate by remember { mutableStateOf(false) }

    fun checkUpdateNow() {
        checkingUpdate = true
        justConfirmedUpToDate = false
        checkForUpdate(context) { result ->
            checkingUpdate = false
            updateInfo = result
            if (result == null) justConfirmedUpToDate = true
        }
    }

    // Checks once when this screen appears, so you see it even before logging in.
    LaunchedEffect(Unit) { checkUpdateNow() }

    // Opens the system "choose a Google account" sheet, then signs in to Firebase with it.
    fun signInWithGoogle() {
        errorMessage = null
        isLoading = true
        scope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(GOOGLE_WEB_CLIENT_ID)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val response = CredentialManager.create(context).getCredential(context, request)
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            onLoggedIn()
                        } else {
                            errorMessage = task.exception?.message.ifBlankUse("Google sign-in failed")
                        }
                    }
            } catch (e: GetCredentialCancellationException) {
                isLoading = false // user backed out of the account picker - not an error
            } catch (e: GetCredentialException) {
                isLoading = false
                errorMessage = e.message.ifBlankUse("Google sign-in failed")
            } catch (e: Exception) {
                // Last resort: anything unexpected (a malformed credential response, a
                // GoogleIdTokenParsingException, etc.) still lands on screen with the
                // exception's own class/message instead of being silently invisible or
                // crashing this coroutine uncaught.
                isLoading = false
                errorMessage = "Google sign-in failed: ${e.javaClass.simpleName}: ${e.message.ifBlankUse("(no details)")}"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        updateInfo?.let { UpdateBanner(it) }

        // Scrollable main content takes all space above the footer; the footer itself
        // is a sibling below it (outside this weighted Column), so it sits at the
        // actual bottom of the screen instead of after the content in scroll order.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppIcon()

            Text("Chat Later - Sign In")

            TextButton(onClick = { checkUpdateNow() }, enabled = !checkingUpdate) {
                Text(
                    when {
                        checkingUpdate -> "Checking for updates..."
                        justConfirmedUpToDate -> "You're on the latest version"
                        else -> "Check for updates"
                    }
                )
            }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        errorMessage?.let {
            Text(text = it, modifier = Modifier.padding(top = 8.dp))
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        } else {
            Button(
                onClick = {
                    // Firebase Auth throws IllegalArgumentException synchronously - not
                    // via the failure listener below - if either string is empty, so
                    // that has to be caught here before calling it at all.
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Enter both an email and a password"
                        return@Button
                    }
                    errorMessage = null
                    isLoading = true
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                onLoggedIn()
                            } else {
                                errorMessage = task.exception?.message.ifBlankUse("Sign in failed")
                            }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Log In")
            }

            TextButton(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Enter both an email and a password"
                        return@TextButton
                    }
                    errorMessage = null
                    isLoading = true
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                // Start their free trial clock the moment the account is created.
                                val uid = task.result?.user?.uid
                                if (uid != null) {
                                    FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(uid)
                                        .set(
                                            mapOf(
                                                "trialStartMillis" to System.currentTimeMillis(),
                                                "isPaid" to false
                                            )
                                        )
                                }
                                onLoggedIn()
                            } else {
                                errorMessage = task.exception?.message.ifBlankUse("Sign up failed")
                            }
                        }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("New here? Create an account")
            }

            Text("or", modifier = Modifier.padding(top = 16.dp))

            OutlinedButton(
                onClick = { signInWithGoogle() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Sign in with Google")
            }
        }
        }

        AppFooter()
    }
}
