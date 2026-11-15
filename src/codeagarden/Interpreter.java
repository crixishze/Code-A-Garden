package codeagarden;

import java.util.*;

class Interpreter {
    interface Game{ 
        Object call(String name, List<Object> args);
    }

    static class Stop extends RuntimeException {
        Stop(){ 
            super(null, null, false, false);
        }
    }

    volatile boolean paused, stopped;
    private final Map<String, Object> vars = new HashMap<>();
    private final Game game;

    Interpreter(Game game) {
        this.game = game;
        vars.put("true", true);
        vars.put("false", false);
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); 
        } catch (InterruptedException e){ 
            throw new Stop();
        }
    }

    void run(List<Stmt> program){
        try { for (Stmt s : program) exec(s);
        } catch (Stop e) {}
    }

    private void tick() {
        while (paused && !stopped) 
            sleep(50);
        if (stopped) 
            throw new Stop();
    }

    private void exec(Stmt s) {
        tick();
        if (s instanceof Stmt.Decl d){
            vars.put(d.name(), d.init() == null ? 0 : eval(d.init()));
        } else if (s instanceof Stmt.If i){
            if (truthy(eval(i.cond()))) exec(i.then());
            else if (i.otherwise() != null) exec(i.otherwise());
        } else if (s instanceof Stmt.While w){
            while (truthy(eval(w.cond()))) exec(w.body());
        } else if (s instanceof Stmt.For f){
            exec(f.init());
            while (truthy(eval(f.cond()))) {
                exec(f.body());
                eval(f.update());
            }
        } else if (s instanceof Stmt.Block b){
            for (Stmt x : b.stmts()) exec(x);
        } else if (s instanceof Stmt.ExprStmt e){
            eval(e.expr());
        }
    }

    private Object eval(Expr e){
        if (e instanceof Expr.Num n) 
            return n.value();
        if (e instanceof Expr.Var v) 
            return vars.getOrDefault(v.name(), v.name());
        if (e instanceof Expr.Assign a){
            Object val = eval(a.value());
            vars.put(a.name(), val);
            return val;
        }
        if (e instanceof Expr.Call c){
            tick();
            List<Object> args = new ArrayList<>();
            for (Expr x : c.args()) args.add(eval(x));
            return game.call(c.name(), args);
        }
        Expr.Binary b = (Expr.Binary) e;
        Object l = eval(b.left()), r = eval(b.right());
        if (b.op().equals("==")) 
            return l.equals(r);

        if (b.op().equals("!=")) 
            return !l.equals(r);

        int x = (Integer) l, y = (Integer) r;
        return switch (b.op()) {
            case "+" -> x + y;
            case "-" -> x - y;
            case "*" -> x * y;
            case "/" -> x / y;
            case "<" -> x < y;
            case "<=" -> x <= y;
            case ">" -> x > y;
            default -> x >= y;
        };
    }

    private boolean truthy(Object v){
        return v instanceof Boolean b ? b : !v.equals(0);
    }
}