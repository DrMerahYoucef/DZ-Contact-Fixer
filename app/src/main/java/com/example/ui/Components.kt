package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.BackupResult
import com.example.data.BatchPhase
import com.example.data.BatchProgress
import com.example.data.CleanupMode
import com.example.data.ContactFixPlan
import com.example.data.ContactFixStatus
import com.example.data.PlannedOperation
import com.example.data.PreviewFilter
import com.example.data.ScanSummary
import com.example.data.SkipReason
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalTelemetryColors

@Composable
fun PermissionGateView(
    permanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.widthIn(max = 520.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsCar,
                        contentDescription = stringResource(R.string.cd_app_emblem),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(38.dp),
                    )
                }

                Text(
                    text = stringResource(R.string.permission_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = stringResource(R.string.permission_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PermissionBulletRow(
                            icon = Icons.Filled.PhoneAndroid,
                            text = "Pairs +213[567]XXXXXXXX and 0[567]XXXXXXXX",
                        )
                        PermissionBulletRow(
                            icon = Icons.Filled.DoNotDisturbOn,
                            text = "Leaves foreign (+33, +90...) & landlines untouched",
                        )
                        PermissionBulletRow(
                            icon = Icons.Filled.Lock,
                            text = "100% offline — exports VCF + JSON backup to Downloads",
                        )
                    }
                }

                if (permanentlyDenied) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.permission_denied_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }

                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("open_settings_button"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.permission_settings_button))
                    }
                }

                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("grant_permission_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (permanentlyDenied) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    ),
                ) {
                    Icon(Icons.Filled.Contacts, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.permission_grant_button))
                }
            }
        }
    }
}

