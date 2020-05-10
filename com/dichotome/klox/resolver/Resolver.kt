package com.dichotome.klox.resolver

import com.dichotome.klox.grammar.Expr
import com.dichotome.klox.grammar.Stmt
import com.dichotome.klox.interpreter.Interpreter
import com.dichotome.klox.scanner.Token
import java.util.*

class Resolver(
    val interpreter: Interpreter
) : Stmt.Visitor<Unit>, Expr.Visitor<Unit> {

    private val scopes = Stack<HashMap<String, Boolean>>()

    //region STMT ------------------------------------------------------------------------------------------------------

    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }


    override fun visitVarStmt(stmt: Stmt.Var) = with(stmt) {
        declare(name.lexeme)
        assignment?.let {
            resolve(it)
        }
        define(name.lexeme)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val name = stmt.name.lexeme
        declare(name)
        define(name)

        resolveFunction(stmt)
    }

    override fun visitAssignStmt(assignment: Stmt.Assign) {
        var stmt: Stmt = assignment
        val tokens = arrayListOf<Token>()
        while (stmt is Stmt.Assign) {
            tokens += stmt.name
            stmt = stmt.value
        }

        val expr = (stmt as Stmt.Expression).expression

        resolve(expr)

        tokens.forEach {
            resolveLocal(expr, it.lexeme)
        }
    }


    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        resolve(stmt.expression)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        with(stmt) {
            resolve(condition)
            resolve(then)
            other?.let {
                resolve(it)
            }
        }
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        with(stmt) {
            resolve(condition)
            resolve(body)
        }
    }

    override fun visitForStmt(stmt: Stmt.For) {
        with(stmt) {
            initializer?.let {
                resolve(it)
            }
            condition?.let {
                resolve(it)
            }
            increment?.let {
                resolve(it)
            }
            resolve(body)
        }
    }

    override fun visitBreakStmt(stmt: Stmt.Break) {
        return
    }

    override fun visitContinueStmt(stmt: Stmt.Continue) {
        return
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        stmt.value?.let {
            resolve(it)
        }
    }

    //endregion

    //region EXPR ------------------------------------------------------------------------------------------------------

    override fun visitVariableExpr(variable: Expr.Variable) = with(variable) {
        val name = name.lexeme
        if (scopes.isNotEmpty() && scopes.peek()[name] == false) {
            resolveLocal(variable, name, true)
        } else {
            resolveLocal(variable, name)
        }
    }

    override fun visitFuncExpr(func: Expr.Function) {
        resolveLambda(func)
    }

    override fun visitNoneExpr() {
        return
    }

    override fun visitBinaryExpr(binary: Expr.Binary) {
        with(binary) {
            resolve(left)
            resolve(right)
        }
    }

    override fun visitCommaExpr(comma: Expr.Comma) {
        comma.list.forEach {
            resolve(it)
        }
    }

    override fun visitTernaryExpr(ternary: Expr.Ternary) {
        with(ternary) {
            resolve(first)
            resolve(second)
            resolve(third)
        }
    }

    override fun visitGroupingExpr(grouping: Expr.Grouping) {
        resolve(grouping.expr)
    }

    override fun visitLiteralExpr(literal: Expr.Literal) {
        return
    }

    override fun visitUnaryExpr(unary: Expr.Unary) {
        resolve(unary.right)
    }

    override fun visitLogicalExpr(logical: Expr.Logical) {
        with(logical) {
            resolve(left)
            resolve(right)
        }
    }

    override fun visitCallExpr(call: Expr.Call) {
        with(call) {
            resolve(callee)
            arguments.forEach {
                resolve(it)
            }
        }
    }

    //endregion

    private fun Expr.evaluate(): Unit = accept(this@Resolver)

    private fun Stmt.execute(): Unit = accept(this@Resolver)

    fun resolve(statements: List<Stmt>) =
        statements.forEach(::resolve)

    private fun resolve(stmt: Stmt) {
        stmt.execute()
    }

    private fun resolve(expr: Expr) {
        expr.evaluate()
    }

    private fun beginScope() {
        scopes += hashMapOf()
    }

    private fun endScope() {
        scopes.pop()
    }

    private fun declare(name: String) {
        if (scopes.isEmpty()) {
            return
        }
        scopes.peek()[name] = false
    }

    private fun define(name: String) {
        if (scopes.isEmpty()) {
            return
        }
        scopes.peek()[name] = true
    }

    private fun resolveLocal(expr: Expr, name: String, isShadowing: Boolean = false) {
        val reversed = scopes.asReversed().toMutableList()

        if (isShadowing) {
            reversed.removeAt(0)
        }
        reversed.forEachIndexed { i, it ->
            if (it.containsKey(name)) {
                interpreter.resolve(expr, i)
                return
            }
        }
    }

    private fun resolveFunction(func: Stmt.Function) {
        beginScope()
        resolveLambda(func.functionExpr)
        endScope()
    }

    private fun resolveLambda(func: Expr.Function) {
        func.params.forEach {
            declare(it.lexeme)
            define(it.lexeme)
        }
        resolve(func.body)
    }
}