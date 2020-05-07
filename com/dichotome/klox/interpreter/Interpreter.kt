package com.dichotome.klox.interpreter

import com.dichotome.klox.Lox
import com.dichotome.klox.environment.Environment
import com.dichotome.klox.error.RuntimeError
import com.dichotome.klox.grammar.Expr
import com.dichotome.klox.grammar.Stmt
import com.dichotome.klox.scanner.Token
import com.dichotome.klox.scanner.TokenType.*
import java.util.*
import kotlin.math.pow

object Interpreter : Expr.Visitor<Any>, Stmt.Visitor<Unit> {

    private var environment = Environment()

    fun interpret(statements: List<Stmt>) =
        try {
            statements.forEach { it.execute() }
        } catch (e: RuntimeError) {
            Lox.runtimeError(e)
        }

    private fun stringify(value: Any): String =
        when (value) {
            Unit -> "nil"
            is Double -> "$value".removeSuffix(".0")
            else -> "$value"
        }

    private fun Expr.evaluate(): Any = accept(this@Interpreter)

    private fun Stmt.execute(): Any = accept(this@Interpreter)

    private fun Stmt.Block.execute(localEnvironment: Environment) {
        val previousEnvironment = environment
        try {
            environment = localEnvironment
            statements.forEach { it.execute() }
        } finally {
            environment = previousEnvironment
        }
    }

    private fun Stmt.Block.executeWithNewEnvironment() = execute(Environment(environment))

    private fun Any.isTruthy(): Boolean = when (this) {
        is Boolean -> this
        Unit -> false
        0.0 -> false
        1.0 -> true
        else -> true
    }

    private fun Any.isNotTruthy(): Boolean = !isTruthy()

    //region EXPR ------------------------------------------------------------------------------------------------------

    override fun visitNoneExpr() = Unit

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

    override fun visitCommaExpr(comma: Expr.Comma): Any =
        comma.list.map { it.evaluate() }.last()

    override fun visitTernaryExpr(ternary: Expr.Ternary): Any = with(ternary) {
        val condition = first.evaluate()
        if (condition is Boolean) {
            if (condition) second.evaluate() else third.evaluate()
        } else {
            throw RuntimeError(ternary.operator, "The value before '?' must be Boolean")
        }
    }

    override fun visitVariableExpr(variable: Expr.Variable): Any =
        environment[variable.name]

    override fun visitLogicalExpr(logical: Expr.Logical): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitCallExpr(call: Expr.Call): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitFuncExpr(func: Expr.Func): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    //endregion

    //region STMT ------------------------------------------------------------------------------------------------------

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        stmt.expression.evaluate()
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = stmt.expression.evaluate()
        println(stringify(value))
    }

    override fun visitVarStmt(stmt: Stmt.Var) = with(stmt) {
        environment += name
        assignment?.execute() ?: return@with
    }

    override fun visitAssignStmt(assignment: Stmt.Assign) {
        var stmt: Stmt? = assignment
        val names = LinkedList<Token>()

        while (stmt is Stmt.Assign) {
            names.addFirst(stmt.name)
            stmt = stmt.value
        }

        if (stmt is Stmt.Expression?) {
            val value = stmt?.expression?.evaluate() ?: Unit
            names.forEach {
                environment[it] = value
            }
        }
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        stmt.executeWithNewEnvironment()
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitBreakStmt(stmt: Stmt.Break) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    //endregion

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