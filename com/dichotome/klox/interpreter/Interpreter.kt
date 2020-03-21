package com.dichotome.klox.interpreter

import com.dichotome.klox.error.RuntimeError
import com.dichotome.klox.grammar.Expr
import com.dichotome.klox.scanner.Token
import com.dichotome.klox.scanner.TokenType.*
import kotlin.math.pow

class Interpreter : Expr.Visitor<Any> {

    fun interpret(expr: Expr): String =
        stringify(expr.evaluate())

    private fun stringify(value: Any): String =
        when (value) {
            Unit -> "nil"
            is Double -> "$value".removeSuffix(".0")
            else -> "$value"
        }

    private fun Expr.evaluate(): Any = accept(this@Interpreter)

    private fun Any.isTruthy(): Boolean = when (this) {
        is Boolean -> this
        Unit -> false
        0.0 -> false
        1.0 -> true
        else -> true
    }

    private fun Any.isNotTruthy(): Boolean = !isTruthy()

    override fun visitLiteralExpr(literal: Expr.Literal): Any = literal.value ?: Unit

    override fun visitGroupingExpr(grouping: Expr.Grouping): Any = grouping.expr.evaluate()

    override fun visitUnaryExpr(unary: Expr.Unary): Any {
        val token = unary.operator

        val right: Any = unary.right.evaluate()

        return when (unary.operator.type) {
            BANG -> right.isNotTruthy()
            MINUS -> negate(right, token)

            else -> Unit
        }
    }

    override fun visitBinaryExpr(binary: Expr.Binary): Any {
        val token = binary.operator

        val left: Any = binary.left.evaluate()
        val right: Any = binary.right.evaluate()

        return when (binary.operator.type) {
            BANG_EQUAL -> left != right
            EQUAL_EQUAL -> left == right

            GREATER -> greater(left, right, token)
            GREATER_EQUAL -> greaterEqual(left, right, token)
            LESS -> less(left, right, token)
            LESS_EQUAL -> lessEqual(left, right, token)

            PLUS -> plus(left, right, token)
            MINUS -> minus(left, right, token)
            SLASH -> div(left, right, token)
            STAR -> times(left, right, token)
            HAT -> pow(left, right, token)
            MOD -> mod(left, right, token)

            else -> Unit
        }
    }

    override fun visitCommaExpr(comma: Expr.Comma): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitTernaryExpr(ternary: Expr.Ternary): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitVariableExpr(variable: Expr.Variable): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitAssignExpr(assign: Expr.Assign): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitLogicalExpr(logical: Expr.Logical): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitCallExpr(call: Expr.Call): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitFuncExpr(func: Expr.Func): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun negate(right: Any, token: Token): Any {
        if (right is Double)
            return -right

        throw RuntimeError(token, "Operand must be a Number")
    }

    private fun greater(left: Any, right: Any, token: Token): Boolean {
        if (left is Double && right is Double)
            return left > right

        throw RuntimeError(token, "Operands must be two Numbers")
    }

    private fun greaterEqual(left: Any, right: Any, token: Token): Boolean {
        if (left is Double && right is Double)
            return left >= right

        throw RuntimeError(token, "Operands must be two Numbers")
    }

    private fun less(left: Any, right: Any, token: Token): Boolean {
        if (left is Double && right is Double)
            return left < right

        throw RuntimeError(token, "Operands must be two Numbers")
    }

    private fun lessEqual(left: Any, right: Any, token: Token): Boolean {
        if (left is Double && right is Double)
            return left <= right

        throw RuntimeError(token, "Operands must be two Numbers")
    }

    private fun plus(left: Any, right: Any, token: Token): Any {
        if (left is Double && right is Double)
            return left + right

        if (left is String && right is String)
            return left + right

        throw RuntimeError(token, "Operands must be two Numbers or Strings")
    }

    private fun minus(left: Any, right: Any, token: Token): Any {
        if (left is Double && right is Double)
            return left - right

        throw RuntimeError(token, "Operands must be two Numbers")
    }

    private fun div(left: Any, right: Any, token: Token): Any {
        if (left is Double && right is Double)
            if (right == 0.0)
                throw RuntimeError(token, "Division by zero")
            else
                return left / right

        throw RuntimeError(token, "Operands must be two Numbers")
    }

    private fun times(left: Any, right: Any, token: Token): Any {
        if (left is Double && right is Double)
            return left * right

        if (left is String && right is Double)
            return left.repeat(right.toInt())

        throw RuntimeError(token, "Operands must be two Numbers")
    }

    private fun pow(left: Any, right: Any, token: Token): Any {
        if (left is Double && right is Double)
            return left.pow(right)

        throw RuntimeError(token, "Operands must be two Numbers")
    }

    private fun mod(left: Any, right: Any, token: Token): Any {
        if (left is Double && right is Double)
            if (right == 0.0)
                throw RuntimeError(token, "Division by zero")
            else
                return left % right

        throw RuntimeError(token, "Operands must be two Numbers")
    }
}