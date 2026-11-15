package kingdom.game;

import java.util.function.Function;
import java.util.function.Predicate;

/** Карта: кто пришёл, что говорит и два варианта ответа. */
public final class Card {
    public String id;
    public String who;
    public Function<Game, String> whoFn;
    public String text;
    public Function<Game, String> textFn;
    public Opt l, r;
    public boolean once, q, story, arc, ev, press, ungone;
    public int season = -1, min = -1;
    public Predicate<Game> cond;

    // только для служебных карт (коронация, прозвище, концовка)
    public int pre;              // 1 — коронация, 2 — выбор прозвища
    public boolean end;          // карта концовки
    public String title;         // заголовок вместо имени персонажа
    public String portrait;      // предмет-портрет вместо персонажа
    public String traitA, traitB;

    Card(String who, String text, Opt l, Opt r) { this.who = who; this.text = text; this.l = l; this.r = r; }

    public Card id(String v) { id = v; return this; }
    public Card once() { once = true; return this; }
    public Card q() { q = true; return this; }
    public Card story() { story = true; return this; }
    public Card arc() { arc = true; return this; }
    public Card ev() { ev = true; return this; }
    public Card press() { press = true; return this; }
    public Card ungone() { ungone = true; return this; }
    public Card season(int s) { season = s; return this; }
    public Card min(int y) { min = y; return this; }
    public Card cond(Predicate<Game> c) { cond = c; return this; }
    public Card who(Function<Game, String> f) { whoFn = f; return this; }
    public Card text(Function<Game, String> f) { textFn = f; return this; }

    /** Одна кнопка вместо двух (коронация, концовка). */
    public boolean single() { return pre == 1 || end; }
}
