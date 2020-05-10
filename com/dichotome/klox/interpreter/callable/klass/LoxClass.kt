package com.dichotome.klox.interpreter.callable.klass

import com.dichotome.klox.interpreter.Interpreter
import com.dichotome.klox.interpreter.callable.LoxCallable
import com.dichotome.klox.interpreter.callable.LoxFunction

class LoxClass(
    override val name: String,
    private val methods: HashMap<String, LoxFunction>
): LoxCallable {

    override val arity: Int = 0

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any =
        LoxInstance(this)

    override fun toString() = "<cls $name>"

    fun findMethod(name: String): LoxFunction? {
        if (methods.containsKey(name))
            return methods[name]

        return null
    }
}