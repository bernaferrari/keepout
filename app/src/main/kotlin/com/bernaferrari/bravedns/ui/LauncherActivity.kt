package com.bernaferrari.bravedns.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Public launcher entry. In-app navigation goes directly to the private Home activity. */
class LauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, HomeScreenActivity::class.java))
        finish()
    }
}
