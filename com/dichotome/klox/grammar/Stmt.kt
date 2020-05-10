package com.dichotome.klox.grammar

import com.dichotome.klox.scanner.Token

sealed class Stmt {

    abstract fun <R> accept(visitor: Visitor<R>): R

    interface Visitor<R> {
        fun visitExpressionStmt(stmt: Expression): R
        fun visitVarStmt(stmt: Var): R
        fun visitBlockStmt(stmt: Block): R
        fun visitIfStmt(stmt: If): R
        fun visitWhileStmt(stmt: While): R
        fun visitForStmt(stmt: For): R
        fun visitBreakStmt(stmt: Break): R
        fun visitContinueStmt(stmt: Continue): R
        fun visitFunctionStmt(stmt: Function): R
        fun visitReturnStmt(stmt: Return): R
        fun visitAssignStmt(assignment: Assign): R
        fun visitClassStmt(clazz: Class): R
        fun visitSetStmt(set: Set): R
    }

    class None : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R =
            throw error("Visited non statement.")

        override fun toString(): String = ""
    }

    class Expression(val expression: Expr) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitExpressionStmt(this)


        override fun toString(): String = "Expression Stmt $expression"
    }

    class Var(val name: Token, val assignment: Assign?) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitVarStmt(this)

        override fun toString(): String = "Var Stmt (${assignment?.let { assignment } ?: name.lexeme})"
    }

    class Block(val statements: List<Stmt>) : Stmt() {

        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitBlockStmt(this)

        override fun toString() = "{\n" + statements.joinToString { "  $it\n" } + "}"
    }

    class If(val condition: Expr, val then: Stmt, val other: Stmt? = None()) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitIfStmt(this)
    }

    class While(val condition: Expr, val body: Stmt) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitWhileStmt(this)
    }

    class For(
        val initializer: Stmt?,
        val condition: Expr?,
        val increment: Stmt?,
        val body: Stmt
    ) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitForStmt(this)

        override fun toString(): String =
            "For ($initializer; $condition; $increment) $body"
    }

    class Break(val keyword: Token) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitBreakStmt(this)
    }

    class Continue(val keyword: Token) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitContinueStmt(this)
    }

    class Function(val name: Token, val functionExpr: Expr.Function) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitFunctionStmt(this)

        override fun toString(): String = with(functionExpr) {
            "Function ${name}(${params.joinToString(", ") { it.lexeme }}) $body"
        }
    }

    class Return(val keyword: Token, val value: Expr?) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitReturnStmt(this)

        override fun toString(): String =
            "Return " + value?.let { it }
    }

    class Assign(val name: Token, val value: Stmt) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitAssignStmt(this)

        override fun toString(): String {
            if (value is Assign) {
                return "${name.lexeme}, ${value.value}"
            }

            if (value is Expression) {
                return "${name.lexeme} = ${value.expression}"
            }
            return "${name.lexeme} = $value"
        }
    }

    class Class(val name: Token, val methods: List<Function>): Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitClassStmt(this)

        override fun toString(): String =
            "class ${name.lexeme} {\n" +
                    methods.joinToString { "  $it\n" } +
                    "}"
    }

    class Set(val obj: Expr, val name: Token, val value: Stmt): Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitSetStmt(this)

        override fun toString(): String =
            "$obj.$name = $value"
    }
}