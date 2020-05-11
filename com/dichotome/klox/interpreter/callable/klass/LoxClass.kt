package com.dichotome.klox.interpreter.callable.klass

import com.dichotome.klox.interpreter.Interpreter
import com.dichotome.klox.interpreter.callable.LoxCallable
import com.dichotome.klox.interpreter.callable.LoxFunction

class LoxClass(
    override val name: String,
    private val superClass: LoxClass?,
    private val methods: HashMap<String, LoxFunction>
): LoxCallable {

    override val arity: Int = findMethod("init")?.arity ?: 0

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        val instance = LoxInstance(this)
        findMethod("init")?.apply {
            bind(instance).call(interpreter, arguments)
        }
        return instance
    }


    override fun toString() = "<cls $name>"

    fun findMethod(name: String): LoxFunction? {
        if (methods.containsKey(name))
            return methods[name]

        superClass?.findMethod(name)?.let {
            return it
        }

        return null
    }
}