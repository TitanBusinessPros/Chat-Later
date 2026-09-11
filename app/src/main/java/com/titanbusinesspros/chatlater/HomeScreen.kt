package com.titanbusinesspros.chatlater

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

// How many days of free use before a login requires payment.
// TODO: change this if you want a longer/shorter trial.
const val TRIAL_DAYS = 7L

// TODO: replace with your real Stripe Payment Link once it's created.
const val STRIPE_PAYMENT_LINK = "https://buy.stripe.com/REPLACE_ME"

private data class TrialStatus(val trialStartMillis: Long, val isPaid: Boolean)

@Composable
fun HomeScreen(user: FirebaseUser, firestore: FirebaseFirestore) {
    val context = LocalContext.current
    var status by remember { mutableStateOf<TrialStatus?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    // Checks once per screen visit whether a newer APK is posted on the download site.
    LaunchedEffect(Unit) {
        checkForUpdate(context) { updateInfo = it }
    }

    // Reads (or creates, for older accounts) this user's trial/payment record in Firestore.
    LaunchedEffect(user.uid) {
        val docRef = firestore.collection("users").document(user.uid)
        docRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val trialStart = snapshot.getLong("trialStartMillis") ?: System.currentTimeMillis()
                    val isPaid = snapshot.getBoolean("isPaid") ?: false
                    status = TrialStatus(trialStart, isPaid)
                } else {
                    val now = System.currentTimeMillis()
                    docRef.set(mapOf("trialStartMillis" to now, "isPaid" to false))
                    status = TrialStatus(now, false)
                }
            }
            .addOnFailureListener { e -> errorMessage = e.message }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        updateInfo?.let { UpdateBanner(it) }

        when {
            errorMessage != null -> Text("Error loading account: $errorMessage")
            status == null -> CircularProgressIndicator()
            else -> {
                val daysUsed = (System.currentTimeMillis() - status!!.trialStartMillis) / (1000L * 60 * 60 * 24)
                val trialActive = status!!.isPaid || daysUsed < TRIAL_DAYS
                if (trialActive) {
                    ConversationTranslatorScreen(
                        daysLeft = (TRIAL_DAYS - daysUsed).coerceAtLeast(0),
                        isPaid = status!!.isPaid
                    )
                } else {
                    TrialExpiredScreen()
                }
            }
        }
    }
}

// Shown at the top of the app when version.json on the download site reports a newer build.
@Composable
private fun UpdateBanner(update: UpdateInfo) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Update available" + if (update.versionName.isNotBlank()) " (v${update.versionName})" else "")
        Button(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.apkUrl)))
        }) {
            Text("Download")
        }
    }
}

@Composable
fun TrialExpiredScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Your free trial has ended.")
        Button(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(STRIPE_PAYMENT_LINK)))
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Subscribe Now")
        }
    }
}
