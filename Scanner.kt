import TokenType.*
import java.lang.NumberFormatException


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

    fun scanTokens(): List<Token> {
        while (!isAtEnd) { // We are at the beginning of the next lexeme.
            start = current
            scanToken()
        }
        tokens += Token(EOF, "", null, line)
        return tokens
    }

    private fun scanToken() {
        val char = advance()
        when (char) {
            '(' -> addToken(LEFT_PAREN)
            ')' -> addToken(RIGHT_PAREN)
            '{' -> addToken(LEFT_BRACE)
            '}' -> addToken(RIGHT_BRACE)
            ':' -> addToken(COLON)
            ',' -> addToken(COMMA)
            '.' -> addToken(DOT)
            '-' -> addToken(MINUS)
            '+' -> addToken(PLUS)
            ';' -> addToken(SEMICOLON)
            '*' -> addToken(STAR)
            '!' -> addToken(if (peek() == '=') { advance(); BANG_EQUAL } else BANG)
            '=' -> addToken(if (peek() == '=') { advance(); EQUAL_EQUAL } else EQUAL)
            '<' -> addToken(if (peek() == '=') { advance(); LESS_EQUAL } else LESS)
            '>' -> addToken(if (peek() == '=') { advance(); GREATER_EQUAL } else GREATER)
            '/' -> {
                if (peek() == '/') { // A comment goes until the end of the line.
                    while (peek() != '\n' && !isAtEnd) advance()
                } else {
                    addToken(SLASH)
                }
            }
            '\n' -> newLine()
            ' ', '\r', '\t' -> {}
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