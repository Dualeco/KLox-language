package com.dichotome.klox.grammar

object PrefixNotationFactory : Expr.Visitor<String> {

    fun create(expr: Expr): String = expr.accept(this)

    private fun parenthesize(name: String, vararg expressions: Expr): String =
        expressions.map { expr -> expr.accept(this) }
            .joinToString { expr -> " $expr" }
            .let { exprs -> "($name $exprs)" }

    override fun visitBinaryExpr(expr: Expr.Binary): String = with(expr) {
        parenthesize(operator.lexeme, left, right)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): String = with(expr) {
        parenthesize("group", expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): String = with(expr) {
        value?.toString() ?: "nil"
    }

    override fun visitUnaryExpr(expr: Expr.Unary): String = with(expr) {
        parenthesize(operator.lexeme, right)
    }

    override fun visitVariableExpr(expr: Expr.Variable): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitAssignExpr(expr: Expr.Assign): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitLogicalExpr(expr: Expr.Logical): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitCallExpr(expr: Expr.Call): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitFuncExpr(expr: Expr.Func): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}