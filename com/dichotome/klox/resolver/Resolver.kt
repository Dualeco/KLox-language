package com.dichotome.klox.resolver

import com.dichotome.klox.Lox
import com.dichotome.klox.grammar.Expr
import com.dichotome.klox.grammar.Stmt
import com.dichotome.klox.interpreter.Interpreter
import com.dichotome.klox.resolver.DeclarationState.*
import com.dichotome.klox.scanner.Token
import java.util.*


class Resolver(
    val interpreter: Interpreter
) : Stmt.Visitor<Unit>, Expr.Visitor<Unit> {

    private val scopes = Stack<HashMap<String, DeclarationState>>()
    private var currentFunction = LoxFunctionType.NONE
    private var currentClass = LoxClassType.NONE
    private var isInsideLoopBody = false

    //region STMT ------------------------------------------------------------------------------------------------------

    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()

        val currentScope = scopes.peek()
        val variables = arrayListOf<Token>()
        val functions = arrayListOf<Token>()
        val classes = arrayListOf<Token>()
        stmt.statements.forEach {
            when (it) {
                is Stmt.Var -> variables += it.name
                is Stmt.Function -> functions += it.name
                is Stmt.Class -> classes += it.name
                else -> {
                }
            }
        }

        resolve(stmt.statements)
        endScope()

        variables.forEach {
            if (currentScope[it.lexeme] != USED) {
                Lox.error(it, "Variable `${it.lexeme}` is never used")
            }
        }
        functions.forEach {
            if (currentScope[it.lexeme] != USED) {
                Lox.error(it, "Function `${it.lexeme}` is never used")
            }
        }
        classes.forEach {
            if (currentScope[it.lexeme] != USED) {
                Lox.error(it, "Class `${it.lexeme}` is never used")
            }
        }
    }


    override fun visitVarStmt(stmt: Stmt.Var) = with(stmt) {
        declare(name)
        assignment?.let {
            resolve(it)
        }
        define(name)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)

        resolveFunction(stmt, LoxFunctionType.FUNCTION)
    }

    override fun visitSetStmt(set: Stmt.Set) {
        assignChain(set)
    }

    override fun visitAssignStmt(assignment: Stmt.Assign) {
        assignChain(assignment)
    }

    private fun assignChain(assignment: Stmt) {
        var stmt: Stmt = assignment
        val tokens = arrayListOf<Token>()
        val setStmts = arrayListOf<Stmt.Set>()
        while (stmt is Stmt.Assign || stmt is Stmt.Set) {
            when (stmt) {
                is Stmt.Assign -> {
                    tokens += stmt.name
                    stmt = stmt.value
                }
                is Stmt.Set -> {
                    setStmts += stmt
                    stmt = stmt.value
                }
            }
        }

        val expr = (stmt as Stmt.Expression).expression

        resolve(expr)

        tokens.forEach {
            resolveLocal(expr, it)
        }

        setStmts.forEach {
            resolve(it.obj)
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
        isInsideLoopBody = true
        with(stmt) {
            resolve(condition)
            resolve(body)
        }
        isInsideLoopBody = false
    }

    override fun visitForStmt(stmt: Stmt.For) {
        isInsideLoopBody = true
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
        isInsideLoopBody = false
    }

    override fun visitBreakStmt(stmt: Stmt.Break) {
        if (!isInsideLoopBody) {
            Lox.error(stmt.keyword, "Unresolved break statement")
        }
        isInsideLoopBody = false
    }

    override fun visitContinueStmt(stmt: Stmt.Continue) {
        if (!isInsideLoopBody) {
            Lox.error(stmt.keyword, "Unresolved continue statement")
        }
        isInsideLoopBody = false
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        when(currentFunction) {
            LoxFunctionType.NONE -> Lox.error(stmt.keyword, "Unresolved return statement");
            LoxFunctionType.INITIALIZER -> Lox.error(stmt.keyword,  "Cannot return a value from an initializer.");
            else -> stmt.value?.let { resolve(it) }
        }
    }

    override fun visitClassStmt(clazz: Stmt.Class) {
        val enclosingClass = currentClass
        currentClass = LoxClassType.CLASS
        with(clazz) {
            declare(name)
            define(name)

            beginScope()
            scopes.peek()["this"] = USED

            methods.forEach {
                val declaration = if (it.name.lexeme == "init")
                    LoxFunctionType.INITIALIZER
                else
                    LoxFunctionType.METHOD

                resolveFunction(it, declaration)
            }

            endScope()
        }
        currentClass = enclosingClass
    }

    //endregion

    //region EXPR ------------------------------------------------------------------------------------------------------

    override fun visitVariableExpr(variable: Expr.Variable) = with(variable) {
        if (scopes.isNotEmpty()) {
            val isShadowing = scopes.peek()[name.lexeme] == DECLARED
            if (isShadowing) {
                resolveLocal(variable, name, true)
            } else {
                resolveLocal(variable, name)
            }
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

    override fun visitGetExpr(get: Expr.Get) {
        resolve(get.obj)
    }

    override fun visitThisExpr(thiz: Expr.This) {
        if (currentClass == LoxClassType.NONE) {
            Lox.error(thiz.keyword, "Unresolved this statement")
        }
        resolveLocal(thiz, thiz.keyword)
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

    private fun declare(token: Token) {
        if (scopes.isEmpty()) {
            return
        }
        scopes.peek().let { currentScope ->
            if (currentScope.containsKey(token.lexeme)) {
                Lox.error(token, "This name is already used by another declaration in this scope.")
            }
            currentScope[token.lexeme] = DECLARED
        }

    }

    private fun define(token: Token) {
        if (scopes.isEmpty()) {
            return
        }
        scopes.peek()[token.lexeme] = DEFINED
    }

    private fun resolveLocal(expr: Expr, name: Token, isShadowing: Boolean = false) {
        val reversed = scopes.asReversed().toMutableList()

        if (isShadowing) {
            reversed.removeAt(0)
        }
        reversed.forEachIndexed { i, it ->
            if (it.containsKey(name.lexeme)) {
                interpreter.resolve(expr, i)
                it[name.lexeme] = USED
                return
            }
        }
    }

    private fun resolveFunction(func: Stmt.Function, type: LoxFunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type

        beginScope()
        resolveLambda(func.functionExpr)
        endScope()

        currentFunction = enclosingFunction
    }

    private fun resolveLambda(func: Expr.Function) {
        func.params.forEach {
            declare(it)
            define(it)
        }
        resolve(func.body)
    }
}