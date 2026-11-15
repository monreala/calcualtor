package kingdom.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

/** Состояние правления и все правила игры. Не зависит от Minecraft. */
public final class Game {
    public static final int[] ZERO = {0, 0, 0, 0};

    public final Store store;
    public final Random rnd;

    // ── состояние правления ──
    public int[] st = {50, 50, 50, 50};
    public int turn;
    public final Map<String, String> flags = new HashMap<String, String>();
    /** id эффекта → оставшиеся сезоны, -1 — постоянный. Порядок добавления сохраняется. */
    public final Map<String, Integer> status = new LinkedHashMap<String, Integer>();
    public final Set<String> gone = new HashSet<String>();
    public final Set<String> seen = new HashSet<String>();
    public final List<String> recent = new ArrayList<String>();
    public final List<Queued> queue = new ArrayList<Queued>();
    public final List<LogEntry> log = new ArrayList<LogEntry>();
    public int lastArc;
    public String trait;
    public boolean female, started, ended;
    public String king = "";
    public String endId;

    // ── то, что видит игрок ──
    public Card cur;
    public String toast = "";
    public String hint = "";
    /** Свершения, открытые последним ходом (для звука и всплывашки). */
    public final List<String> unlocked = new ArrayList<String>();

    private final List<String> notices = new ArrayList<String>();

    public static final class Queued {
        public final String id; public final int at;
        Queued(String id, int at) { this.id = id; this.at = at; }
    }
    public static final class LogEntry {
        public final int year; public final String text;
        LogEntry(int year, String text) { this.year = year; this.text = text; }
    }
    /** Подсказка над шкалой: 0 — нет, 1 — мало, 2 — много, 3 — неизвестно. dir: +1/-1. */
    public static final class Pv {
        public final int kind, dir;
        Pv(int kind, int dir) { this.kind = kind; this.dir = dir; }
    }

    public Game(Store store, Random rnd) { this.store = store; this.rnd = rnd; }

    // ───────── удобства для карт ─────────
    public boolean has(String id) { return status.containsKey(id); }
    public boolean flag(String k) { return flags.containsKey(k); }
    public String flagS(String k) { return flags.get(k); }
    public int flagI(String k) { return Store.intOf(flags.get(k), 0); }
    public int year() { return turn / 4 + 1; }
    public int season() { return turn % 4; }
    public void removeQueued(String id) {
        for (Iterator<Queued> it = queue.iterator(); it.hasNext(); ) if (it.next().id.equals(id)) it.remove();
    }

    public String tx(String s) { return s == null ? "" : s.replace("{G}", female ? "государыня" : "государь"); }
    public String traitName(String id) {
        Data.Trait t = Data.TRAITS.get(id);
        return t == null ? "" : (female ? t.f : t.m);
    }
    public String kingName() { return trait == null ? king : king + " " + traitName(trait); }

    public String reignLine() {
        if (!started) return "коронация";
        return Data.SEASONS[season()].toLowerCase() + ", " + year() + "-й год правления";
    }

    // ───────── карта на экране ─────────
    public String whoId(Card c) { return c.whoFn != null ? c.whoFn.apply(this) : c.who; }
    public String cardTitle() {
        if (cur == null) return "";
        if (cur.title != null) return cur.title;
        Data.Who w = Data.WHO.get(whoId(cur));
        return w == null ? "" : w.name;
    }
    public String cardItem() {
        if (cur == null) return "minecraft:paper";
        if (cur.portrait != null) return cur.portrait;
        Data.Who w = Data.WHO.get(whoId(cur));
        return w == null ? "minecraft:paper" : w.item;
    }
    /** Цвет щита или 0, если карта-событие без щита. */
    public int cardShield() {
        if (cur == null || cur.end) return 0;
        Data.Who w = Data.WHO.get(whoId(cur));
        return w == null ? 0xFF1F2B4D : w.shield;
    }
    public boolean cardDark() { return cur != null && (cur.ev || cur.end || cardShield() == 0); }
    public boolean cardStory() { return cur != null && (cur.story || cur.arc); }
    public String cardText() {
        if (cur == null) return "";
        return tx(cur.textFn != null ? cur.textFn.apply(this) : cur.text);
    }

