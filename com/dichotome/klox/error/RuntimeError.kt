package com.dichotome.klox.error

import com.dichotome.klox.scanner.Token

class RuntimeError(val token: Token, message: String): RuntimeException(message)