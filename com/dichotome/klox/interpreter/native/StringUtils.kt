package com.dichotome.klox.interpreter.native

fun Any.stringify(): String =
    when (this) {
        Unit -> "nil"
        is Double -> "$this".removeSuffix(".0")
        else -> "$this"
    }