package com.origami.assistant.agent.tools

import com.origami.assistant.agent.model.Tool
import com.origami.assistant.agent.model.ToolParameter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalculatorTool @Inject constructor() : BaseTool() {

    override val definition = Tool(
        name = "calculate",
        description = "Evaluate a mathematical expression. Use for arithmetic, unit conversion, or any precise numerical calculation.",
        parameters = listOf(
            ToolParameter("expression", "string", "Mathematical expression to evaluate, e.g. '2 * (3 + 4) / 7'", required = true)
        )
    )

    override suspend fun execute(params: Map<String, Any>): String {
        val expression = param(params, "expression")
            .replace("×", "*")
            .replace("÷", "/")
            .trim()
        return try {
            val result = parseExpr(tokenize(expression), intArrayOf(0))
            "= $result"
        } catch (e: Exception) {
            "Calculation error: ${e.message}"
        }
    }

    private fun parseExpr(tokens: List<String>, pos: IntArray): Double {
        var left = parseTerm(tokens, pos)
        while (pos[0] < tokens.size && (tokens[pos[0]] == "+" || tokens[pos[0]] == "-")) {
            val op = tokens[pos[0]++]
            val right = parseTerm(tokens, pos)
            left = if (op == "+") left + right else left - right
        }
        return left
    }

    private fun parseTerm(tokens: List<String>, pos: IntArray): Double {
        var left = parseFactor(tokens, pos)
        while (pos[0] < tokens.size && (tokens[pos[0]] == "*" || tokens[pos[0]] == "/")) {
            val op = tokens[pos[0]++]
            val right = parseFactor(tokens, pos)
            left = if (op == "*") left * right else {
                if (right == 0.0) error("Division by zero")
                left / right
            }
        }
        return left
    }

    private fun parseFactor(tokens: List<String>, pos: IntArray): Double {
        if (pos[0] >= tokens.size) error("Unexpected end of expression")
        return when (val tok = tokens[pos[0]]) {
            "-" -> { pos[0]++; -parseFactor(tokens, pos) }
            "+" -> { pos[0]++; parseFactor(tokens, pos) }
            "(" -> {
                pos[0]++
                val v = parseExpr(tokens, pos)
                if (pos[0] >= tokens.size || tokens[pos[0]] != ")") error("Missing ')'")
                pos[0]++
                v
            }
            else -> {
                pos[0]++
                tok.toDoubleOrNull() ?: error("Invalid token: '$tok'")
            }
        }
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            when {
                expr[i].isWhitespace() -> i++
                expr[i].isDigit() || expr[i] == '.' -> {
                    val start = i
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                    tokens.add(expr.substring(start, i))
                }
                expr[i] in "+-*/()" -> tokens.add(expr[i++].toString())
                else -> error("Invalid character '${expr[i]}'")
            }
        }
        return tokens
    }
}
