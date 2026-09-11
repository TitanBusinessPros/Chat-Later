package com.titanbusinesspros.chatlater

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.titanbusinesspros.chatlater.ui.theme.ChatLaterYallTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashReporter(this)
        val lastCrash = readAndClearLastCrash(this)
        enableEdgeToEdge()
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        setContent {
            ChatLaterYallTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Start already-logged-in if Firebase remembers a session, otherwise show login.
                    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
                    var crashToShow by remember { mutableStateOf(lastCrash) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isLoggedIn && auth.currentUser != null) {
                            HomeScreen(
                                user = auth.currentUser!!,
                                firestore = firestore
                            )
                        } else {
                            LoginScreen(
                                auth = auth,
                                onLoggedIn = { isLoggedIn = true }
                            )
                        }

                        // If the app crashed last time, show exactly what happened right on
                        // screen - no computer or cable needed to see and report it.
                        crashToShow?.let { crash ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("The app crashed last time. Copy this and send it in to get it fixed:")
                                    SelectionContainer {
                                        Text(text = crash, modifier = Modifier.padding(top = 8.dp))
                                    }
                                    Button(
                                        onClick = { crashToShow = null },
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}