    // ───────── эффекты ─────────
    int[] adj(int[] fx) {
        double k = 1 + Math.min(.6, Math.max(0, year() - 3) * .06);
        int[] f = new int[4];
        for (int i = 0; i < 4; i++) f[i] = (int) Math.round(fx[i] * k);
        if ("warrior".equals(trait) && f[2] > 0) f[2] = (int) Math.round(f[2] * 1.5);
        if ("pious".equals(trait) && f[0] < 0) f[0] = (int) Math.round(f[0] * .5);
        if ("generous".equals(trait) && f[1] > 0) f[1] = (int) Math.round(f[1] * 1.5);
        if ("miser".equals(trait) && f[3] < 0) f[3] = (int) Math.round(f[3] * .67);
        if (has("wall") && f[2] < 0) f[2] = (int) Math.round(f[2] * .7);
        return f;
    }
    private void addFx(int[] fx) { for (int i = 0; i < 4; i++) st[i] += fx[i]; }

    /** Что покажут точки над шкалами, если выбрать сторону side ('l' или 'r'). */
    public Pv[] preview(char side) {
        Pv[] res = new Pv[4];
        if (cur == null || cur.pre != 0 || cur.end) return res;
        Opt opt = side == 'l' ? cur.l : cur.r;
        if (opt.roll != null || opt.dyn != null) {
            List<int[]> cs = new ArrayList<int[]>();
            if (opt.fx != null) cs.add(opt.fx);
            if (opt.pv != null) cs.add(opt.pv);
            if (opt.roll != null) {
                cs.add(opt.roll.win.fx != null ? opt.roll.win.fx : ZERO);
                cs.add(opt.roll.lose.fx != null ? opt.roll.lose.fx : ZERO);
            }
            for (int i = 0; i < 4; i++) for (int[] f : cs) if (f[i] != 0) { res[i] = new Pv(3, 0); break; }
        } else {
            int[] f = adj(opt.fx != null ? opt.fx : ZERO);
            for (int i = 0; i < 4; i++) if (f[i] != 0) res[i] = new Pv(Math.abs(f[i]) >= 15 ? 2 : 1, f[i] > 0 ? 1 : -1);
        }
        return res;
    }
    /** Видно ли направление (Тайный приказ или прозвище «Хитрый»). */
    public boolean seesDirection() { return has("spy"); }

    private void unlock(String id) {
        if (store.ach.contains(id)) return;
        store.ach.add(id);
        unlocked.add(id);
        notices.add("Свершение: «" + Data.ACH.get(id)[0] + "».");
    }
    private void addStatus(String id, int n) {
        status.put(id, n);
        if (has("wall") && has("mills")) unlock("builder");
    }
    private void pushNext(List<Opt.Next> next, int base) {
        for (Opt.Next n : next) {
            boolean dup = false;
            for (Queued q : queue) if (q.id.equals(n.id)) { dup = true; break; }
            if (!dup) queue.add(new Queued(n.id, base + n.delay));
        }
    }

    Opt resolve(Opt opt) {
        Opt o = opt;
        if (o.dyn != null) o = o.merge(o.dyn.apply(this));
        if (o.roll != null) {
            double p = o.roll.p.applyAsDouble(this);
            if ("lucky".equals(trait)) p = Math.min(.95, p + .2);
            o = o.merge(rnd.nextDouble() < p ? o.roll.win : o.roll.lose);
        }
        return o;
    }
    private void applyOpt(Opt o) {
        if (o.fx != null) addFx(adj(o.fx));
        if (o.set != null) flags.put(o.set, "1");
        if (o.addId != null) addStatus(o.addId, o.addN);
        if (o.rem != null) status.remove(o.rem);
        if (o.next != null) pushNext(o.next, turn);
        if (o.log != null) log.add(new LogEntry(year(), o.log));
        if (o.ach != null) unlock(o.ach);
        if (o.act != null) o.act.accept(this);
    }
    private String tick(int season) {
        StringBuilder msgs = new StringBuilder();
        for (String id : new ArrayList<String>(status.keySet())) {
            Data.Status d = Data.STATUS.get(id);
            if (d == null || !status.containsKey(id)) continue;
            if (d.tick != null) { int[] f = d.tick.apply(this, season); if (f != null) addFx(f); }
            int n = status.get(id);
            if (n >= 0) {
                n--;
                if (n <= 0) {
                    status.remove(id);
                    if (d.off != null) { if (msgs.length() > 0) msgs.append(' '); msgs.append(d.off); }
                } else status.put(id, n);
            }
        }
        return msgs.toString();
    }
    private String fatal() {
        for (int pass = 0; pass < 4; pass++) {
            int i = -1;
            for (int k = 0; k < 4; k++) if (st[k] <= 0 || st[k] >= 100) { i = k; break; }
            if (i < 0) return null;
            if (has("firebird")) {
                status.remove("firebird");
                st[i] = 50;
                notices.add("Перо Жар-птицы вспыхнуло и сгорело — беда прошла стороной.");
                continue;
            }
            return Data.STAT_KEYS[i] + (st[i] <= 0 ? "0" : "100");
        }
        return null;
    }

