package com.dichotome.klox.interpreter.callable

import com.dichotome.klox.environment.Environment
import com.dichotome.klox.grammar.Expr
import com.dichotome.klox.interpreter.Interpreter
import com.dichotome.klox.interpreter.error.ReturnError

class LoxFunction(
    private val expression: Expr.Function,
    private val closure: Environment
) : LoxCallable {

    override val name: String = expression.name

    override val arity: Int = expression.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any = with(expression) {
        Environment(closure).let { environment ->
            params.forEachIndexed { i, token ->
                environment.define(token.lexeme, arguments[i])
            }
            try {
                interpreter.executeBlock(body, Environment(environment))
            } catch (returnError: ReturnError) {
                return@with returnError.value
            }
        }

        return Unit
    }

    override fun toString(): String = "<fn $name >"
}