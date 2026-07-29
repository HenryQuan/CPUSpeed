package com.yihengquan.cpuspeed.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yihengquan.cpuspeed.data.CPUManager
import com.yihengquan.cpuspeed.data.CoreFrequency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Settings bottom sheet
    if (state.showSettings) {
        ModalBottomSheet(onDismissRequest = { viewModel.dismissSettings() }) {
            SettingsSheetContent()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("CPUSpeed v${getVersion()}") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                actions = {
                    IconButton(onClick = { viewModel.toggleSettings() }) {
                        Icon(Icons.Default.Info, contentDescription = "About")
                    }
                    IconButton(onClick = { viewModel.loadCPUInfo() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (!state.rootChecked) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Root status card
            RootStatusCard(isRooted = state.isRooted)

            // CPU info
            if (state.cpuInfo.hasData) {
                CPUInfoCard(
                    info = state.cpuInfo.cpuInfo,
                    coreCount = state.cpuInfo.coreCount,
                    currentGovernor = state.currentGovernor
                )

                // Mode toggle: All-in-one vs Per-core
                ModeToggle(
                    perCoreMode = state.perCoreMode,
                    enabled = state.isRooted && !state.isBusy,
                    onToggle = { viewModel.togglePerCoreMode() }
                )

                if (state.perCoreMode && state.cpuInfo.coreFrequencies.isNotEmpty()) {
                    // Per-core sliders
                    PerCoreSliders(
                        coreFrequencies = state.cpuInfo.coreFrequencies,
                        coreMaxSliders = state.coreMaxSliders,
                        coreMinSliders = state.coreMinSliders,
                        enabled = state.isRooted && !state.isBusy,
                        onMaxChange = { core, p -> viewModel.setCoreMaxSlider(core, p) },
                        onMinChange = { core, p -> viewModel.setCoreMinSlider(core, p) },
                    )
                } else {
                    // All-in-one sliders
                    FrequencySlider(
                        label = "Max Frequency",
                        freq = state.cpuInfo.calcFrequency(state.maxSlider),
                        percent = state.maxSlider,
                        onValueChange = { viewModel.setMaxSpeed(it) },
                        enabled = state.isRooted && !state.isBusy
                    )
                    FrequencySlider(
                        label = "Min Frequency",
                        freq = state.cpuInfo.calcFrequency(state.minSlider),
                        percent = state.minSlider,
                        onValueChange = { viewModel.setMinSpeed(it) },
                        enabled = state.isRooted && !state.isBusy
                    )
                }

                // Apply button
                Button(
                    onClick = { viewModel.applySpeed() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.isRooted && !state.isBusy
                ) {
                    if (state.isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Apply")
                }

                // Governor selector
                if (state.availableGovernors.isNotEmpty()) {
                    GovernorSelector(
                        current = state.currentGovernor,
                        governors = state.availableGovernors,
                        enabled = state.isRooted && !state.isBusy,
                        onSelect = { viewModel.setGovernor(it) }
                    )
                }

                // Apply on boot toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Apply on boot", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = state.applyOnBoot,
                        onCheckedChange = { viewModel.toggleApplyOnBoot(it) },
                        enabled = state.isRooted
                    )
                }
            } else {
                // No data state
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Unable to read CPU info",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        if (!state.isRooted) {
                            Text(
                                "Device may not be rooted or CPU files are inaccessible",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadCPUInfo() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

// region Settings/About bottom sheet

@Composable
private fun SettingsSheetContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("About CPUSpeed", style = MaterialTheme.typography.headlineSmall)
        Text(
            "v${getVersion()} — Set CPU frequencies on rooted Android devices.\n\n" +
                    "It aims to help you set CPU speed easily for rooted android devices. " +
                    "Please visit the GitHub repository for more info.",
            style = MaterialTheme.typography.bodyMedium
        )
        HorizontalDivider()
        Text("Settings", style = MaterialTheme.typography.titleSmall)
        Text(
            "• All cores: set the same max/min frequency across all cores\n" +
                    "• Per core: set individual frequencies for each CPU core\n" +
                    "• Governor: change CPU frequency scaling governor\n" +
                    "• Apply on boot: automatically re-apply settings after reboot",
            style = MaterialTheme.typography.bodySmall
        )
        HorizontalDivider()
        Text("Links", style = MaterialTheme.typography.titleSmall)
        Text(
            "GitHub: github.com/HenryQuan/CPUSpeed\n" +
                    "Feedback: open a GitHub issue",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// endregion

// region Cards

@Composable
private fun RootStatusCard(isRooted: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRooted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRooted) "✓ Rooted" else "✗ Not Rooted",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isRooted)
                    "CPU frequency control is available"
                else
                    "Root access required for CPU control",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CPUInfoCard(
    info: String,
    coreCount: Int,
    currentGovernor: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("CPU Info", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text("Cores: $coreCount", style = MaterialTheme.typography.bodyMedium)
            Text(info, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Governor: $currentGovernor",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// endregion

// region Sliders

@Composable
private fun ModeToggle(
    perCoreMode: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            if (perCoreMode) "Per-core mode" else "All cores mode",
            style = MaterialTheme.typography.labelLarge
        )
        Switch(
            checked = perCoreMode,
            onCheckedChange = { onToggle() },
            enabled = enabled
        )
    }
}

@Composable
private fun PerCoreSliders(
    coreFrequencies: List<CoreFrequency>,
    coreMaxSliders: List<Float>,
    coreMinSliders: List<Float>,
    enabled: Boolean,
    onMaxChange: (Int, Float) -> Unit,
    onMinChange: (Int, Float) -> Unit,
) {
    coreFrequencies.forEachIndexed { i, cf ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Core $i — ${CPUManager.formatFrequency(cf.curFreq)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                val maxP = coreMaxSliders.getOrElse(i) { 0f }
                val minP = coreMinSliders.getOrElse(i) { 0f }
                val diff = maxOf(cf.maxFreq - cf.minFreq, 1)
                FrequencySlider(
                    label = "Max",
                    freq = (maxP * diff + cf.minFreq).toInt(),
                    percent = maxP,
                    onValueChange = { onMaxChange(i, it) },
                    enabled = enabled
                )
                FrequencySlider(
                    label = "Min",
                    freq = (minP * diff + cf.minFreq).toInt(),
                    percent = minP,
                    onValueChange = { onMinChange(i, it) },
                    enabled = enabled
                )
            }
        }
    }
}

@Composable
private fun FrequencySlider(
    label: String,
    freq: Int,
    percent: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                CPUManager.formatFrequency(freq),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = percent,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        )
    }
}

// endregion

// region Governor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GovernorSelector(
    current: String,
    governors: List<String>,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            value = current,
            onValueChange = {},
            readOnly = true,
            label = { Text("Governor") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            enabled = enabled
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            governors.forEach { gov ->
                DropdownMenuItem(
                    text = { Text(gov) },
                    onClick = {
                        onSelect(gov)
                        expanded = false
                    }
                )
            }
        }
    }
}

// endregion

private fun getVersion(): String = "1.1.0"
