package com.dichotome.klox

import com.dichotome.klox.grammar.Expr
import com.dichotome.klox.grammar.RuntimeError
import com.dichotome.klox.grammar.util.PrefixNotationFactory
import com.dichotome.klox.interpreter.Interpreter
import com.dichotome.klox.parser.Parser
import com.dichotome.klox.scanner.Scanner
import com.dichotome.klox.scanner.Token
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess


object Lox {
    private var hadError = false
    private var hadRuntimeError = false

    @JvmStatic
    fun main(args: Array<String>) {
        when {
            args.size > 1 -> {
                println("Usage: jlox [script]")
                exitProcess(64)
            }
            args.size == 1 -> {
                runFile(args[0])
            }
            else -> {
                runPrompt()
            }
        }
    }

    fun error(line: Int, message: String) = report(line, "", message)

    fun runtimeError(error: RuntimeError) {
        System.err.println("${error.message} \n[line " + error.token.line + "]")
        hadRuntimeError = true
    }

    fun report(line: Int, where: String, message: String) {
        System.err.println("[line $line] Error$where: $message")
        hadError = true
    }

    private fun runFile(path: String) {
        val bytes = Files.readAllBytes(Paths.get(path))
        scan(String(bytes, Charset.defaultCharset()))

        // Indicate an error in the exit code.
        when {
            hadError -> exitProcess(65)
            hadRuntimeError -> exitProcess(70)
        }

    }

    private fun runPrompt() {
        val input = InputStreamReader(System.`in`)
        val reader = BufferedReader(input)
        while (true) {
            print("> ")
            scan(reader.readLine())
            hadError = false
        }
    }

    private fun scan(source: String) {

        // Scan tokens
        println("\nScanning: -----------------------------------------------------------------------------------------\n")
        val scanner = Scanner(source)
        val tokens: List<Token> = scanner.scanTokens()
        tokens.forEach { println(it) }

        // Parse tokens
        println("\nParsing: ------------------------------------------------------------------------------------------\n")
        val parser = Parser(tokens)
        val expression: Expr = parser.parse() ?: return
        println(PrefixNotationFactory.create(expression))

        // Stop if there was a syntax error.
        if (hadError) return

        // Parse tokens
        println("\nInterpret: ----------------------------------------------------------------------------------------\n")
        val interpreter = Interpreter()
        println(interpreter.interpret(expression))
    }
}