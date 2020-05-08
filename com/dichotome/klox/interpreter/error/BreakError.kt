package com.dichotome.klox.interpreter.error

import com.dichotome.klox.error.RuntimeError
import com.dichotome.klox.scanner.Token

class BreakError(token: Token) : RuntimeError(token, "Break statement outside a loop")