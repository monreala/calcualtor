package kingdom.game;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/** Один вариант ответа на карте (или исход броска). Поля null — «не задано». */
public final class Opt {
    public String t;
    public int[] fx;
    public int[] pv;
    public String msg, log, ach, end, rem, set;
    public String addId;
    public int addN = -1;
    public List<Next> next;
    public Roll roll;
    public Function<Game, Opt> dyn;
    public Consumer<Game> act;
    public Supplier<Card> go;
    public boolean fortune, dice;

    public static final class Next {
        public final String id; public final int delay;
        Next(String id, int delay) { this.id = id; this.delay = delay; }
    }

    public static final class Roll {
        public final ToDoubleFunction<Game> p; public final Opt win, lose;
        Roll(ToDoubleFunction<Game> p, Opt win, Opt lose) { this.p = p; this.win = win; this.lose = lose; }
    }

    // ── построитель ──
    public static Opt o(String t, int c, int p, int a, int g) { Opt o = new Opt(); o.t = t; o.fx = new int[]{c, p, a, g}; return o; }
    public static Opt o(String t) { Opt o = new Opt(); o.t = t; return o; }
    public static Opt fx(int c, int p, int a, int g) { return o(null, c, p, a, g); }
    public static Opt none() { return new Opt(); }

    public Opt msg(String m) { msg = m; return this; }
    public Opt log(String l) { log = l; return this; }
    public Opt ach(String a) { ach = a; return this; }
    public Opt end(String e) { end = e; return this; }
    public Opt rem(String r) { rem = r; return this; }
    public Opt set(String s) { set = s; return this; }
    public Opt add(String id) { addId = id; addN = -1; return this; }
    public Opt add(String id, int n) { addId = id; addN = n; return this; }
    public Opt next(String id, int d) { if (next == null) next = new ArrayList<Next>(); next.add(new Next(id, d)); return this; }
    public Opt roll(double p, Opt win, Opt lose) { roll = new Roll(g -> p, win, lose); return this; }
    public Opt roll(ToDoubleFunction<Game> p, Opt win, Opt lose) { roll = new Roll(p, win, lose); return this; }
    public Opt dyn(Function<Game, Opt> f) { dyn = f; return this; }
    public Opt act(Consumer<Game> c) { act = c; return this; }
    public Opt go(Supplier<Card> s) { go = s; return this; }
    public Opt pv(int c, int p, int a, int g) { pv = new int[]{c, p, a, g}; return this; }
    public Opt fortune() { fortune = true; return this; }
    public Opt dice() { dice = true; return this; }

    /** Рискованный ли вариант — для значка кубика. */
    public boolean risky() { return roll != null || dice; }

    /** Копия this, поверх которой записаны заданные поля b (как Object.assign). */
    Opt merge(Opt b) {
        Opt r = new Opt();
        r.t = t; r.fx = fx; r.pv = pv; r.msg = msg; r.log = log; r.ach = ach; r.end = end; r.rem = rem; r.set = set;
        r.addId = addId; r.addN = addN; r.next = next; r.roll = roll; r.dyn = dyn; r.act = act; r.go = go;
        r.fortune = fortune; r.dice = dice;
        if (b == null) return r;
        if (b.t != null) r.t = b.t;
        if (b.fx != null) r.fx = b.fx;
        if (b.pv != null) r.pv = b.pv;
        if (b.msg != null) r.msg = b.msg;
        if (b.log != null) r.log = b.log;
        if (b.ach != null) r.ach = b.ach;
        if (b.end != null) r.end = b.end;
        if (b.rem != null) r.rem = b.rem;
        if (b.set != null) r.set = b.set;
        if (b.addId != null) { r.addId = b.addId; r.addN = b.addN; }
        if (b.next != null) r.next = b.next;
        if (b.roll != null) r.roll = b.roll;
        if (b.dyn != null) r.dyn = b.dyn;
        if (b.act != null) r.act = b.act;
        if (b.fortune) r.fortune = true;
        return r;
    }
}
