package com.visible.internationalminutes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.visible.internationalminutes.ui.MainApp
import com.visible.internationalminutes.ui.MainViewModel
import com.visible.internationalminutes.ui.theme.VisibleInternationalMinutesTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        checkAndUpdatePermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkAndUpdatePermissions()
        requestAppPermissions()

        setContent {
            VisibleInternationalMinutesTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val uiState by viewModel.uiState.collectAsState()
                    MainApp(
                        viewModel = viewModel,
                        uiState = uiState,
                        onRequestPermissions = { requestAppPermissions() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndUpdatePermissions()
        viewModel.checkBatteryOptimization()
    }

    private fun checkAndUpdatePermissions() {
        val hasCallLog = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        viewModel.updatePermissions(hasCallLog, hasNotification)
        viewModel.checkBatteryOptimization()
    }

    private fun requestAppPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestPermissionsLauncher.launch(permissions.toTypedArray())
    }
}
