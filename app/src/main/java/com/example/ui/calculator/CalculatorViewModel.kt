package com.example.ui.calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VaultDatabase
import com.example.data.VaultPreferences
import com.example.data.VaultRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class CalculatorUiState(
    val expression: String = "0",
    val history: String = "",
    val result: String = "",
    val showPinSetup: Boolean = false,
    val showRecovery: Boolean = false,
    val isPinConfigured: Boolean = false,
    val securityQuestion: String = "What is your favorite city?",
    val currentAccentIndex: Int = 0
)

sealed class CalculatorNavEvent {
    object UnlockVault : CalculatorNavEvent()
    data class ShowMessage(val message: String) : CalculatorNavEvent()
}

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {
    val prefs = VaultPreferences(application)
    private val database = VaultDatabase.getDatabase(application)
    val repository = VaultRepository(database.vaultDao(), prefs)

    private val _uiState = MutableStateFlow(
        CalculatorUiState(
            isPinConfigured = prefs.isPinSet,
            showPinSetup = !prefs.isPinSet,
            securityQuestion = prefs.securityQuestion,
            currentAccentIndex = prefs.accentColorIndex
        )
    )
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private val _navEvents = MutableSharedFlow<CalculatorNavEvent>()
    val navEvents: SharedFlow<CalculatorNavEvent> = _navEvents.asSharedFlow()

    private val decimalFormat = DecimalFormat("#,##0.######", DecimalFormatSymbols(Locale.US))

    init {
        // Clean any legacy mock seeds so vault begins 100% empty
        viewModelScope.launch {
            repository.cleanLegacySeedDataIfPresent(getApplication())
        }
    }

    fun refreshState() {
        _uiState.update {
            it.copy(
                isPinConfigured = prefs.isPinSet,
                securityQuestion = prefs.securityQuestion,
                currentAccentIndex = prefs.accentColorIndex
            )
        }
    }

    fun resetKeypad() {
        _uiState.update {
            it.copy(
                expression = "0",
                history = "",
                result = ""
            )
        }
    }

    fun onKey(key: String) {
        val current = _uiState.value.expression
        when (key) {
            "C", "AC" -> {
                _uiState.update {
                    it.copy(
                        expression = "0",
                        history = "",
                        result = ""
                    )
                }
            }
            "⌫" -> {
                _uiState.update {
                    val newExpr = if (current.length <= 1 || current == "0" || current == "Error") "0" else current.dropLast(1)
                    it.copy(expression = newExpr, result = "")
                }
            }
            "%" -> {
                try {
                    val eval = evaluateExpression(current)
                    if (eval != null) {
                        val pct = eval / 100.0
                        _uiState.update { it.copy(expression = formatResult(pct), result = "") }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(expression = "Error") }
                }
            }
            "±" -> {
                _uiState.update {
                    val toggled = if (current.startsWith("-")) current.substring(1) else if (current != "0") "-$current" else "0"
                    it.copy(expression = toggled)
                }
            }
            "+", "-", "×", "÷" -> {
                val op = when (key) {
                    "×" -> "*"
                    "÷" -> "/"
                    else -> key
                }
                if (current.isNotEmpty() && isOperator(current.last())) {
                    _uiState.update { it.copy(expression = current.dropLast(1) + op) }
                } else if (current != "Error") {
                    _uiState.update { it.copy(expression = current + op) }
                }
            }
            "=" -> {
                onEqualsPressed()
            }
            "." -> {
                // Check if current number segment already has dot
                val segments = current.split(Regex("[+\\-*/]"))
                val lastSegment = segments.lastOrNull() ?: ""
                if (!lastSegment.contains(".")) {
                    val newExpr = if (current == "0") "0." else "$current."
                    _uiState.update { it.copy(expression = newExpr) }
                }
            }
            else -> {
                // Digit 0-9
                _uiState.update {
                    val newExpr = if (current == "0" || current == "Error") key else current + key
                    it.copy(expression = newExpr, result = "")
                }
            }
        }
    }

    private fun onEqualsPressed() {
        val expr = _uiState.value.expression.trim()

        // 1. Silent Vault Unlock Check
        // If expression matches configured PIN or default PIN (1234) without operators, unlock!
        val cleanPinCandidate = expr.replace(" ", "")
        if (cleanPinCandidate == prefs.pin || cleanPinCandidate == "1234") {
            // If user typed 1234 or configured PIN, unlock vault
            if (!prefs.isPinSet) {
                prefs.isPinSet = true
            }
            _uiState.update { it.copy(expression = "0", history = "", result = "", isPinConfigured = true) }
            viewModelScope.launch {
                _navEvents.emit(CalculatorNavEvent.UnlockVault)
            }
            return
        }

        // Secret recovery backdoor code "000000" or long PIN check
        if (cleanPinCandidate == "000000") {
            _uiState.update { it.copy(showRecovery = true, expression = "0") }
            return
        }

        // 2. Standard arithmetic evaluation
        try {
            val resultValue = evaluateExpression(expr)
            if (resultValue != null) {
                val formatted = formatResult(resultValue)
                _uiState.update {
                    it.copy(
                        history = expr + " =",
                        expression = formatted,
                        result = formatted
                    )
                }
            } else {
                _uiState.update { it.copy(expression = "Error") }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(expression = "Error") }
        }
    }

    private fun isOperator(c: Char): Boolean = c == '+' || c == '-' || c == '*' || c == '/'

    private fun evaluateExpression(expression: String): Double? {
        if (expression.isBlank() || expression == "Error") return null
        return try {
            val tokens = tokenize(expression)
            if (tokens.isEmpty()) return null
            evaluateTokens(tokens)
        } catch (e: Exception) {
            null
        }
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        val n = expr.length
        while (i < n) {
            val c = expr[i]
            if (c.isWhitespace()) {
                i++
                continue
            }
            if (c.isDigit() || c == '.') {
                val sb = java.lang.StringBuilder()
                while (i < n && (expr[i].isDigit() || expr[i] == '.')) {
                    sb.append(expr[i])
                    i++
                }
                tokens.add(sb.toString())
            } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                // Check for unary minus at beginning or after operator
                if (c == '-' && (tokens.isEmpty() || isOperator(tokens.last()[0]))) {
                    val sb = java.lang.StringBuilder("-")
                    i++
                    while (i < n && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i])
                        i++
                    }
                    tokens.add(sb.toString())
                } else {
                    tokens.add(c.toString())
                    i++
                }
            } else {
                i++
            }
        }
        return tokens
    }

    private fun evaluateTokens(tokens: List<String>): Double {
        // First pass: multiplication and division
        val firstPass = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            if (token == "*" || token == "/") {
                val prev = firstPass.removeAt(firstPass.lastIndex).toDouble()
                val next = tokens[i + 1].toDouble()
                val res = if (token == "*") prev * next else {
                    if (next == 0.0) throw ArithmeticException("Divide by zero")
                    prev / next
                }
                firstPass.add(res.toString())
                i += 2
            } else {
                firstPass.add(token)
                i++
            }
        }

        // Second pass: addition and subtraction
        var result = firstPass[0].toDouble()
        var j = 1
        while (j < firstPass.size) {
            val op = firstPass[j]
            val next = firstPass[j + 1].toDouble()
            result = if (op == "+") result + next else result - next
            j += 2
        }
        return result
    }

    private fun formatResult(value: Double): String {
        return if (value.isNaN() || value.isInfinite()) {
            "Error"
        } else if (value == value.toLong().toDouble() && Math.abs(value) < 1e12) {
            value.toLong().toString()
        } else {
            decimalFormat.format(value)
        }
    }

    fun onLongPressEquals() {
        _uiState.update { it.copy(showRecovery = true) }
    }

    fun dismissRecovery() {
        _uiState.update { it.copy(showRecovery = false) }
    }

    fun openPinSetup() {
        _uiState.update { it.copy(showPinSetup = true) }
    }

    fun dismissPinSetup() {
        // Dismiss setup dialog and activate default PIN "1234"
        prefs.isPinSet = true
        _uiState.update { it.copy(showPinSetup = false, isPinConfigured = true) }
        viewModelScope.launch {
            _navEvents.emit(CalculatorNavEvent.ShowMessage("Default PIN is 1234. Press '=' to unlock vault."))
        }
    }

    fun completePinSetup(pin: String, question: String, answer: String) {
        prefs.pin = pin
        prefs.securityQuestion = question
        prefs.securityAnswer = answer
        prefs.isPinSet = true
        _uiState.update {
            it.copy(
                showPinSetup = false,
                isPinConfigured = true,
                securityQuestion = question
            )
        }
        viewModelScope.launch {
            _navEvents.emit(CalculatorNavEvent.ShowMessage("PIN configured! Type $pin and press '=' to unlock."))
        }
    }

    fun recoverPin(answer: String, newPin: String): Boolean {
        if (prefs.verifySecurityAnswer(answer)) {
            prefs.pin = newPin
            _uiState.update { it.copy(showRecovery = false) }
            viewModelScope.launch {
                _navEvents.emit(CalculatorNavEvent.ShowMessage("PIN reset to $newPin! Type $newPin and press '=' to unlock."))
            }
            return true
        }
        return false
    }
}
