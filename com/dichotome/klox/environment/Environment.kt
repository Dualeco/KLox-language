package com.dichotome.klox.environment

import com.dichotome.klox.error.RuntimeError
import com.dichotome.klox.scanner.Token

object Environment {
    private val variables: MutableMap<String, Any> = hashMapOf()

    fun define(name: String, value: Any) {
        variables[name] = value
    }

    fun get(token: Token): Any =
        variables[token.lexeme] ?: throw RuntimeError(token, "Undefined variable '${token.lexeme}'")

    fun assign(token: Token, value: Any) =
        variables[token.lexeme]?.let {
            variables[token.lexeme] = value
        } ?: throw RuntimeError(token, "Undefined variable '${token.lexeme}'")
}