    // ───────── выбор следующей карты ─────────
    boolean elig(Card c) {
        if (c.q) return false;
        if (c.once && seen.contains(c.id)) return false;
        String w = c.whoFn != null ? null : c.who;
        if (w != null && gone.contains(w)) return false;
        if (female && ("princess".equals(w) || "queen".equals(w))) return false;
        if (c.season >= 0 && c.season != season()) return false;
        if (c.min >= 0 && year() < c.min) return false;
        return c.cond == null || c.cond.test(this);
    }
    private boolean isRecent(Card c) { return recent.contains(c.id); }
    private Card take(Card c) {
        seen.add(c.id);
        recent.add(c.id);
        if (recent.size() > 20) recent.remove(0);
        return c;
    }
    private <T> T pickRandom(List<T> l) { return l.get(rnd.nextInt(l.size())); }

    Card pick() {
        int t = turn;
        if (t >= 48 && !seen.contains("jubilee")) return take(Cards.BY_ID.get("jubilee"));
        Collections.sort(queue, new Comparator<Queued>() {
            @Override public int compare(Queued a, Queued b) { return Integer.compare(a.at, b.at); }
        });
        while (!queue.isEmpty() && queue.get(0).at <= t) {
            Card c = Cards.BY_ID.get(queue.remove(0).id);
            if (c == null) continue;
            if (c.ungone) gone.remove(c.who);
            String w = c.whoFn != null ? null : c.who;
            if ((c.cond == null || c.cond.test(this)) && !(w != null && gone.contains(w))) return take(c);
        }
        if (t >= 3 && t - lastArc >= 5 && rnd.nextDouble() < .3) {
            List<Card> arcs = new ArrayList<Card>();
            for (Card c : Cards.ALL) if (c.arc && elig(c)) arcs.add(c);
            if (!arcs.isEmpty()) { lastArc = t; return take(pickRandom(arcs)); }
        }
        if (rnd.nextDouble() < .24) {
            List<Card> ev = new ArrayList<Card>();
            for (Card c : Cards.ALL) if (c.ev && elig(c) && !isRecent(c)) ev.add(c);
            if (!ev.isEmpty()) return take(pickRandom(ev));
        }
        List<Card> press = new ArrayList<Card>();
        for (Card c : Cards.ALL) if (c.press && elig(c) && !isRecent(c)) press.add(c);
        if (!press.isEmpty() && rnd.nextDouble() < .55) return take(pickRandom(press));
        List<Card> pool = new ArrayList<Card>();
        for (Card c : Cards.ALL) if (isBase(c) && !isRecent(c)) pool.add(c);
        if (pool.isEmpty()) for (Card c : Cards.ALL) if (isBase(c)) pool.add(c);
        return take(pickRandom(pool));
    }
    private boolean isBase(Card c) { return !c.arc && !c.ev && !c.press && elig(c); }

