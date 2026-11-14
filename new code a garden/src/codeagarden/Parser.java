package codeagarden;

import java.util.*;

class Parser {
    private static final String[][] LEVELS = {
        {"==", "!="}, {"<", "<=", ">", ">="}, {"+", "-"}, {"*", "/"}
    };

    private final List<String> t;
    private int i = 0;

    Parser(List<String> tokens){
        t = tokens; 
    }

    List<Stmt> parse() {
        List<Stmt> out = new ArrayList<>();
        while (!peek().equals("<EOF>")) out.add(statement());
        return out;
    }

    private String peek() { return t.get(i); }
    private String next() { return t.get(i++); }

    private boolean match(String... options) {
        if (Arrays.asList(options).contains(peek())){
            i++; return true;
        }
        return false;
    }

    private Stmt statement() {
        if (match("int")){
            String name = next();
            Expr init = match("=") ? expr() : null;
            next();
            return new Stmt.Decl(name, init);
        }
        if (match("if")){
            next();
            Expr cond = expr();
            next();
            Stmt then = statement();
            return new Stmt.If(cond, then, match("else") ? statement() : null);
        }
        if (match("while")){
            next();
            Expr cond = expr();
            next();
            return new Stmt.While(cond, statement());
        }
        if (match("for")){
            next();
            Stmt init = statement();
            Expr cond = expr();
            next();
            Expr update = expr();
            next();
            return new Stmt.For(init, cond, update, statement());
        }
        if (match("{")){
            List<Stmt> body = new ArrayList<>();
            while (!match("}")) body.add(statement());
            return new Stmt.Block(body);
        }
        Expr e = expr();
        next();
        return new Stmt.ExprStmt(e);
    }

    private Expr expr(){
        Expr left = binary(0);
        if (left instanceof Expr.Var v && match("=")) return new Expr.Assign(v.name(), expr());
        return left;
    }

    private Expr binary(int level){
        if (level == LEVELS.length) return primary();
        Expr left = binary(level + 1);
        while (match(LEVELS[level])) {
            String op = t.get(i - 1);
            left = new Expr.Binary(left, op, binary(level + 1));
        }
        return left;
    }

    private Expr primary(){
        String tok = next();
        if (Character.isDigit(tok.charAt(0))) return new Expr.Num(Integer.parseInt(tok));
        if (tok.equals("(")){Expr e = expr(); next(); return e;}
        if (match("(")){
            List<Expr> args = new ArrayList<>();
            while (!match(")")){args.add(expr()); match(",");}
            return new Expr.Call(tok, args);
        }
        if (match("++"))
            return new Expr.Assign(tok, new Expr.Binary(new Expr.Var(tok), "+", new Expr.Num(1)));
        return new Expr.Var(tok);
    }
}
