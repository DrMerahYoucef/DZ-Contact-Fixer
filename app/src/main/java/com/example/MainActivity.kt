package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.DZContactFixerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requiredContactsPermissions = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DZContactFixerTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val lifecycleOwner = LocalLifecycleOwner.current

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    val allGranted = requiredContactsPermissions.all { perm ->
                        results[perm] == true || hasPermission(perm)
                    }
                    val permanentlyDenied = !allGranted && requiredContactsPermissions.any { perm ->
                        !hasPermission(perm) &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, perm)
                    }
                    viewModel.onPermissionStateUpdated(
                        granted = allGranted,
                        permanentlyDenied = permanentlyDenied,
                    )
                }

                LaunchedEffect(Unit) {
                    val granted = hasAllContactsPermissions()
                    viewModel.onPermissionStateUpdated(granted = granted, permanentlyDenied = false)
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val granted = hasAllContactsPermissions()
                            if (granted != uiState.hasContactsPermission) {
                                viewModel.onPermissionStateUpdated(
                                    granted = granted,
                                    permanentlyDenied = false,
                                )
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                MainScreen(
                    uiState = uiState,
                    events = viewModel.events,
                    onRequestContactsPermission = {
                        permissionLauncher.launch(requiredContactsPermissions)
                    },
                    onOpenAppSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", packageName, null),
                        )
                        startActivity(intent)
                    },
                    onRescan = viewModel::scanContacts,
                    onToggleCleanupMode = viewModel::toggleCleanupMode,
                    onFilterSelected = viewModel::setFilter,
                    onSearchQueryChange = viewModel::setSearchQuery,
                    onToggleContactSelection = viewModel::toggleContactSelection,
                    onToggleSelectAllVisible = viewModel::toggleSelectAllVisible,
                    onBackup = viewModel::backupContacts,
                    onRequestApplyFixes = viewModel::requestApplyFixes,
                    onDismissApplyConfirmation = viewModel::dismissApplyConfirmation,
                    onConfirmApplyFixes = viewModel::confirmApplyFixes,
                    onRequestRevertLocal = viewModel::requestRevertLocalNumbers,
                    onDismissRevertConfirmation = viewModel::dismissRevertConfirmation,
                    onConfirmRevertLocal = viewModel::confirmRevertLocalNumbers,
                )
            }
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun hasAllContactsPermissions(): Boolean =
        requiredContactsPermissions.all(::hasPermission)
}
