package com.dichotome.klox.interpreter.error

import com.dichotome.klox.error.RuntimeError
import com.dichotome.klox.scanner.Token

class ContinueError(token: Token) : RuntimeError(token, "Continue statement outside a loop")
