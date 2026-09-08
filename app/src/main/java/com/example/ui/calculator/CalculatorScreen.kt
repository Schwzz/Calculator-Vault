package com.example.ui.calculator

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CalcActionKeyBg
import com.example.ui.theme.CalcNumKeyBg
import com.example.ui.theme.CalcOperatorKeyBg
import com.example.ui.theme.VaultBackground
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    onNavigateToVault: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.refreshState()
        viewModel.navEvents.collectLatest { event ->
            when (event) {
                is CalculatorNavEvent.UnlockVault -> {
                    onNavigateToVault()
                }
                is CalculatorNavEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBackground),
        color = VaultBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Calculator Display Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    if (state.history.isNotEmpty()) {
                        Text(
                            text = state.history,
                            color = Color(0xFF888888),
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    val displayText = state.expression
                    val fontSize = when {
                        displayText.length > 12 -> 34.sp
                        displayText.length > 8 -> 44.sp
                        else -> 56.sp
                    }

                    Text(
                        text = displayText,
                        color = Color.White,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .testTag("calculator_display")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Keypad rows
                val keyRows = listOf(
                    listOf("C", "±", "%", "÷"),
                    listOf("7", "8", "9", "×"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "+"),
                    listOf("0", ".", "⌫", "=")
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    keyRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { key ->
                                CalcButton(
                                    key = key,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.onKey(key) },
                                    onLongClick = {
                                        if (key == "=") {
                                            viewModel.onLongPressEquals()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )
        }
    }

    // Modal Overlays
    if (state.showPinSetup) {
        PinSetupDialog(
            onDismiss = {},
            onConfirm = { pin, question, answer ->
                viewModel.completePinSetup(pin, question, answer)
            }
        )
    }

    if (state.showRecovery) {
        PinRecoveryDialog(
            question = state.securityQuestion,
            onDismiss = { viewModel.dismissRecovery() },
            onRecover = { answer, newPin ->
                viewModel.recoverPin(answer, newPin)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalcButton(
    key: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val isEquals = key == "="
    val isOperator = key in listOf("÷", "×", "-", "+")
    val isAction = key in listOf("C", "±", "%", "⌫")

    val bgColor = when {
        isEquals -> MaterialTheme.colorScheme.primary
        isOperator -> CalcOperatorKeyBg
        isAction -> CalcActionKeyBg
        else -> CalcNumKeyBg
    }

    val textColor = when {
        isEquals -> MaterialTheme.colorScheme.onPrimary
        isOperator -> MaterialTheme.colorScheme.primary
        isAction -> Color(0xFFE2E8F0)
        else -> Color.White
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(bgColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("calc_key_$key"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key,
            color = textColor,
            fontSize = if (key.length > 1) 22.sp else 28.sp,
            fontWeight = if (isEquals || isOperator) FontWeight.Bold else FontWeight.Medium
        )
    }
}
