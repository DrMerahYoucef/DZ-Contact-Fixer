package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.PreviewFilter
import kotlinx.coroutines.flow.SharedFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    events: SharedFlow<MainUiEvent>,
    onRequestContactsPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRescan: () -> Unit,
    onToggleCleanupMode: (Boolean) -> Unit,
    onFilterSelected: (PreviewFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleContactSelection: (Long) -> Unit,
    onToggleSelectAllVisible: () -> Unit,
    onBackup: () -> Unit,
    onRequestApplyFixes: () -> Unit,
    onDismissApplyConfirmation: () -> Unit,
    onConfirmApplyFixes: () -> Unit,
    onRequestRevertLocal: () -> Unit,
    onDismissRevertConfirmation: () -> Unit,
    onConfirmRevertLocal: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    val legacyStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onBackup()
        }
    }

    val handleBackupClick: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            onBackup()
        } else {
            val hasLegacyWrite = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
            if (hasLegacyWrite) {
                onBackup()
            } else {
                legacyStorageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is MainUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DirectionsCar,
                                contentDescription = stringResource(R.string.cd_app_emblem),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stringResource(R.string.app_subtitle),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.hasContactsPermission) {
                        IconButton(
                            onClick = onRescan,
                            enabled = !uiState.isScanning,
                            modifier = Modifier.testTag("rescan_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.cd_rescan),
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { overflowMenuExpanded = true },
                                modifier = Modifier.testTag("more_actions_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(R.string.cd_more_actions),
                                )
                            }
                            DropdownMenu(
                                expanded = overflowMenuExpanded,
                                onDismissRequest = { overflowMenuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.backup_button)) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.SaveAlt, contentDescription = null)
                                    },
                                    onClick = {
                                        overflowMenuExpanded = false
                                        handleBackupClick()
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        val count = uiState.summary.addedLocalEntriesCount
                                        Text(
                                            if (count > 0) {
                                                stringResource(R.string.revert_button_with_count, count)
                                            } else {
                                                stringResource(R.string.revert_button)
                                            }
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.DeleteSweep,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        overflowMenuExpanded = false
                                        onRequestRevertLocal()
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (uiState.hasContactsPermission && uiState.hasCompletedInitialScan) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth < 720.dp) {
                        CompactActionBottomBar(
                            selectedContactsCount = uiState.selectedModifiableCount,
                            addedLocalCount = uiState.summary.addedLocalEntriesCount,
                            isScanning = uiState.isScanning,
                            onBackup = handleBackupClick,
                            onApplyFixes = onRequestApplyFixes,
                            onRevertLocal = onRequestRevertLocal,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = when {
                !uiState.hasContactsPermission -> ScreenMode.PERMISSION_GATE
                uiState.isScanning && !uiState.hasCompletedInitialScan -> ScreenMode.INITIAL_SCANNING
                else -> ScreenMode.DASHBOARD
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "screen_mode_transition",
        ) { mode ->
            when (mode) {
                ScreenMode.PERMISSION_GATE -> {
                    PermissionGateView(
                        permanentlyDenied = uiState.permissionPermanentlyDenied,
                        onRequestPermission = onRequestContactsPermission,
                        onOpenSettings = onOpenAppSettings,
                    )
                }

                ScreenMode.INITIAL_SCANNING -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = stringResource(R.string.scanning_contacts),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }

                ScreenMode.DASHBOARD -> {
                    DashboardContent(
                        uiState = uiState,
                        onToggleCleanupMode = onToggleCleanupMode,
                        onFilterSelected = onFilterSelected,
                        onSearchQueryChange = onSearchQueryChange,
                        onToggleContactSelection = onToggleContactSelection,
                        onToggleSelectAllVisible = onToggleSelectAllVisible,
                        onBackup = handleBackupClick,
                        onApplyFixes = onRequestApplyFixes,
                        onRevertLocal = onRequestRevertLocal,
                    )
                }
            }
        }
    }

    BatchProgressDialog(progress = uiState.batchProgress)

    if (uiState.showApplyConfirmation) {
        ApplyConfirmationDialog(
            selectedContacts = uiState.selectedModifiableCount,
            totalOperations = uiState.selectedOperationsCount,
            hasBackupInSession = uiState.lastBackupResult != null,
            onDismiss = onDismissApplyConfirmation,
            onConfirm = onConfirmApplyFixes,
        )
    }

    if (uiState.showRevertConfirmation) {
        RevertConfirmationDialog(
            localEntriesCount = uiState.summary.addedLocalEntriesCount,
            onDismiss = onDismissRevertConfirmation,
            onConfirm = onConfirmRevertLocal,
        )
    }
}

private enum class ScreenMode {
    PERMISSION_GATE,
    INITIAL_SCANNING,
    DASHBOARD,
}

@Composable
private fun DashboardContent(
    uiState: MainUiState,
    onToggleCleanupMode: (Boolean) -> Unit,
    onFilterSelected: (PreviewFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleContactSelection: (Long) -> Unit,
    onToggleSelectAllVisible: () -> Unit,
    onBackup: () -> Unit,
    onApplyFixes: () -> Unit,
    onRevertLocal: () -> Unit,
) {
    val visibleModifiable = remember(uiState.filteredPlans) {
        uiState.filteredPlans.filter { it.isModifiable }
    }
    val allVisibleSelected = remember(visibleModifiable, uiState.excludedRawContactIds) {
        visibleModifiable.isNotEmpty() &&
            visibleModifiable.all { !uiState.excludedRawContactIds.contains(it.rawContactId) }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedWidth = maxWidth >= 720.dp

        if (isExpandedWidth) {
            // Tablet / Foldable Two-Pane Canonical Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SummaryDashboardSection(
                        summary = uiState.summary,
                        cleanupMode = uiState.cleanupMode,
                        lastBackupResult = uiState.lastBackupResult,
                        onToggleCleanupMode = onToggleCleanupMode,
                    )

                    ExpandedSidebarActions(
                        selectedContactsCount = uiState.selectedModifiableCount,
                        addedLocalCount = uiState.summary.addedLocalEntriesCount,
                        isScanning = uiState.isScanning,
                        onBackup = onBackup,
                        onApplyFixes = onApplyFixes,
                        onRevertLocal = onRevertLocal,
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SearchAndFilterHeader(
                        searchQuery = uiState.searchQuery,
                        activeFilter = uiState.activeFilter,
                        filterCounts = uiState.filterCounts,
                        visibleModifiableCount = visibleModifiable.size,
                        allVisibleSelected = allVisibleSelected,
                        onSearchQueryChange = onSearchQueryChange,
                        onFilterSelected = onFilterSelected,
                        onToggleSelectAllVisible = onToggleSelectAllVisible,
                    )

                    PreviewListColumn(
                        uiState = uiState,
                        onToggleContactSelection = onToggleContactSelection,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            // Compact Phone Layout
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "summary_dashboard") {
                    SummaryDashboardSection(
                        summary = uiState.summary,
                        cleanupMode = uiState.cleanupMode,
                        lastBackupResult = uiState.lastBackupResult,
                        onToggleCleanupMode = onToggleCleanupMode,
                    )
                }

                item(key = "search_filter_header") {
                    SearchAndFilterHeader(
                        searchQuery = uiState.searchQuery,
                        activeFilter = uiState.activeFilter,
                        filterCounts = uiState.filterCounts,
                        visibleModifiableCount = visibleModifiable.size,
                        allVisibleSelected = allVisibleSelected,
                        onSearchQueryChange = onSearchQueryChange,
                        onFilterSelected = onFilterSelected,
                        onToggleSelectAllVisible = onToggleSelectAllVisible,
                    )
                }

                if (uiState.errorMessage != null) {
                    item(key = "error_banner") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = uiState.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }

                if (uiState.filteredPlans.isEmpty()) {
                    item(key = "empty_state") {
                        EmptyPreviewCard(
                            allCompliant = uiState.summary.contactsNeedingFixCount == 0 &&
                                uiState.summary.totalContactsScanned > 0 &&
                                uiState.searchQuery.isBlank()
                        )
                    }
                } else {
                    items(
                        items = uiState.filteredPlans,
                        key = { it.rawContactId },
                    ) { plan ->
                        val isSelected = !uiState.excludedRawContactIds.contains(plan.rawContactId)
                        ContactPlanCard(
                            plan = plan,
                            isSelected = isSelected,
                            onToggleSelect = { onToggleContactSelection(plan.rawContactId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewListColumn(
    uiState: MainUiState,
    onToggleContactSelection: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.filteredPlans.isEmpty()) {
            item(key = "empty_state") {
                EmptyPreviewCard(
                    allCompliant = uiState.summary.contactsNeedingFixCount == 0 &&
                        uiState.summary.totalContactsScanned > 0 &&
                        uiState.searchQuery.isBlank()
                )
            }
        } else {
            items(
                items = uiState.filteredPlans,
                key = { it.rawContactId },
            ) { plan ->
                val isSelected = !uiState.excludedRawContactIds.contains(plan.rawContactId)
                ContactPlanCard(
                    plan = plan,
                    isSelected = isSelected,
                    onToggleSelect = { onToggleContactSelection(plan.rawContactId) },
                )
            }
        }
    }
}

@Composable
private fun CompactActionBottomBar(
    selectedContactsCount: Int,
    addedLocalCount: Int,
    isScanning: Boolean,
    onBackup: () -> Unit,
    onApplyFixes: () -> Unit,
    onRevertLocal: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (addedLocalCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "$addedLocalCount DZ Local (0X) number(s) active",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = onRevertLocal,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("revert_local_chip_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.revert_button_with_count, addedLocalCount),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onBackup,
                    enabled = !isScanning,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("backup_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SaveAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.backup_button),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Button(
                    onClick = onApplyFixes,
                    enabled = !isScanning && selectedContactsCount > 0,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.15f)
                        .height(50.dp)
                        .testTag("apply_fix_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.apply_button, selectedContactsCount),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedSidebarActions(
    selectedContactsCount: Int,
    addedLocalCount: Int,
    isScanning: Boolean,
    onBackup: () -> Unit,
    onApplyFixes: () -> Unit,
    onRevertLocal: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onApplyFixes,
            enabled = !isScanning && selectedContactsCount > 0,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("apply_fix_button"),
        ) {
            Icon(Icons.Filled.Build, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.apply_button, selectedContactsCount))
        }

        OutlinedButton(
            onClick = onBackup,
            enabled = !isScanning,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("backup_button"),
        ) {
            Icon(Icons.Filled.SaveAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.backup_button))
        }

        OutlinedButton(
            onClick = onRevertLocal,
            enabled = !isScanning && addedLocalCount > 0,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                1.dp,
                if (addedLocalCount > 0) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("revert_local_button"),
        ) {
            Icon(Icons.Filled.DeleteSweep, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                if (addedLocalCount > 0) {
                    stringResource(R.string.revert_button_with_count, addedLocalCount)
                } else {
                    stringResource(R.string.revert_button)
                }
            )
        }
    }
}
