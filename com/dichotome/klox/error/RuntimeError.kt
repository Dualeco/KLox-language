package com.dichotome.klox.error

import com.dichotome.klox.scanner.Token

open class RuntimeError(val token: Token, message: String): RuntimeException("$token: $message")