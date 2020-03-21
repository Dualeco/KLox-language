package com.dichotome.klox.scanner

enum class TokenType {
    // Single-character tokens.
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, COLON, COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR, QUESTION, HAT,
    // One or two character tokens.
    BANG, BANG_EQUAL, EQUAL, BANG_MOD, MOD, EQUAL_EQUAL, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL,
    // Literals.
    IDENTIFIER, STRING, NUMBER,
    // Keywords.
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR, PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

    EOF
}