@Composable
private fun PermissionBulletRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun SummaryDashboardSection(
    summary: ScanSummary,
    cleanupMode: CleanupMode,
    lastBackupResult: BackupResult?,
    onToggleCleanupMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val telemetry = LocalTelemetryColors.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Primary telemetry row (Scanned, Compliant, Needs Fix)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricTile(
                label = stringResource(R.string.metric_total_scanned),
                value = summary.totalContactsScanned,
                accentColor = MaterialTheme.colorScheme.onSurface,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = stringResource(R.string.metric_compliant),
                value = summary.alreadyCompliantCount,
                accentColor = telemetry.addedContent,
                containerColor = telemetry.addedContainer.copy(alpha = 0.45f),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = stringResource(R.string.metric_missing_national),
                value = summary.missingNationalCount,
                accentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.weight(1f),
            )
        }

        // Secondary telemetry row (Missing +213, Skipped numbers)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricTile(
                label = stringResource(R.string.metric_missing_intl),
                value = summary.missingInternationalCount,
                accentColor = telemetry.infoContent,
                containerColor = telemetry.infoContainer.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "Spaced Cleanups",
                value = summary.spacedDuplicatesCount,
                accentColor = telemetry.cleanContent,
                containerColor = telemetry.cleanContainer.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = stringResource(R.string.metric_skipped),
                value = summary.ignoredNumbersCount,
                accentColor = telemetry.neutralBadgeContent,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.weight(1f),
            )
        }

        // Mode B vs Mode A toggle card
        val isCleanMode = cleanupMode == CleanupMode.CLEAN_AND_NORMALIZE
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SyncAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.mode_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = stringResource(
                            if (isCleanMode) R.string.mode_clean_desc else R.string.mode_append_desc
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Switch(
                    checked = isCleanMode,
                    onCheckedChange = onToggleCleanupMode,
                    modifier = Modifier.testTag("cleanup_mode_switch"),
                )
            }
        }

        AnimatedVisibility(visible = lastBackupResult != null) {
            lastBackupResult?.let { backup ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = telemetry.addedContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, telemetry.addedContent.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DownloadDone,
                            contentDescription = null,
                            tint = telemetry.addedContent,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Backup (${backup.timestampFormatted}): ${backup.vcfFileName} + ${backup.jsonFileName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: Int,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                ),
                color = accentColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SearchAndFilterHeader(
    searchQuery: String,
    activeFilter: PreviewFilter,
    filterCounts: Map<PreviewFilter, Int>,
    visibleModifiableCount: Int,
    allVisibleSelected: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (PreviewFilter) -> Unit,
    onToggleSelectAllVisible: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.testTag("clear_search_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_clear_search),
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_contacts_input"),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PreviewFilter.entries.forEach { filter ->
                val count = filterCounts[filter] ?: 0
                val label = when (filter) {
                    PreviewFilter.ALL_CHANGES -> stringResource(R.string.filter_all_changes, count)
                    PreviewFilter.MISSING_0X -> stringResource(R.string.filter_missing_0x, count)
                    PreviewFilter.MISSING_213 -> stringResource(R.string.filter_missing_213, count)
                    PreviewFilter.SPACED_CLEANUPS -> stringResource(R.string.filter_spaced, count)
                    PreviewFilter.COMPLIANT -> stringResource(R.string.filter_compliant, count)
                    PreviewFilter.SKIPPED -> stringResource(R.string.filter_skipped, count)
                }
                FilterChip(
                    selected = activeFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}"),
                )
            }
        }

        if (visibleModifiableCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$visibleModifiableCount modifiable contact(s) shown",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onToggleSelectAllVisible,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("toggle_select_all_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SelectAll,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            if (allVisibleSelected) R.string.deselect_all else R.string.select_all
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactPlanCard(
    plan: ContactFixPlan,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val telemetry = LocalTelemetryColors.current
    val borderColor by animateColorAsState(
        targetValue = when {
            plan.isModifiable && isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = spring(),
        label = "card_border",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("contact_card_${plan.rawContactId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.record.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val accountLabel = buildString {
                        append(formatAccountType(plan.record.accountType))
                        if (plan.hasAddedLocalEntry) {
                            append(" • DZ Local (0X) tagged")
                        }
                    }
                    Text(
                        text = accountLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                when (plan.status) {
                    ContactFixStatus.NEEDS_FIX -> {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("contact_checkbox_${plan.rawContactId}"),
                        )
                    }
                    ContactFixStatus.COMPLIANT -> {
                        StatusPill(
                            text = stringResource(R.string.badge_compliant),
                            containerColor = telemetry.addedContainer,
                            contentColor = telemetry.addedContent,
                            icon = Icons.Filled.CheckCircle,
                        )
                    }
                    ContactFixStatus.COVERED_BY_WRITABLE -> {
                        StatusPill(
                            text = stringResource(R.string.badge_covered_by_writable),
                            containerColor = telemetry.infoContainer,
                            contentColor = telemetry.infoContent,
                            icon = Icons.Filled.CheckCircle,
                        )
                    }
                    ContactFixStatus.READ_ONLY -> {
                        StatusPill(
                            text = stringResource(R.string.badge_read_only),
                            containerColor = telemetry.cleanContainer,
                            contentColor = telemetry.cleanContent,
                            icon = Icons.Filled.Lock,
                        )
                    }
                    ContactFixStatus.SKIPPED_ONLY -> {
                        StatusPill(
                            text = "Ignored",
                            containerColor = telemetry.neutralBadgeContainer,
                            contentColor = telemetry.neutralBadgeContent,
                            icon = Icons.Filled.DoNotDisturbOn,
                        )
                    }
                }
            }

            // Current numbers section
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.label_current_numbers),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    plan.record.phoneEntries.forEach { entry ->
                        val suffix = entry.label
                            ?.takeIf { it in com.example.data.AlgerianNumberNormalizer.REVERTIBLE_CUSTOM_LABELS }
                            ?.let { " [$it]" }
                            ?: ""
                        MonoNumberPill(
                            text = "${entry.rawNumber}$suffix",
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            val additions = plan.operations.filter {
                it is PlannedOperation.InsertInternational ||
                    it is PlannedOperation.InsertNationalLocal ||
                    it is PlannedOperation.CreateWritableMirrorContact
            }
            val cleanups = plan.operations.filter {
                it is PlannedOperation.NormalizeExistingToClean || it is PlannedOperation.DeleteSpacedDuplicate
            }

            if (additions.isNotEmpty() || cleanups.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            if (additions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val mirrorOp = additions.filterIsInstance<PlannedOperation.CreateWritableMirrorContact>().firstOrNull()
                    Text(
                        text = if (mirrorOp != null) {
                            val targetLabel = mirrorOp.targetAccount.accountName
                                ?: formatAccountType(mirrorOp.targetAccount.accountType)
                            stringResource(R.string.label_will_copy_to_account, targetLabel)
                        } else {
                            stringResource(R.string.label_will_add)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = telemetry.addedContent,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        additions.forEach { op ->
                            when (op) {
                                is PlannedOperation.InsertInternational -> {
                                    MonoNumberPill(
                                        text = "+ ${op.targetNumber} [Mobile]",
                                        containerColor = telemetry.addedContainer,
                                        contentColor = telemetry.addedContent,
                                    )
                                }
                                is PlannedOperation.InsertNationalLocal -> {
                                    MonoNumberPill(
                                        text = "+ ${op.targetNumber} [DZ Local]",
                                        containerColor = telemetry.addedContainer,
                                        contentColor = telemetry.addedContent,
                                    )
                                }
                                is PlannedOperation.CreateWritableMirrorContact -> {
                                    op.cores.forEach { core9 ->
                                        MonoNumberPill(
                                            text = "+ +213$core9 [Mobile]",
                                            containerColor = telemetry.addedContainer,
                                            contentColor = telemetry.addedContent,
                                        )
                                        MonoNumberPill(
                                            text = "+ 0$core9 [DZ Local]",
                                            containerColor = telemetry.addedContainer,
                                            contentColor = telemetry.addedContent,
                                        )
                                    }
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }

            if (cleanups.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.label_will_clean),
                        style = MaterialTheme.typography.labelSmall,
                        color = telemetry.cleanContent,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        cleanups.forEach { op ->
                            when (op) {
                                is PlannedOperation.NormalizeExistingToClean -> {
                                    MonoNumberPill(
                                        text = "${op.originalNumber} -> ${op.targetNumber}",
                                        containerColor = telemetry.cleanContainer,
                                        contentColor = telemetry.cleanContent,
                                    )
                                }
                                is PlannedOperation.DeleteSpacedDuplicate -> {
                                    MonoNumberPill(
                                        text = "- ${op.originalNumber}",
                                        containerColor = telemetry.cleanContainer,
                                        contentColor = telemetry.cleanContent,
                                        strikethrough = true,
                                    )
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }

            if (plan.untouchedEntries.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.label_untouched),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        plan.untouchedEntries.forEach { item ->
                            val reasonTag = when (item.reason) {
                                SkipReason.FOREIGN_NUMBER -> "Foreign"
                                SkipReason.LANDLINE_OR_SHORT -> "Landline/Special"
                                SkipReason.UNRECOGNIZED -> "Skipped"
                            }
                            MonoNumberPill(
                                text = "${item.entry.rawNumber} ($reasonTag)",
                                containerColor = telemetry.neutralBadgeContainer,
                                contentColor = telemetry.neutralBadgeContent,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun MonoNumberPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    strikethrough: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.5.sp,
                textDecoration = if (strikethrough) TextDecoration.LineThrough else TextDecoration.None,
            ),
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun EmptyPreviewCard(
    allCompliant: Boolean,
    modifier: Modifier = Modifier,
) {
    val telemetry = LocalTelemetryColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (allCompliant) Icons.Filled.CheckCircle else Icons.Filled.Search,
                contentDescription = null,
                tint = if (allCompliant) telemetry.addedContent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(44.dp),
            )
            Text(
                text = stringResource(
                    if (allCompliant) R.string.all_compliant_title else R.string.empty_results_title
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    if (allCompliant) R.string.all_compliant_subtitle else R.string.empty_results_subtitle
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun BatchProgressDialog(progress: BatchProgress) {
    if (progress.phase == BatchPhase.IDLE) return

    val message = when (progress.phase) {
        BatchPhase.EXPORTING_BACKUP -> stringResource(R.string.batch_progress_backing_up)
        BatchPhase.APPLYING_FIXES -> stringResource(
            R.string.batch_progress_applying,
            progress.currentOps,
            progress.totalOps,
        )
        BatchPhase.REVERTING_LOCAL -> stringResource(
            R.string.batch_progress_reverting,
            progress.currentOps,
            progress.totalOps,
        )
        BatchPhase.IDLE -> ""
    }

    AlertDialog(
        onDismissRequest = {},
        title = { Text(text = "Processing Contacts") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (progress.phase == BatchPhase.EXPORTING_BACKUP || progress.totalOps <= 0) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(
                        progress = { progress.fraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
fun ApplyConfirmationDialog(
    selectedContacts: Int,
    totalOperations: Int,
    hasBackupInSession: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.AddCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(stringResource(R.string.confirm_apply_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "This will apply $totalOperations operation(s) across $selectedContacts contact(s) in batches of up to 400 operations.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!hasBackupInSession) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(R.string.backup_recommended_warning),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.confirm_apply_cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_apply_dialog_button"),
            ) {
                Text(stringResource(R.string.confirm_apply_proceed))
            }
        },
    )
}

@Composable
fun RevertConfirmationDialog(
    localEntriesCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.DeleteSweep,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(stringResource(R.string.confirm_revert_title)) },
        text = {
            Text(
                text = stringResource(R.string.confirm_revert_message, localEntriesCount),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.confirm_apply_cancel))
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = onConfirm,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_revert_dialog_button"),
            ) {
                Text(stringResource(R.string.confirm_revert_proceed))
            }
        },
    )
}

private fun formatAccountType(accountType: String?): String = when (accountType) {
    null -> "Device / Local"
    "com.google" -> "Google Account"
    "com.whatsapp" -> "WhatsApp (Read-only)"
    "com.whatsapp.w4b" -> "WhatsApp Business (Read-only)"
    "org.telegram.messenger" -> "Telegram (Read-only)"
    "com.samsung.android.mobileservice",
    "com.osp.app.signin" -> "Samsung Account"
    "com.xiaomi" -> "Xiaomi Account"
    else -> accountType.substringAfterLast('.').replaceFirstChar { it.uppercase() }
}
