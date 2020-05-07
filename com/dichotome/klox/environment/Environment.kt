package com.dichotome.klox.environment

import com.dichotome.klox.error.RuntimeError
import com.dichotome.klox.scanner.Token

class Environment(private val enclosing: Environment? = null) {

    private val variables: MutableMap<String, Any> = hashMapOf()

    operator fun get(token: Token) = getVariable(token)

    operator fun plusAssign(token: Token) = defineVariable(token.lexeme)

    operator fun set(token: Token, value: Any) = assignVariable(token, value)

    private fun getVariable(token: Token): Any = token.lexeme.let { name ->
        variables[name] ?: enclosing?.getVariable(token) ?: throw RuntimeError(
            token,
            "Undefined variable '${token.lexeme}'"
        )
    }

    private fun defineVariable(name: String) {
        variables[name] = Unit
    }

    private fun assignVariable(token: Token, value: Any) {
        variables[token.lexeme]?.let {
            variables[token.lexeme] = value
        } ?: enclosing?.assignVariable(token, value) ?: throw RuntimeError(
            token,
            "Undefined variable '${token.lexeme}'"
        )
    }
}