package com.dichotome.klox.interpreter.error

import com.dichotome.klox.error.RuntimeError
import com.dichotome.klox.scanner.Token

class ReturnError(token: Token, val value: Any) : RuntimeError(token, "Redundant return statement")
