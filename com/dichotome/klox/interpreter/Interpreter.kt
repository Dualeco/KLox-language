package com.dichotome.klox.interpreter

import com.dichotome.klox.Lox
import com.dichotome.klox.environment.Environment
import com.dichotome.klox.error.RuntimeError
import com.dichotome.klox.grammar.Expr
import com.dichotome.klox.grammar.Stmt
import com.dichotome.klox.interpreter.callable.LoxCallable
import com.dichotome.klox.interpreter.callable.LoxFunction
import com.dichotome.klox.interpreter.error.BreakError
import com.dichotome.klox.interpreter.error.ContinueError
import com.dichotome.klox.interpreter.error.ReturnError
import com.dichotome.klox.interpreter.native.Native
import com.dichotome.klox.interpreter.native.stringify
import com.dichotome.klox.scanner.Token
import com.dichotome.klox.scanner.TokenType.*
import java.util.*
import kotlin.math.pow

object Interpreter : Expr.Visitor<Any>, Stmt.Visitor<Unit> {

    val globals = Environment()
    private var environment = globals

    init {
        Native.defineAll(this)
    }

    fun interpret(statements: List<Stmt>) =
        try {
            statements.forEach { it.execute() }
        } catch (e: RuntimeError) {
            Lox.runtimeError(e)
        }

    private fun Expr.evaluate(): Any = accept(this@Interpreter)

    private fun Stmt.execute(): Any = accept(this@Interpreter)

    fun executeBlock(block: Stmt.Block, localEnvironment: Environment) {
        val previousEnvironment = environment
        try {
            environment = localEnvironment
            block.statements.forEach { it.execute() }
        } finally {
            environment = previousEnvironment
        }
    }

    fun Stmt.Block.execute(localEnvironment: Environment) =
        executeBlock(this, localEnvironment)

    private fun Stmt.Block.executeWithNewEnvironment() = execute(Environment(environment))

    private fun Any.isTruthy(): Boolean = when (this) {
        is Boolean -> this
        is Expr.Literal -> value?.isTruthy() ?: false
        "true" -> true
        "false" -> false
        Unit -> false
        0.0 -> false
        1.0 -> true
        else -> true
    }

    private fun Any.isNotTruthy(): Boolean = !isTruthy()

    //region EXPR ------------------------------------------------------------------------------------------------------

    override fun visitNoneExpr(): Any {
        return Unit
    }

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
        with(logical) {
            val left = left.evaluate()
            if (operator.type == OR) {
                if (left.isTruthy()) return true
            } else {
                if (left.isNotTruthy()) return false
            }

            return right.evaluate().isTruthy()
        }
    }

    override fun visitCallExpr(call: Expr.Call): Any = with(call) {
        val arguments = arguments.map {
            it.evaluate()
        }

        val callee = callee.evaluate()

        val function = callee as? LoxCallable ?: throw RuntimeError(
            paren, "Only functions and classes are callable"
        )

        if (function.arity != arguments.size) {
            throw RuntimeError(
                paren, "Expected ${function.arity} arguments but got ${arguments.size}."
            )
        }

        return callee.call(this@Interpreter, arguments)
    }

    override fun visitFuncExpr(func: Expr.Function): Any =
        LoxFunction(func, environment)

    //endregion

    //region STMT ------------------------------------------------------------------------------------------------------

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        stmt.expression.evaluate()
    }

    override fun visitVarStmt(stmt: Stmt.Var) = with(stmt) {
        environment.define(name.lexeme)
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
        with(stmt) {
            if (condition.evaluate().isTruthy()) then.execute() else other?.execute()
        }
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        with(stmt) {
            while (condition.evaluate().isTruthy()) {
                try {
                    body.execute()
                } catch (e: BreakError) {
                    break
                } catch (e: ContinueError) {
                    continue
                }
            }
        }
    }

    override fun visitForStmt(stmt: Stmt.For) {
        with(stmt) {
            initializer?.execute()
            while (condition?.evaluate()?.isTruthy() != false) {
                try {
                    body.execute()
                } catch (e: BreakError) {
                    break
                } catch (e: ContinueError) {
                    increment?.execute()
                    continue
                }
                increment?.execute()
            }
        }
    }

    override fun visitBreakStmt(stmt: Stmt.Break) {
        throw BreakError(stmt.breakToken)
    }

    override fun visitContinueStmt(stmt: Stmt.Continue) {
        throw ContinueError(stmt.continueToken)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) = with(stmt) {
        environment.define(name.lexeme, functionExpr.evaluate() as LoxFunction)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) = with(stmt) {
        throw ReturnError(keyword, value?.evaluate() ?: Unit)
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

        if ((left is Double || left is String) && (right is Double || right is String))
            return "${left.stringify()}${right.stringify()}"

        throw RuntimeError(token, "Operands must be Numbers or Strings")
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