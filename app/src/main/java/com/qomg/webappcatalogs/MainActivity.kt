package com.qomg.webappcatalogs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.CommonStatusCodes.SUCCESS
import com.qomg.webappcatalogs.api.AppLogger
import com.qomg.webappcatalogs.network.NetworkUsage
import com.qomg.webappcatalogs.ui.theme.WebAppCatalogsTheme
import com.qomg.webappcatalogs.webapp.WebAppHomeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebAppCatalogsTheme {
                WebAppHomeScreen()
            }
        }
        simulateSocketTimeout()
        simulateTimeoutWithCoroutine()
        // Pings and downloads synchronously, so it must stay off the main thread
        lifecycleScope.launch(Dispatchers.IO) { NetworkUsage.updateNetworkStatus(applicationContext) }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        onTokenComplete(this)
        AppLogger.d("MyActivity", "Activity created")

        try {
            // Some operation
        } catch (e: Exception) {
            AppLogger.e("MyActivity", "Operation failed", e)
        }
    }

    override fun onStart() {
        super.onStart()
        val gpa = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (gpa == SUCCESS) return
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
    }
}
