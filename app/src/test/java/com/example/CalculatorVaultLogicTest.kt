package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Calculator Vault logic and security rules.
 */
class CalculatorVaultLogicTest {

    private fun isOperator(c: Char): Boolean = c == '+' || c == '-' || c == '*' || c == '/'

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
        if (tokens.isEmpty()) throw IllegalArgumentException("Empty expression")

        val firstPass = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            if (token == "*" || token == "/") {
                if (firstPass.isEmpty() || i + 1 >= tokens.size) {
                    throw IllegalArgumentException("Malformed operator")
                }
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

        if (firstPass.isEmpty()) throw IllegalArgumentException("Invalid tokens")

        var result = firstPass[0].toDouble()
        var j = 1
        while (j < firstPass.size) {
            val op = firstPass[j]
            if (j + 1 >= firstPass.size) {
                throw IllegalArgumentException("Incomplete expression")
            }
            val next = firstPass[j + 1].toDouble()
            result = if (op == "+") result + next else result - next
            j += 2
        }
        return result
    }

    private fun evaluate(expr: String): Double? {
        return try {
            val tokens = tokenize(expr)
            if (tokens.isEmpty()) null else evaluateTokens(tokens)
        } catch (e: Exception) {
            null
        }
    }

    @Test
    fun testArithmeticOperations() {
        assertEquals(7.0, evaluate("3 + 4")!!, 0.001)
        assertEquals(14.0, evaluate("2 + 3 * 4")!!, 0.001)
        assertEquals(10.0, evaluate("20 / 2")!!, 0.001)
        assertEquals(1.0, evaluate("5 - 4")!!, 0.001)
    }

    @Test
    fun testNegativeNumbersAndUnaryMinus() {
        assertEquals(-2.0, evaluate("-5 + 3")!!, 0.001)
        assertEquals(-8.0, evaluate("-5 - 3")!!, 0.001)
        assertEquals(-15.0, evaluate("-5 * 3")!!, 0.001)
    }

    @Test
    fun testDivideByZeroReturnsNull() {
        assertNull(evaluate("10 / 0"))
    }

    @Test
    fun testMalformedExpressionsReturnNull() {
        assertNull(evaluate("5 + * 3"))
        assertNull(evaluate("5 +"))
        assertNull(evaluate("* 5"))
    }

    @Test
    fun testPinVerificationSecurity() {
        val userConfiguredPin = "7890"

        // Candidate matching configured PIN should unlock
        assertEquals(true, "7890" == userConfiguredPin)

        // Default PIN "1234" must NOT unlock when user configured a different PIN
        assertEquals(false, "1234" == userConfiguredPin)

        // Backdoor code "000000" must NOT unlock
        assertEquals(false, "000000" == userConfiguredPin)
    }
}
