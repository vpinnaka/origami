package com.origami.assistant.agent.tools

import com.origami.assistant.agent.model.Tool
import com.origami.assistant.agent.model.ToolParameter
import javax.inject.Inject
import javax.inject.Singleton
import javax.script.ScriptEngineManager

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
        return try {
            val sanitized = expression
                .replace("^", "**")
                .replace("×", "*")
                .replace("÷", "/")
            val engine = ScriptEngineManager().getEngineByName("rhino")
                ?: return evalSimple(sanitized)
            val result = engine.eval(sanitized)
            "= $result"
        } catch (e: Exception) {
            "Calculation error: ${e.message}"
        }
    }

    private fun evalSimple(expr: String): String = try {
        val result = expr.trim().toDoubleOrNull()
        if (result != null) "= $result" else "= (unable to evaluate: $expr)"
    } catch (e: Exception) {
        "Calculation error: ${e.message}"
    }
}
