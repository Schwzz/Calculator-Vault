package com.example.ui.vault

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.calculator.PRESET_QUESTIONS
import com.example.ui.theme.AccentPalettes
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.VaultCardBackground
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import kotlinx.coroutines.launch

@Composable
fun VaultSettingsScreen(
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit,
    onLockApp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showChangePinDialog by remember { mutableStateOf(false) }
    var showUpdateSecurityDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBackground),
        containerColor = VaultBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = VaultCardBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = "Vault Settings",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Analyzer Section
            item {
                Text(
                    text = "STORAGE ANALYZER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val storage = uiState.storageBreakdown
                val total = storage.totalVaultBytes.coerceAtLeast(1L)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = VaultCardBackground),
                    border = BorderStroke(1.dp, VaultCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Total Vault Used", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Text(formatFileSize(storage.totalVaultBytes), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Segmented bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2A2A2A))
                        ) {
                            val photoWeight = (storage.photoBytes.toFloat() / total).coerceAtLeast(0.01f)
                            val videoWeight = (storage.videoBytes.toFloat() / total).coerceAtLeast(0.01f)
                            val audioWeight = (storage.audioBytes.toFloat() / total).coerceAtLeast(0.01f)
                            val fileWeight = (storage.fileBytes.toFloat() / total).coerceAtLeast(0.01f)
                            val dlWeight = (storage.downloadBytes.toFloat() / total).coerceAtLeast(0.01f)

                            Box(modifier = Modifier.weight(photoWeight).fillMaxSize().background(Color(0xFF60A5FA)))
                            Box(modifier = Modifier.weight(videoWeight).fillMaxSize().background(Color(0xFFF87171)))
                            Box(modifier = Modifier.weight(audioWeight).fillMaxSize().background(Color(0xFFFBBF24)))
                            Box(modifier = Modifier.weight(fileWeight).fillMaxSize().background(Color(0xFF34D399)))
                            Box(modifier = Modifier.weight(dlWeight).fillMaxSize().background(Color(0xFF38BDF8)))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        StorageRowItem("Photos", formatFileSize(storage.photoBytes), Color(0xFF60A5FA))
                        StorageRowItem("Videos", formatFileSize(storage.videoBytes), Color(0xFFF87171))
                        StorageRowItem("Audio", formatFileSize(storage.audioBytes), Color(0xFFFBBF24))
                        StorageRowItem("Files & Documents", formatFileSize(storage.fileBytes), Color(0xFF34D399))
                        StorageRowItem("Downloads Cache", formatFileSize(storage.downloadBytes), Color(0xFF38BDF8))
                    }
                }
            }

            // Theme Accent Picker Section
            item {
                Text(
                    text = "THEME CUSTOMIZATION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = VaultCardBackground),
                    border = BorderStroke(1.dp, VaultCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vibrant Accent Color", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text("Personalize the calculator and private vault dark theme aesthetic", color = VaultTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AccentPalettes.forEachIndexed { index, accent ->
                                val isSelected = uiState.accentIndex == index
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(accent.primary)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.setAccentColorIndex(index) }
                                        .testTag("accent_color_$index"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Security Section
            item {
                Text(
                    text = "SECURITY & DISGUISE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = VaultCardBackground),
                    border = BorderStroke(1.dp, VaultCardBorder)
                ) {
                    Column {
                        SettingsClickableRow(
                            icon = Icons.Default.Lock,
                            title = "Change 4-Digit PIN",
                            subtitle = "Requires verification of current PIN",
                            onClick = { showChangePinDialog = true }
                        )
                        SettingsClickableRow(
                            icon = Icons.Default.Security,
                            title = "Security Recovery Question",
                            subtitle = "Setup offline recovery question/answer",
                            onClick = { showUpdateSecurityDialog = true }
                        )
                        SettingsClickableRow(
                            icon = Icons.Default.Visibility,
                            title = "Lock Vault Now",
                            subtitle = "Return instantly to deceptive calculator",
                            onClick = onLockApp
                        )
                    }
                }
            }

            // Disguise Info Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = VaultBackground),
                    border = BorderStroke(1.dp, VaultCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Disguise Quick Guide", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "• The app launches as a 100% normal functional calculator.\n" +
                            "• Standard arithmetic (+, -, ×, ÷) evaluates properly.\n" +
                            "• Type your 4-digit PIN and press '=' to unlock your private vault.\n" +
                            "• Long-press '=' if you ever forget your PIN to access offline recovery.",
                            color = VaultTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        var currentPinInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }
        var confirmPinInput by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { showChangePinDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                color = VaultCardBackground,
                border = BorderStroke(1.dp, VaultCardBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Change 4-Digit PIN", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) currentPinInput = it },
                        label = { Text("Current PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = VaultCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinInput = it },
                        label = { Text("New 4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = VaultCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPinInput = it },
                        label = { Text("Confirm New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = VaultCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showChangePinDialog = false }) {
                            Text("Cancel", color = Color(0xFFA0A0A0))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (currentPinInput != viewModel.prefs.pin) {
                                    errorMsg = "Incorrect current PIN."
                                } else if (newPinInput.length != 4) {
                                    errorMsg = "New PIN must be 4 digits."
                                } else if (newPinInput != confirmPinInput) {
                                    errorMsg = "PINs do not match."
                                } else {
                                    viewModel.prefs.pin = newPinInput
                                    showChangePinDialog = false
                                    scope.launch { snackbarHostState.showSnackbar("PIN updated successfully") }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save PIN", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }

    // Security Question Update Dialog
    if (showUpdateSecurityDialog) {
        var selectedQ by remember { mutableStateOf(viewModel.prefs.securityQuestion.ifEmpty { PRESET_QUESTIONS[0] }) }
        var answerInput by remember { mutableStateOf(viewModel.prefs.securityAnswer) }
        var dropdownExpanded by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showUpdateSecurityDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                color = VaultCardBackground,
                border = BorderStroke(1.dp, VaultCardBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Security Recovery Question", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedQ,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.clickable { dropdownExpanded = true }
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = VaultCardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().clickable { dropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(VaultCardBackground)
                        ) {
                            PRESET_QUESTIONS.forEach { q ->
                                DropdownMenuItem(
                                    text = { Text(q, color = Color.White) },
                                    onClick = {
                                        selectedQ = q
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = answerInput,
                        onValueChange = { answerInput = it },
                        label = { Text("Security Answer") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = VaultCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showUpdateSecurityDialog = false }) {
                            Text("Cancel", color = Color(0xFFA0A0A0))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (answerInput.trim().isNotEmpty()) {
                                    viewModel.prefs.securityQuestion = selectedQ
                                    viewModel.prefs.securityAnswer = answerInput.trim()
                                    showUpdateSecurityDialog = false
                                    scope.launch { snackbarHostState.showSnackbar("Security recovery question saved") }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StorageRowItem(label: String, sizeText: String, dotColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, color = VaultTextSecondary, fontSize = 13.sp)
        }
        Text(sizeText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SettingsClickableRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = VaultTextSecondary, fontSize = 12.sp)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF666666))
    }
}
