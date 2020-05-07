package com.dichotome.klox.environment

import com.dichotome.klox.error.RuntimeError
import com.dichotome.klox.scanner.Token

class Environment {
    private val variables: MutableMap<String, Any> = hashMapOf()

    operator fun get(token: Token) = getVariable(token)

    operator fun plusAssign(token: Token) = defineVariable(token.lexeme)

    operator fun set(token: Token, value: Any) = assignVariable(token, value)

    private fun getVariable(token: Token): Any =
        variables[token.lexeme] ?: throw RuntimeError(token, "Undefined variable '${token.lexeme}'")

    private fun defineVariable(name: String) {
        variables[name] = Unit
    }

    private fun assignVariable(token: Token, value: Any) {
        getVariable(token)

        // if variable is defined
        variables[token.lexeme] = value
    }
}