    // ───────── новое правление ─────────
    public void newReign() {
        Store.Last last = store.last;
        female = last != null && last.heir;
        String name = female ? "Агата" : Data.NAMES[rnd.nextInt(Data.NAMES.length)];
        Integer cnt = store.names.get(name);
        store.names.put(name, cnt == null ? 1 : cnt + 1);
        store.reigns++;
        st = new int[]{50, 50, 50, 50};
        turn = 0; flags.clear(); status.clear(); gone.clear(); seen.clear(); recent.clear(); queue.clear(); log.clear();
        lastArc = 0; trait = null; started = false; ended = false; endId = null;
        king = name + " " + roman(store.names.get(name));
        String legacy = "";
        if (last != null) {
            String e = last.end;
            int k = -1;
            for (int i = 0; i < 4; i++) if (e.startsWith(Data.STAT_KEYS[i])) k = i;
            if (k >= 0) st[k] = e.endsWith("100") ? 58 : 42;
            else if ("assassin".equals(e)) st[2] = 40;
            else if ("jester".equals(e)) { st[0] = 40; st[1] = 60; }
            else st = new int[]{55, 55, 55, 55};
            String lg = Data.LEGACY.get(e);
            legacy = lg == null ? "" : lg;
            List<String> got = new ArrayList<String>();
            if (last.wall) { status.put("wall", -1); got.add("каменная стена"); }
            if (last.mills) { status.put("mills", -1); got.add("мельницы"); }
            if (!got.isEmpty()) legacy += " От предшественника остались " + String.join(" и ", got) + ".";
            if (female) legacy = "Король сам назвал вас наследницей. " + legacy;
        }
        toast = "";
        hint = store.reigns > 1 ? "" : "Перетащите карту влево или вправо либо нажмите кнопку. Стрелки и A/D тоже работают.";
        Card intro = new Card("herald",
            "Да здравствует " + (female ? "королева " : "король ") + king + "! "
                + (last != null ? legacy : "Церковь, народ, армия и казна ждут ваших решений. Не дайте ни одной шкале опустеть — или переполниться."),
            Opt.o("Принять корону"), Opt.o("Принять корону"));
        intro.id = "#intro";
        intro.pre = 1;
        cur = intro;
        save();
    }

    private Card traitCard(String a, String b) {
        Card c = new Card("chronicler",
            "Летописец макает перо в чернила: «Под каким прозвищем вас запомнят?»\n\n"
                + traitName(a) + " — " + Data.TRAITS.get(a).d + ".\n"
                + traitName(b) + " — " + Data.TRAITS.get(b).d + ".",
            Opt.o(traitName(a)), Opt.o(traitName(b)));
        c.id = "#trait";
        c.pre = 2;
        c.traitA = a; c.traitB = b;
        return c;
    }
    private Card newTraitCard() {
        List<String> ids = new ArrayList<String>(Data.TRAITS.keySet());
        String a = pickRandom(ids);
        String b = a;
        while (b.equals(a)) b = pickRandom(ids);
        return traitCard(a, b);
    }

    // ───────── ход ─────────
    /** Игрок выбрал сторону 'l' или 'r'. */
    public void choose(char side) {
        unlocked.clear();
        notices.clear();
        if (cur == null) return;
        if (ended) { newReign(); return; }
        if (cur.pre == 1) { cur = newTraitCard(); toast = ""; save(); return; }
        if (cur.pre == 2) {
            trait = side == 'l' ? cur.traitA : cur.traitB;
            started = true;
            if ("cunning".equals(trait)) status.put("spy", -1);
            hint = store.reigns > 1 ? "" : "Точки над шкалами показывают, что изменится. Кубик — решение с риском. Карты с печатью — сюжетные.";
            toast = "";
            cur = pick();
            save();
            return;
        }
        if (side == 'r' && cur.single()) side = 'l';
        Opt o = resolve(side == 'l' ? cur.l : cur.r);
        applyOpt(o);
        int season = season();
        turn++;
        String tmsg = tick(season);
        if (turn == 12) notices.add("Годы идут: теперь каждое решение весит больше.");
        if (turn == 20) unlock("y5");
        if (turn == 40) unlock("y10");
        String end = o.end != null ? o.end : fatal();
        if (end == null && turn >= 80) end = "oldage";
        String msg = o.msg != null ? o.msg : "";
        if (o.fortune) {
            int y = 3 + rnd.nextInt(12);
            msg = "Гадалка шепчет: «Ещё " + y + " " + plural(y, "год", "года", "лет") + "… если не ошибётесь».";
        }
        StringBuilder sb = new StringBuilder(msg);
        for (String s : new String[]{tmsg}) if (!s.isEmpty()) { if (sb.length() > 0) sb.append(' '); sb.append(s); }
        for (String s : notices) { if (sb.length() > 0) sb.append(' '); sb.append(s); }
        toast = sb.toString();
        if (end != null) finish(end);
        else cur = pick();
        save();
    }

