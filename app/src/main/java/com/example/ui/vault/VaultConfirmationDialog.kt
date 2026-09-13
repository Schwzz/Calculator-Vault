package com.example.ui.vault

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalVaultCornerStyle
import com.example.ui.theme.VaultCardBackground
import com.example.ui.theme.VaultTextSecondary

@Composable
fun VaultConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    cancelText: String = "Cancel",
    isDestructive: Boolean = false,
    confirmTestTag: String = "confirm_dialog_button",
    cancelTestTag: String = "cancel_dialog_button",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val cornerStyle = LocalVaultCornerStyle.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Text(
                text = message,
                color = VaultTextSecondary,
                fontSize = 14.sp
            )
        },
        shape = cornerStyle.dialogShape,
        containerColor = VaultCardBackground,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                shape = cornerStyle.buttonShape,
                modifier = androidx.compose.ui.Modifier.testTag(confirmTestTag)
            ) {
                Text(
                    text = confirmText,
                    color = if (isDestructive) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = cornerStyle.buttonShape,
                modifier = androidx.compose.ui.Modifier.testTag(cancelTestTag)
            ) {
                Text(
                    text = cancelText,
                    color = Color(0xFFA0A0A0)
                )
            }
        }
    )
}
