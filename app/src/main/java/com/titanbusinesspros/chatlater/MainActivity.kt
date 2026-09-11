package com.titanbusinesspros.chatlater

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.titanbusinesspros.chatlater.ui.theme.ChatLaterYallTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        setContent {
            ChatLaterYallTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Start already-logged-in if Firebase remembers a session, otherwise show login.
                    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

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
                }
            }
        }
    }
}