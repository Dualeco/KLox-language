package com.dichotome.klox.parser

import com.dichotome.klox.Lox
import com.dichotome.klox.error.RuntimeError
import com.dichotome.klox.grammar.Expr
import com.dichotome.klox.grammar.Stmt
import com.dichotome.klox.resolver.LoxFunctionType
import com.dichotome.klox.resolver.LoxFunctionType.FUNCTION
import com.dichotome.klox.resolver.LoxFunctionType.METHOD
import com.dichotome.klox.scanner.Token
import com.dichotome.klox.scanner.TokenType
import com.dichotome.klox.scanner.TokenType.*


class Parser(
    private val tokens: List<Token>
) {
    private class ParseError : RuntimeException()

    companion object {
        fun error(token: Token, message: String) = Lox.report(
            token.line,
            " at ${if (token.type === EOF) "end" else "'" + token.lexeme + "'"}",
            message
        )
    }

    private var current = 0

    fun parse(): List<Stmt> = arrayListOf<Stmt>().apply {
        while (!isAtEnd()) {
            declaration()?.let { add(it) }
        }
    }

    private fun block(): Stmt.Block {
        val statements = arrayListOf<Stmt>()

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            declaration()?.let { statements += it }
        }

        consume(RIGHT_BRACE, "Expect } at the end of block")

        return Stmt.Block(statements)
    }

    private fun declaration(): Stmt? = try {
        when (peek().type) {
            CLASS -> classDeclaration()
            FUN -> funDeclaration(FUNCTION)
            VAR -> varDeclaration()
            else -> assignment()
        }.also {
            match(NEW_LINE)
        }
    } catch (e: ParseError) {
        synchronize()
        null
    }

    private fun classDeclaration(): Stmt {
        match(CLASS)
        val name = consume(IDENTIFIER, "Expect class name.")

        var superClass: Expr.Variable? = null
        if (match(COLON)) {
            consume(IDENTIFIER, "Expect superclass name.")
            superClass = Expr.Variable(previous())
        }

        consume(LEFT_BRACE, "Expect '{' before class body.")

        val methods = arrayListOf<Stmt.Function>()

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            val stmt = funDeclaration(METHOD)

            // if fun is lambda
            if (stmt is Stmt.Expression) {
                val expr = stmt.expression
                if (expr is Expr.Function) {
                    throw RuntimeError(
                        expr.paren, "Only named functions are allowed in classes"
                    )
                } else if (expr is Expr.None) {
                    continue
                }
            } else {
                methods += stmt as Stmt.Function
            }
        }

        consume(RIGHT_BRACE, "Expect '}' after class body.")

        return Stmt.Class(name, superClass, methods)
    }

    private fun funDeclaration(kind: LoxFunctionType): Stmt {
        match(FUN)

        if (peek().type == IDENTIFIER) {
            val name = consume(IDENTIFIER, "")
            val functionExpr = finishFunction(kind, name.lexeme)

            return Stmt.Function(name, functionExpr)
        }

        return Stmt.Expression(functionExpression())
    }

    private fun varDeclaration(): Stmt {
        match(VAR)
        if (peek().type != IDENTIFIER) {
            return statement()
        }

        if (next().type != EQUAL) {
            return Stmt.Var(peek(), null)
        }

        val assignment = assignment()

        if (assignment !is Stmt.Assign) {
            return statement()
        }

        return Stmt.Var(assignment.name, assignment)
    }

    private fun assignment(): Stmt {
        val stmt = statement()
        if (match(EQUAL)) {
            val expr = (stmt as Stmt.Expression).expression
            when (expr) {
                is Expr.Variable -> {
                    return Stmt.Assign(
                        expr.name,
                        assignment()
                    )
                }
                is Expr.Get -> {
                    return Stmt.Set(
                        expr.obj,
                        expr.name,
                        assignment()
                    )
                }
            }
        }
        return stmt
    }

    private fun whileStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'while'")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after 'while'")
        val body = statement()

        return Stmt.While(condition, body)
    }

    private fun forStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'for'")

        val initializer = when {
            match(SEMICOLON) -> null
            match(VAR) -> varDeclaration()
            else -> expressionStatement()
        }
        consume(SEMICOLON, "Expect ';' after 'for' condition")


        val condition = when {
            !check(SEMICOLON) -> expression()
            else -> null
        }
        consume(SEMICOLON, "Expect ';' after 'for' loop condition")

        val increment: Stmt? = when {
            !check(RIGHT_PAREN) -> assignment()
            else -> null
        }
        consume(RIGHT_PAREN, "Expect ')' after 'for'")

        val body = statement()

        return Stmt.For(initializer, condition, increment, body)
    }

    private fun ifStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'if'")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after 'if'")

        val thenBranch = statement()
        val elseBranch = if (match(ELSE)) statement() else null

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun expressionStatement(): Stmt = Stmt.Expression(expression())

    private fun returnStatement(): Stmt {
        val keyword = previous()
        val value = when {
            check(listOf(NEW_LINE, EOF)) -> null
            else -> expression()
        }

        return Stmt.Return(keyword, value)
    }

    private fun statement(): Stmt = when {
        match(RETURN) -> returnStatement()
        match(CONTINUE) -> Stmt.Continue(previous())
        match(BREAK) -> Stmt.Break(previous())
        match(FOR) -> forStatement()
        match(WHILE) -> whileStatement()
        match(IF) -> ifStatement()
        match(LEFT_BRACE) -> block()
        else -> expressionStatement()
    }

    private fun expression(): Expr = comma()

    private fun comma(): Expr {
        val list = arrayListOf(ternary())
        while (match(COMMA)) {
            list += ternary()
        }
        return if (list.size == 1) list.first() else Expr.Comma(list)
    }

    private fun ternary(): Expr {
        var expr = or()
        val second: Expr
        val third: Expr
        val firstToken = peek()
        if (match(QUESTION)) {
            try {
                second = ternary()
                if (match(COLON)) {
                    third = ternary()
                    expr = Expr.Ternary(firstToken, expr, second, third)
                } else {
                    throw error(
                        peek(),
                        "Incomplete ternary operator: '$expr ? $second'"
                    )
                }
            } catch (e: ParseError) {
                throw error(
                    previous(),
                    "Incomplete ternary operator: '$expr ?'\nCause '?' and ':' branches are missing"
                )
            }
        }
        return expr
    }

    private fun or(): Expr {
        var expr = and()

        while (match(OR)) {
            val operator = previous()
            val right = and()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun and(): Expr {
        var expr = equality()

        while (match(AND)) {
            val operator = previous()
            val right = equality()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun equality(): Expr {
        var expr = comparison()
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            val operator = previous()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun comparison(): Expr {
        var expr = addition()
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            val operator = previous()
            val right = addition()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun addition(): Expr {
        var expr = multiplication()
        while (match(MINUS, PLUS)) {
            val operator = previous()
            val right = multiplication()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun multiplication(): Expr {
        var expr = power()
        while (match(SLASH, STAR)) {
            val operator = previous()
            val right = power()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun power(): Expr {
        var expr = mod()
        while (match(HAT)) {
            val operator = previous()
            val right = mod()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun mod(): Expr {
        var expr = unary()
        while (match(MOD)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun unary(): Expr {
        if (match(BANG, MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }
        return functionExpression()
    }

    private fun functionExpression(): Expr {
        if (peek().type == FUN) {
            match(FUN)
            return finishFunction(FUNCTION)
        }
        return call()
    }

    private fun finishFunction(kind: LoxFunctionType, name: String = "anonymous"): Expr.Function {
        consume(LEFT_PAREN, "Expect '(' after ${kind.name} declaration")

        val parameters = arrayListOf<Token>()

        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    error(peek(), "Cannot have more than 255 parameters.");
                } else {
                    parameters += consume(IDENTIFIER, "Expect parameter name.")
                }
            } while (match(COMMA))
        }

        val paren = consume(RIGHT_PAREN, "Expect ')' after parameters")

        return when {
            match(ARROW) -> {
                val returned = Stmt.Return(previous(), expression())
                Expr.Function(
                    paren, parameters, Stmt.Block(listOf(returned)), name
                )
            }
            else -> {
                consume(LEFT_BRACE, "Expect '{' before ${kind.name} body")
                Expr.Function(paren, parameters, block(), name)
            }
        }
    }

    private fun call(): Expr {
        var expr = primary()
        var shouldRun = true
        while (shouldRun) {
            when {
                match(LEFT_PAREN) -> expr = finishCall(expr)
                match(DOT) -> {
                    val name = consume(IDENTIFIER, "Expect property name after `.` .")
                    expr = Expr.Get(expr, name)
                }
                else -> shouldRun = false
            }
        }
        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments = arrayListOf<Expr>()
        if (!check(RIGHT_PAREN)) {
            when (val expr = comma()) {
                is Expr.Comma -> arguments.addAll(expr.list)
                else -> arguments += expr
            }
        }
        val paren = consume(RIGHT_PAREN, "Expect ')' after arguments.")

        return Expr.Call(callee, paren, arguments)
    }

    private fun primary(): Expr =
        when {
            match(FALSE) -> Expr.Literal(false)
            match(TRUE) -> Expr.Literal(true)
            match(NIL) -> Expr.Literal(null)
            match(NUMBER, STRING) -> Expr.Literal(previous().literal)
            match(SUPER) -> {
                val keyword = previous()
                consume(DOT, "Expect '.' after 'super'.")
                val token = consume(IDENTIFIER, "Expect superclass member name.")
                Expr.Super(keyword, token)
            }
            match(THIS) -> Expr.This(previous())
            match(IDENTIFIER) -> Expr.Variable(previous())
            match(LEFT_PAREN) -> {
                val expr = expression()
                consume(RIGHT_PAREN, "Expect ')' after expression.")
                Expr.Grouping(expr)
            }
            match(NEW_LINE) -> Expr.None()
            checkBinaryOperator(peek()) ->
                throw noLeftOperandError(peek())
            else -> throw error(peek(), "Expect expression. ${previous()}, ${peek()}")
        }

    private fun checkBinaryOperator(token: Token) =
        token.type in listOf(
            COMMA,
            QUESTION,
            BANG_EQUAL,
            EQUAL_EQUAL,
            GREATER,
            GREATER_EQUAL,
            LESS,
            LESS_EQUAL,
            MINUS,
            PLUS,
            SLASH,
            STAR,
            BANG,
            MINUS,
            MOD
        )

    private fun match(vararg types: TokenType): Boolean {
        types.forEach {
            if (check(it)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun consumeLineEnd(): Token =
        consume(listOf(NEW_LINE, EOF), "Expect new line after expression ${peek().lexeme}")

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun consume(types: List<TokenType>, message: String): Token {
        if (check(types)) return advance()
        throw error(peek(), message)
    }

    private fun noLeftOperandError(token: Token): ParseError =
        error(token, "Missing left operand before ${token.lexeme}")

    private fun error(token: Token, message: String): ParseError {
        Parser.error(token, message)
        return ParseError()
    }

    private fun check(types: List<TokenType>): Boolean =
        types.fold(false) { acc, tokenType -> acc || check(tokenType) }

    private fun check(type: TokenType): Boolean =
        if (isAtEnd() && type != EOF) {
            false
        } else {
            peek().type == type
        }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd(): Boolean =
        peek().type === EOF

    private fun next(): Token =
        tokens[current + 1]

    private fun peek(): Token =
        tokens[current]

    private fun previous(): Token =
        tokens[current - 1]

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {
            if (previous().type === SEMICOLON) return
            when (peek().type) {
                CLASS, FUN, VAR, FOR, IF, WHILE, RETURN -> return
                else -> advance()
            }
        }
    }
}