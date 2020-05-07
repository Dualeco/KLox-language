package com.dichotome.klox.grammar.util

import com.dichotome.klox.grammar.Expr

object PrefixNotationFactory : Expr.Visitor<String> {

    fun create(expr: Expr): String = expr.accept(this)

    private fun parenthesize(name: String, vararg expressions: Expr): String =
        expressions.map { expr -> expr.accept(this) }
            .joinToString(separator = ", ")
            .let { exprs -> "($name $exprs)" }

    override fun visitNoneExpr() = ""

    override fun visitBinaryExpr(binary: Expr.Binary): String = with(binary) {
        parenthesize(operator.lexeme, left, right)
    }

    override fun visitCommaExpr(comma: Expr.Comma): String = with(comma) {
        parenthesize("comma", *list.toTypedArray())
    }

    override fun visitTernaryExpr(ternary: Expr.Ternary): String = with(ternary) {
        parenthesize("ternary", first, second, third)
    }

    override fun visitGroupingExpr(grouping: Expr.Grouping): String = with(grouping) {
        parenthesize("group", expr)
    }

    override fun visitLiteralExpr(literal: Expr.Literal): String = with(literal) {
        value?.toString() ?: "nil"
    }

    override fun visitUnaryExpr(unary: Expr.Unary): String = with(unary) {
        parenthesize(operator.lexeme, right)
    }

    override fun visitVariableExpr(variable: Expr.Variable): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitLogicalExpr(logical: Expr.Logical): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitCallExpr(call: Expr.Call): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitFuncExpr(func: Expr.Func): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}