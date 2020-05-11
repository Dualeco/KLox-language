# KLox-language

A variant of the Lox language with enhanced syntax

(Great thanks to [GraydenH](https://github.com/GraydenH) for [Expr.kt](https://github.com/DichotoMe/KLox-language/blob/master/com/dichotome/klox/grammar/Expr.kt) and [Stmt.kt](https://github.com/DichotoMe/KLox-language/blob/master/com/dichotome/klox/grammar/Stmt.kt))

**Syntax in BNF:**

```
program        → declaration* EOF ;
```
```
1. Declarations

declaration    → classDecl
               | funDecl
               | varDecl
               | statement ;

classDecl      → "class" IDENTIFIER ( ":" IDENTIFIER )? "{" function* "}" ;

funDecl        → "fun" function ;

varDecl        → "var" IDENTIFIER ( "=" expression )? ;
```


```
2. Statements

statement      → exprStmt
               | forStmt
               | ifStmt
               | returnStmt
               | whileStmt
               | block ;

exprStmt       → expression ;

forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
                           expression? ";"
                           expression? ")" statement ;

ifStmt         → "if" "(" expression ")" statement ( "else" statement )? ;

returnStmt     → "return" expression? ;

whileStmt      → "while" "(" expression ")" statement ;

block          → "{" declaration* "}" ;
```


```
3. Expressions

expression     → assignment ;

assignment     → ( ( call "." )? IDENTIFIER "=" )* assignment
               | logic_or;
               
logic_or       → logic_and ( "or" logic_and )* ;

logic_and      → equality ( "and" equality )* ;

equality       → comparison ( ( "!=" | "==" ) comparison )* ;

comparison     → addition ( ( ">" | ">=" | "<" | "<=" ) addition )* ;

addition       → multiplication ( ( "-" | "+" ) multiplication )* ;

multiplication → power ( ( "/" | "*" ) power )* ;

power → mod ( "^" mod )* ;

mod → unary ( "%" unary )* ;

unary          → ( "!" | "-" ) unary | call ;

call           → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;

primary        → "true" | "false" | "nil" | "this" | "break | "continue"
               | NUMBER | STRING | IDENTIFIER | "(" expression ")"
               | "super" "." IDENTIFIER ;
```


```
4. Utility Rules

function       → IDENTIFIER ( "(" parameters? ")" block | "->" expression ) ;
parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
arguments      → expression ( "," expression )* ;
```