    private void finish(String end) {
        Data.Ending e = Data.ENDINGS.get(end);
        int years = turn / 4;
        if ("oldage".equals(end)) unlock("old");
        boolean record = years > store.best;
        if (record) store.best = years;
        boolean newEnd = store.ends.add(end);
        store.kings.add(0, new Store.King(kingName(), years, end));
        while (store.kings.size() > 15) store.kings.remove(store.kings.size() - 1);
        Store.Last l = new Store.Last();
        l.end = end; l.heir = has("heir") && !female; l.wall = has("wall"); l.mills = has("mills");
        store.last = l;
        ended = true;
        endId = end;
        cur = endCard(end);
        List<String> bits = new ArrayList<String>();
        if (record && years > 0) bits.add("Новый рекорд династии.");
        if (newEnd) bits.add("Открыта концовка «" + e.name + "».");
        if (!log.isEmpty()) {
            StringBuilder sb = new StringBuilder("В летописи: ");
            for (int i = 0; i < log.size(); i++) {
                String t = log.get(i).text;
                if (i > 0) sb.append("; ");
                sb.append(Character.toLowerCase(t.charAt(0))).append(t.substring(1));
            }
            bits.add(sb.append('.').toString());
        }
        hint = String.join(" ", bits);
    }
    private Card endCard(String end) {
        Data.Ending e = Data.ENDINGS.get(end);
        int years = turn / 4;
        Card c = new Card(null, e.text, Opt.o("Короновать " + (store.last != null && store.last.heir ? "Агату" : "наследника")), Opt.o(""));
        c.id = "#end";
        c.end = true;
        c.portrait = e.item;
        c.title = kingName() + " " + (female ? "правила " : "правил ") + years + " " + plural(years, "год", "года", "лет");
        return c;
    }

    // ───────── сохранение партии ─────────
    public void save() {
        store.game = toProps();
        store.save();
    }

