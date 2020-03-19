package com.dichotome.klox.scanner

import com.dichotome.klox.Lox
import com.dichotome.klox.scanner.TokenType.*


internal class Scanner(
    private val source: String
) {
    val keywords = hashMapOf(
        "and" to AND,
        "class" to CLASS,
        "else" to ELSE,
        "false" to FALSE,
        "for" to FOR,
        "fun" to FUN,
        "if" to IF,
        "nil" to NIL,
        "or" to OR,
        "print" to PRINT,
        "return" to RETURN,
        "super" to SUPER,
        "this" to THIS,
        "true" to TRUE,
        "var" to VAR,
        "while" to WHILE
    )

    private val tokens: MutableList<Token> = arrayListOf()
    private var start = 0
    private var current = 0
    private var line = 1

    private val isAtEnd get() = current >= source.length
    private val isBeforeEndOfMultilineComment get() = peek() == '*' && peekNext() == '/'
    private val isBeforeStartOfMultilineComment get()  = peek() == '/' && peekNext() == '*'

    fun scanTokens(): List<Token> {
        while (!isAtEnd) { // We are at the beginning of the next lexeme.
            start = current
            scanToken()
        }
        tokens += Token(EOF, "", null, line)
        return tokens
    }

    private fun scanToken() {
        val char = peek()
        when (char) {
            '(', ')', '{', '}', ':', ',', '.', '-', '+', ';', '*', ' ', '\n', '\r', '\t' -> consumeSingleCharToken(char)
            '!', '=', '<', '>' -> consumeBooleanOperators(char)
            '/' -> consumeSlashOrComment()
            '"' -> consumeString()?.let { addToken(STRING, it) }
            else -> {
                when {
                    char.isDigit() -> consumeNumber()?.let { addToken(NUMBER, it) }
                    char.isAlpha() -> addToken(consumeIdentifier())
                    else -> Lox.error(line, "Unexpected character")
                }
            }
        }
    }

    private fun consumeBooleanOperators(char: Char) = when(char) {
        '!' -> if (peekNext() == '=') BANG_EQUAL else BANG
        '=' -> if (peekNext() == '=') EQUAL_EQUAL else EQUAL
        '<' -> if (peekNext() == '=') LESS_EQUAL else LESS
        '>' -> if (peekNext() == '=') GREATER_EQUAL else GREATER
        else -> null
    }?.let {
        when(it) {
            BANG_EQUAL, EQUAL_EQUAL, LESS_EQUAL, GREATER_EQUAL -> advanceTwice()
            BANG, EQUAL, LESS, GREATER -> advance()
        }
        addToken(it)
    }

    private fun consumeSlashOrComment() = when(peekNext()) {
        '/' -> consumeSingleLineComment()
        '*' -> consumeMultilineComment()
        else -> consumeSingleCharToken('/')
    }

    private fun consumeSingleCharToken(char: Char) = when(char) {
        '/' -> SLASH
        '(' -> LEFT_PAREN
        ')' -> RIGHT_PAREN
        '{' -> LEFT_BRACE
        '}' -> RIGHT_BRACE
        ':' -> COLON
        ',' -> COMMA
        '.' -> DOT
        '-' -> MINUS
        '+' -> PLUS
        ';' -> SEMICOLON
        '*' -> STAR
        '\n' -> {
            newLine()
            null
        }
        ' ', '\r', '\t' -> null
        else -> null
    }.also {
        advance()
    }?.let {
        addToken(it)
    }

    private fun consumeMultilineComment() {
        advanceTwice()

        while (current + 1 < source.length && !isBeforeEndOfMultilineComment) {
            if (isBeforeStartOfMultilineComment) {
                consumeMultilineComment()
            } else {
                advance()
            }
        }

        if (current + 1 >= source.length) {
            Lox.error(line, "Unterminated multiline comment.")
            return
        } else {
            advance()
            advance()
        }
    }

    private fun consumeSingleLineComment() {
        advanceTwice()
        while (peek() != '\n' && !isAtEnd) {
            advance()
        }
    }

    private fun consumeNumber(): Double? {
        while (peek().isDigit() || peek() == '.' && peekNext().isDigit()) {
            advance()
        }
        return try {
            source.substring(start, current).toDouble()
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun consumeString(): String? {
        while (peek() != '"' && !isAtEnd) {
            if (peek() == '\n') line++
            advance()
        }
        return if (isAtEnd) { // Unterminated string.
            Lox.error(line, "Unterminated string.")
            null
        } else { // The closing ".
            advance()
            source.substring(start + 1, current - 1)
        }
    }

    private fun consumeIdentifier(): TokenType {
        while (peek().isAlphaNumeric()) {
            advance()
        }

        return keywords[source.substring(start, current)] ?: IDENTIFIER
    }

    private fun newLine() = line++

    private fun advanceTwice() {
        advance()
        advance()
    }

    private fun advance() = source[current].also {
        current++
    }

    private fun peek() = if (!isAtEnd) source[current] else '\u0000'

    private fun peekNext() = if (current + 1 < source.length) source[current + 1] else '\u0000'

    private fun addToken(type: TokenType) = addToken(type, null)

    private fun addToken(type: TokenType, literal: Any?) {
        val text = source.substring(start, current)
        tokens += Token(type, text, literal, line)
    }

    private fun Char.isAlpha() = (this in 'a'..'z') || (this in 'A'..'Z') || this == '_';
    private fun Char.isAlphaNumeric() = isDigit() || isAlpha()
}