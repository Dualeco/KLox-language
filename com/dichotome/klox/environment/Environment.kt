package com.dichotome.klox.environment

import com.dichotome.klox.error.RuntimeError
import com.dichotome.klox.interpreter.callable.LoxCallable
import com.dichotome.klox.scanner.Token

class Environment(private val enclosing: Environment? = null) {

    private val variables: MutableMap<String, Any> = hashMapOf()
    private val overloads: MutableMap<Pair<String, Int>, LoxCallable> = hashMapOf()

    fun define(name: String, value: Any = Unit) {
        variables[name] = value
    }

    fun overload(name: String, arity: Int, value: LoxCallable) {
        overloads[name to arity] = value
    }

    operator fun get(token: Token) = getToken(token)

    operator fun get(distance: Int, token: Token) =
        ancestor(distance).variables[token.lexeme]

    operator fun get(distance: Int, name: String, arity: Int) =
        ancestor(distance).overloads[name to arity] ?: throw RuntimeException(
            "No overload of ${name} with $arity arguments."
        )

    operator fun set(token: Token, value: Any) = assign(token, value)

    private fun getToken(token: Token): Any = token.lexeme.let { name ->
        variables[name] ?: enclosing?.get(token) ?: throw RuntimeError(
            token,
            "Undefined variable '${token.lexeme}'"
        )
    }

    private fun assign(token: Token, value: Any) {
        variables[token.lexeme]?.let {
            variables[token.lexeme] = value
        } ?: enclosing?.assign(token, value) ?: throw RuntimeError(
            token,
            "Undefined variable '${token.lexeme}'"
        )
    }

    private fun ancestor(distance: Int): Environment {
        var environment = this
        for (i in 0 until distance) {
            environment = environment.enclosing ?: throw RuntimeException("Enclosing environment null")
        }

        return environment
    }
}