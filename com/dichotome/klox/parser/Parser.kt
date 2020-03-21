package com.dichotome.klox.parser

import com.dichotome.klox.Lox
import com.dichotome.klox.grammar.Expr
import com.dichotome.klox.grammar.Expr.Literal
import com.dichotome.klox.scanner.Token
import com.dichotome.klox.scanner.TokenType
import com.dichotome.klox.scanner.TokenType.*


class Parser(
    private val tokens: List<Token>
) {
    private class ParseError : RuntimeException()

    companion object {
        fun error(token: Token, message: String) {
            if (token.type === EOF) {
                Lox.report(token.line, " at end", message)
            } else {
                Lox.report(token.line, " at '" + token.lexeme + "'", message)
            }
        }
    }

    private var current = 0

    fun parse(): Expr? = try {
        expression()
    } catch (error: ParseError) {
        null
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
        var expr = equality()
        val second: Expr
        val third: Expr
        if (match(QUESTION)) {
            second = ternary()
            if (match(COLON)) {
                third = ternary()
                expr = Expr.Ternary(expr, second, third)
            } else {
                throw error(peek(), "Incomplete ternary operator: $expr ? $second, ':' branch is missing")
            }
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
        var expr = unary()
        while (match(SLASH, STAR)) {
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
        return primary()
    }

    private fun primary(): Expr =
        when {
            match(FALSE) -> Literal(false)
            match(TRUE) -> Literal(true)
            match(NIL) -> Literal(null)
            match(NUMBER, STRING) -> {
                Literal(previous().literal)
            }
            match(LEFT_PAREN) -> {
                val expr = expression()
                consume(RIGHT_PAREN, "Expect ')' after expression.")
                Expr.Grouping(expr)
            }
            checkBinaryOperator(peek()) ->
                throw noLeftOperandError(peek())
            else -> throw error(peek() , "Expect expression.")
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
            MINUS
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

    private fun consume(type: TokenType, message: String): Token? {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun noLeftOperandError(token: Token): ParseError =
        error(token, "Missing left operand before ${token.lexeme}")

    private fun error(token: Token, message: String): ParseError {
        Parser.error(token, message)
        return ParseError()
    }

    private fun check(type: TokenType): Boolean =
        if (isAtEnd()) {
            false
        } else {
            peek().type === type
        }

    private fun advance(): Token? {
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
                CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> return
                else -> advance()

            }
        }
    }
}