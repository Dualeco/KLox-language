package com.dichotome.klox.grammar

import com.dichotome.klox.scanner.Token

sealed class Expr {

    // abstract functions

    abstract fun <R> accept(visitor: Visitor<R>): R

    // implementing types

    interface Visitor<R> {
        fun visitNoneExpr(): R
        fun visitBinaryExpr(binary: Binary): R
        fun visitCommaExpr(comma: Comma): R
        fun visitTernaryExpr(ternary: Ternary): R
        fun visitGroupingExpr(grouping: Grouping): R
        fun visitLiteralExpr(literal: Literal): R
        fun visitUnaryExpr(unary: Unary): R
        fun visitVariableExpr(variable: Variable): R
        fun visitLogicalExpr(logical: Logical): R
        fun visitCallExpr(call: Call): R
        fun visitFuncExpr(func: Function): R
    }

    class None : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitNoneExpr()

        override fun toString(): String = ""
    }

    class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitBinaryExpr(this)

        override fun toString(): String = "$left ${operator.lexeme} $right"
    }

    class Comma(val list: List<Expr>): Expr() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitCommaExpr(this)

        override fun toString(): String = list.joinToString(separator = ", ")
    }

    class Ternary(val operator: Token, val first: Expr, val second: Expr, val third: Expr) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitTernaryExpr(this)

        override fun toString(): String = "$first ? $second : $third"
    }

    class Grouping(val expr: Expr) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitGroupingExpr(this)

        override fun toString(): String = "($expr)"
    }

    class Literal(val value: Any?) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitLiteralExpr(this)

        override fun toString(): String = "$value"
    }

    class Unary(val operator: Token, val right: Expr) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitUnaryExpr(this)

        override fun toString(): String = "${operator.lexeme}$right"
    }

    class Variable(val name: Token) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitVariableExpr(this)

        override fun toString() = name.lexeme
    }

    class Logical(val left: Expr, val operator: Token, val right: Expr) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitLogicalExpr(this)
    }

    class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>) : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitCallExpr(this)

        override fun toString(): String =
            "$callee(${arguments.joinToString(", ")})"
    }

    class Function(val params: List<Token>, val body: Stmt.Block, val name: String = "anonymous") : Expr() {
        override fun <R> accept(visitor: Visitor<R>): R =
            visitor.visitFuncExpr(this)
    }
}