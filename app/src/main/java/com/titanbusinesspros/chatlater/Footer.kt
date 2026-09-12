package com.titanbusinesspros.chatlater

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Shown at the bottom of every primary screen: sign-in, the translator, and trial-expired.
@Composable
fun AppFooter() {
    val context = LocalContext.current
    fun open(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppLogo()

        Text("Created by Titan Business Pros LLC", textAlign = TextAlign.Center)

        Row {
            Text(
                "405-998-7979",
                modifier = Modifier.clickable { open("tel:+14059987979") }
            )
            Text(" · ")
            Text(
                "www.oklahoma.marketing",
                modifier = Modifier.clickable { open("https://www.oklahoma.marketing/") }
            )
        }

        Text("Oklahoma City, OK")

        Row {
            Text(
                "Terms of Service",
                modifier = Modifier.clickable { open("https://titanbusinesspros.github.io/Chat-Later/terms.html") }
            )
            Text(" · ")
            Text(
                "Privacy Policy",
                modifier = Modifier.clickable { open("https://titanbusinesspros.github.io/Chat-Later/privacy.html") }
            )
        }

        Text(
            "© 2026 Titan Business Pros LLC. All rights reserved.",
            textAlign = TextAlign.Center
        )
    }
}

// The Titan Business Pros logo, shown above the footer text. Bundled as a local drawable
// resource (res/drawable-nodpi/titan_logo.png) - never downloaded at runtime.
@Composable
private fun AppLogo() {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = R.drawable.titan_logo),
        contentDescription = "Titan Business Pros logo",
        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        modifier = Modifier
            .padding(bottom = 4.dp)
            .height(16.dp)
    )
}

// The app's own launcher icon, shown at the top of each primary screen (sign-in, the
// translator, and trial-expired) in place of the Titan logo, which now lives in the
// footer instead. Uses the existing launcher resource - not a network image.
@Composable
fun AppIcon() {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = R.mipmap.ic_launcher_foreground),
        contentDescription = "Chat-Later app icon",
        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        modifier = Modifier
            .padding(bottom = 8.dp)
            .height(64.dp)
    )
}
