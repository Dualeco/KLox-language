package com.dichotome.klox.interpreter.callable

import com.dichotome.klox.environment.Environment
import com.dichotome.klox.grammar.Expr
import com.dichotome.klox.grammar.Stmt
import com.dichotome.klox.interpreter.Interpreter
import com.dichotome.klox.interpreter.callable.klass.LoxInstance
import com.dichotome.klox.interpreter.error.ReturnError

class LoxFunction(
    private val expression: Expr.Function,
    private val closure: Environment,
    private val isInitializer: Boolean
) : LoxCallable {

    override val name: String = expression.name

    override val arity: Int = expression.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any = with(expression) {
        Environment(closure).let { environment ->
            params.forEachIndexed { i, token ->
                environment.define(token.lexeme, arguments[i])
            }
            try {
                val block = if (body is Stmt.Block) {
                    body
                } else {
                    Stmt.Block(listOf(body))
                }
                interpreter.executeBlock(block, Environment(environment))

            } catch (returnError: ReturnError) {
                return@with if (isInitializer) {
                    environment[0, "this"]!!
                } else {
                    returnError.value
                }
            }

            if (isInitializer)
                return closure[0, "this"]!!
        }

        return Unit
    }

    fun bind(instance: LoxInstance): LoxFunction =
        Environment(closure).let { environment ->
            environment.define("this", instance)

            LoxFunction(expression, environment, isInitializer)
        }

    override fun toString(): String = "<fn $name >"
}