    Properties toProps() {
        Properties p = new Properties();
        p.setProperty("game.st", st[0] + "," + st[1] + "," + st[2] + "," + st[3]);
        p.setProperty("game.turn", String.valueOf(turn));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : flags.entrySet()) { if (sb.length() > 0) sb.append(';'); sb.append(e.getKey()).append('=').append(e.getValue()); }
        p.setProperty("game.flags", sb.toString());
        sb.setLength(0);
        for (Map.Entry<String, Integer> e : status.entrySet()) { if (sb.length() > 0) sb.append(','); sb.append(e.getKey()).append(':').append(e.getValue()); }
        p.setProperty("game.status", sb.toString());
        p.setProperty("game.gone", Store.join(gone));
        p.setProperty("game.seen", Store.join(seen));
        p.setProperty("game.recent", Store.join(recent));
        sb.setLength(0);
        for (Queued q : queue) { if (sb.length() > 0) sb.append(','); sb.append(q.id).append(':').append(q.at); }
        p.setProperty("game.queue", sb.toString());
        p.setProperty("game.log.n", String.valueOf(log.size()));
        for (int i = 0; i < log.size(); i++) {
            p.setProperty("game.log." + i + ".y", String.valueOf(log.get(i).year));
            p.setProperty("game.log." + i + ".t", log.get(i).text);
        }
        p.setProperty("game.lastArc", String.valueOf(lastArc));
        if (trait != null) p.setProperty("game.trait", trait);
        p.setProperty("game.female", female ? "1" : "0");
        p.setProperty("game.started", started ? "1" : "0");
        p.setProperty("game.king", king);
        p.setProperty("game.toast", toast);
        p.setProperty("game.hint", hint);
        if (ended) p.setProperty("game.end", endId);
        if (cur != null) {
            if (cur.pre == 1) { p.setProperty("game.cur", "#intro"); p.setProperty("game.introText", cur.text); }
            else if (cur.pre == 2) p.setProperty("game.cur", "#trait:" + cur.traitA + ":" + cur.traitB);
            else if (!cur.end) p.setProperty("game.cur", cur.id);
        }
        return p;
    }

    /** Восстановить партию. false — сохранения нет или оно не подходит. */
    public boolean restore(Properties p) {
        if (p == null || p.getProperty("game.king") == null) return false;
        try {
            List<String> s = Store.split(p.getProperty("game.st"), ',');
            if (s.size() != 4) return false;
            for (int i = 0; i < 4; i++) st[i] = Integer.parseInt(s.get(i));
            turn = Store.intOf(p.getProperty("game.turn"), 0);
            flags.clear();
            for (String kv : Store.split(p.getProperty("game.flags"), ';')) {
                int i = kv.indexOf('=');
                if (i > 0) flags.put(kv.substring(0, i), kv.substring(i + 1));
            }
            status.clear();
            for (String kv : Store.split(p.getProperty("game.status"), ',')) {
                int i = kv.lastIndexOf(':');
                if (i > 0 && Data.STATUS.containsKey(kv.substring(0, i))) status.put(kv.substring(0, i), Store.intOf(kv.substring(i + 1), -1));
            }
            gone.clear(); gone.addAll(Store.split(p.getProperty("game.gone"), ','));
            seen.clear(); seen.addAll(Store.split(p.getProperty("game.seen"), ','));
            recent.clear(); recent.addAll(Store.split(p.getProperty("game.recent"), ','));
            queue.clear();
            for (String kv : Store.split(p.getProperty("game.queue"), ',')) {
                int i = kv.lastIndexOf(':');
                if (i > 0 && Cards.BY_ID.containsKey(kv.substring(0, i))) queue.add(new Queued(kv.substring(0, i), Store.intOf(kv.substring(i + 1), turn)));
            }
            log.clear();
            int n = Store.intOf(p.getProperty("game.log.n"), 0);
            for (int i = 0; i < n; i++) {
                String t = p.getProperty("game.log." + i + ".t");
                if (t != null) log.add(new LogEntry(Store.intOf(p.getProperty("game.log." + i + ".y"), 1), t));
            }
            lastArc = Store.intOf(p.getProperty("game.lastArc"), 0);
            trait = p.getProperty("game.trait");
            if (trait != null && !Data.TRAITS.containsKey(trait)) trait = null;
            female = "1".equals(p.getProperty("game.female"));
            started = "1".equals(p.getProperty("game.started"));
            king = p.getProperty("game.king");
            toast = p.getProperty("game.toast", "");
            hint = p.getProperty("game.hint", "");
            endId = p.getProperty("game.end");
            ended = endId != null && Data.ENDINGS.containsKey(endId);
            String c = p.getProperty("game.cur", "");
            if (ended) cur = endCard(endId);
            else if (c.equals("#intro")) {
                cur = new Card("herald", p.getProperty("game.introText", "Да здравствует " + king + "!"), Opt.o("Принять корону"), Opt.o("Принять корону"));
                cur.id = "#intro"; cur.pre = 1;
            } else if (c.startsWith("#trait:")) {
                String[] ab = c.split(":");
                cur = ab.length == 3 && Data.TRAITS.containsKey(ab[1]) && Data.TRAITS.containsKey(ab[2]) ? traitCard(ab[1], ab[2]) : newTraitCard();
            } else if (Cards.BY_ID.containsKey(c) && started) cur = Cards.BY_ID.get(c);
            else if (started) cur = pick();
            else cur = newTraitCard();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Загрузить сохранённую партию или начать новое правление. */
    public static Game open(Store store, Random rnd) {
        Game g = new Game(store, rnd);
        if (!g.restore(store.game)) g.newReign();
        return g;
    }

    // ───────── мелочи ─────────
    public static String roman(int n) {
        int[] v = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] s = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < v.length; i++) while (n >= v[i]) { r.append(s[i]); n -= v[i]; }
        return r.toString();
    }
    public static String plural(int n, String a, String b, String c) {
        int m10 = n % 10, m100 = n % 100;
        if (m10 == 1 && m100 != 11) return a;
        if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) return b;
        return c;
    }
}
