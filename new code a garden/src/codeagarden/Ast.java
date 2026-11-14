package codeagarden;

import java.util.List;

sealed interface Expr {
    record Num(int value) implements Expr {}
    record Var(String name) implements Expr {}
    record Assign(String name, Expr value) implements Expr {}
    record Binary(Expr left, String op, Expr right) implements Expr {}
    record Call(String name, List<Expr> args) implements Expr {}
}

sealed interface Stmt {
    record Decl(String name, Expr init) implements Stmt {}
    record If(Expr cond, Stmt then, Stmt otherwise) implements Stmt {}
    record While(Expr cond, Stmt body) implements Stmt {}
    record For(Stmt init, Expr cond, Expr update, Stmt body) implements Stmt {}
    record Block(List<Stmt> stmts) implements Stmt {}
    record ExprStmt(Expr expr) implements Stmt {}
}
