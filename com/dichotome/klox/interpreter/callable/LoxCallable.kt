package com.dichotome.klox.interpreter.callable

import com.dichotome.klox.interpreter.Interpreter

interface LoxCallable {

    val name: String

    val arity: Int

    fun call(interpreter: Interpreter, arguments: List<Any>): Any
}