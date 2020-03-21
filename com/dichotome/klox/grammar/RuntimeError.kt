package com.dichotome.klox.grammar

import com.dichotome.klox.scanner.Token

class RuntimeError(val token: Token, message: String): RuntimeException(message)