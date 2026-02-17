package com.yihengquan.cpuspeed

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CPUSpeedTheme {
                CPUSpeedApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CPUSpeedApp(viewModel: CPUViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val maxSpeed by viewModel.maxSpeed.collectAsState()
    val minSpeed by viewModel.minSpeed.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showWelcomeDialog by remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CPUSpeed") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("About") },
                            onClick = {
                                showMenu = false
                                showAboutDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Info, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = {
                                showMenu = false
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, 
                                        "https://play.google.com/store/apps/details?id=com.yihengquan.cpuspeed")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share CPUSpeed"))
                            },
                            leadingIcon = { Icon(Icons.Default.Share, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Send Feedback") },
                            onClick = {
                                showMenu = false
                                val intent = Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/HenryQuan/CPUSpeed/issues/new"))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is CPUViewModel.UIState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is CPUViewModel.UIState.Error -> {
                ErrorScreen(
                    message = state.message,
                    modifier = Modifier.padding(padding)
                )
            }
            is CPUViewModel.UIState.Success -> {
                CPUControlScreen(
                    cpuInfo = state.cpuInfo,
                    maxSpeed = maxSpeed,
                    minSpeed = minSpeed,
                    onMaxSpeedChange = viewModel::updateMaxSpeed,
                    onMinSpeedChange = viewModel::updateMinSpeed,
                    onApplySpeed = {
                        viewModel.applyCPUSpeed(
                            onSuccess = {
                                Toast.makeText(context, "CPU speed updated successfully", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
    
    // Welcome Dialog
    if (showWelcomeDialog) {
        WelcomeDialog(onDismiss = { showWelcomeDialog = false })
    }
    
    // About Dialog
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
fun ErrorScreen(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Device Not Supported",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun CPUControlScreen(
    cpuInfo: CPUManager.CPUInfo,
    maxSpeed: Float,
    minSpeed: Float,
    onMaxSpeedChange: (Float) -> Unit,
    onMinSpeedChange: (Float) -> Unit,
    onApplySpeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // CPU Info Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "CPU Information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider()
                
                // Display CPU cores and frequencies
                cpuInfo.speedInfo.forEach { (freq, count) ->
                    val ghz = freq.toFloat() / 1000000
                    Text(
                        text = String.format(Locale.US, "%d × %.2f GHz", count, ghz),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                Text(
                    text = String.format(
                        Locale.US,
                        "Range: %d MHz - %d MHz",
                        cpuInfo.minFreqInfo,
                        cpuInfo.maxFreqInfo
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Max Frequency Slider
        SliderCard(
            title = "Max Frequency",
            value = maxSpeed,
            valueRange = cpuInfo.minFreqInfo.toFloat()..cpuInfo.maxFreqInfo.toFloat(),
            onValueChange = onMaxSpeedChange
        )
        
        // Min Frequency Slider
        SliderCard(
            title = "Min Frequency",
            value = minSpeed,
            valueRange = cpuInfo.minFreqInfo.toFloat()..cpuInfo.maxFreqInfo.toFloat(),
            onValueChange = onMinSpeedChange
        )
        
        // Apply Button
        Button(
            onClick = onApplySpeed,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Apply CPU Speed")
        }
        
        // Warning Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "⚠️ Warning",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Underclocking may cause freezes or shutdowns. Overclocking increases heat and battery drain.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun SliderCard(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.US, "%d MHz", value.toInt()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun WelcomeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Welcome to CPUSpeed") },
        text = {
            Text(
                "Thank you for downloading this app.\n\n" +
                "Please note that if you underclock your device, it might freeze or even shutdown. " +
                "If you overclock your device, it might become warm and battery will run out quickly.\n\n" +
                "This app might not work on your device. In this case, you can use other apps."
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("I Understand")
            }
        }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CPUSpeed v1.1.0") },
        text = {
            Text(
                "It aims to help you set CPU speed easily for rooted Android devices. " +
                "Please visit the GitHub repository for more info."
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/HenryQuan/CPUSpeed"))
                    context.startActivity(intent)
                    onDismiss()
                }
            ) {
                Text("GitHub")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/HenryQuan/CPUSpeed/blob/master/Privacy%20Policy.md"))
                    context.startActivity(intent)
                    onDismiss()
                }
            ) {
                Text("Privacy Policy")
            }
        }
    )
}

@Composable
fun CPUSpeedTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = dynamicDarkColorScheme(LocalContext.current),
        content = content
    )
}
