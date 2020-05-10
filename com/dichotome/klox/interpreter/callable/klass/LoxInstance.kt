package com.dichotome.klox.interpreter.callable.klass

import com.dichotome.klox.error.RuntimeError
import com.dichotome.klox.scanner.Token

class LoxInstance(
    private val klass: LoxClass
) {
    private val properties = hashMapOf<String, Any>()

    operator fun get(token: Token): Any = when {
        properties.containsKey(token.lexeme) -> properties[token.lexeme]!!
        else -> klass.findMethod(token.lexeme)?.bind(this) ?: throw RuntimeError(
            token, "Undefined property ${token.lexeme}"
        )
    }

    operator fun set(token: Token, value: Any) {
        properties[token.lexeme] = value
    }

    override fun toString(): String = "<inst ${klass.name}>"
}