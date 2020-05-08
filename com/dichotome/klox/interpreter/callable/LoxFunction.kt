package com.dichotome.klox.interpreter.callable

import com.dichotome.klox.environment.Environment
import com.dichotome.klox.grammar.Stmt
import com.dichotome.klox.interpreter.Interpreter

class LoxFunction(private val declaration: Stmt.Function) : LoxCallable {

    override val name: String = declaration.name.lexeme

    override val arity: Int = declaration.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any = with(declaration) {
        Environment(interpreter.globals).let { environment ->
            params.forEachIndexed { i, token ->
                environment.define(token.lexeme, arguments[i])
            }
            interpreter.executeBlock(body, environment)
        }

        return Unit
    }

    override fun toString(): String = "<fn $name >"
}