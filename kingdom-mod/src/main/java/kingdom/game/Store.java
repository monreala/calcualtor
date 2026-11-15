package kingdom.game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Всё, что переживает правление: рекорды, свершения, концовки, династия и текущая партия. */
public final class Store {
    public int best, reigns;
    public final Map<String, Integer> names = new LinkedHashMap<String, Integer>();
    public final Set<String> ach = new LinkedHashSet<String>();
    public final Set<String> ends = new LinkedHashSet<String>();
    public final List<King> kings = new ArrayList<King>();
    public Last last;
    /** Сохранённая незаконченная партия (ключи «game.*»). */
    public Properties game;

    private final File file;

    public static final class King {
        public final String name, end; public final int years;
        public King(String name, int years, String end) { this.name = name; this.years = years; this.end = end; }
    }
    public static final class Last {
        public String end; public boolean heir, wall, mills;
    }

    public Store(File file) { this.file = file; }

    public static Store load(File file) {
        Store s = new Store(file);
        if (file == null || !file.isFile()) return s;
        Properties p = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            p.load(in);
        } catch (IOException e) {
            return s;
        } finally {
            if (in != null) try { in.close(); } catch (IOException ignored) {}
        }
        try {
            s.best = intOf(p.getProperty("best"), 0);
            s.reigns = intOf(p.getProperty("reigns"), 0);
            for (String kv : split(p.getProperty("names"), ',')) {
                int i = kv.indexOf(':');
                if (i > 0) s.names.put(kv.substring(0, i), intOf(kv.substring(i + 1), 1));
            }
            s.ach.addAll(split(p.getProperty("ach"), ','));
            s.ends.addAll(split(p.getProperty("ends"), ','));
            int n = intOf(p.getProperty("kings.n"), 0);
            for (int i = 0; i < n; i++) {
                String nm = p.getProperty("kings." + i + ".name");
                if (nm != null) s.kings.add(new King(nm, intOf(p.getProperty("kings." + i + ".years"), 0), p.getProperty("kings." + i + ".end", "")));
            }
            if (p.getProperty("last.end") != null) {
                s.last = new Last();
                s.last.end = p.getProperty("last.end");
                s.last.heir = "1".equals(p.getProperty("last.heir"));
                s.last.wall = "1".equals(p.getProperty("last.wall"));
                s.last.mills = "1".equals(p.getProperty("last.mills"));
            }
            Properties g = new Properties();
            for (String k : p.stringPropertyNames()) if (k.startsWith("game.")) g.setProperty(k, p.getProperty(k));
            if (!g.isEmpty()) s.game = g;
        } catch (RuntimeException e) {
            return new Store(file); // повреждённый файл — начинаем с чистого листа
        }
        return s;
    }

    public void save() {
        if (file == null) return;
        Properties p = new Properties();
        p.setProperty("best", String.valueOf(best));
        p.setProperty("reigns", String.valueOf(reigns));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : names.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(e.getKey()).append(':').append(e.getValue());
        }
        p.setProperty("names", sb.toString());
        p.setProperty("ach", join(ach));
        p.setProperty("ends", join(ends));
        p.setProperty("kings.n", String.valueOf(kings.size()));
        for (int i = 0; i < kings.size(); i++) {
            King k = kings.get(i);
            p.setProperty("kings." + i + ".name", k.name);
            p.setProperty("kings." + i + ".years", String.valueOf(k.years));
            p.setProperty("kings." + i + ".end", k.end);
        }
        if (last != null) {
            p.setProperty("last.end", last.end);
            p.setProperty("last.heir", last.heir ? "1" : "0");
            p.setProperty("last.wall", last.wall ? "1" : "0");
            p.setProperty("last.mills", last.mills ? "1" : "0");
        }
        if (game != null) for (String k : game.stringPropertyNames()) p.setProperty(k, game.getProperty(k));
        File dir = file.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) return;
        File tmp = new File(file.getPath() + ".tmp");
        OutputStream out = null;
        try {
            out = new FileOutputStream(tmp);
            p.store(out, "Tridesyatoe korolevstvo");
            out.close();
            out = null;
            if (file.exists() && !file.delete()) return;
            if (!tmp.renameTo(file)) return;
        } catch (IOException ignored) {
        } finally {
            if (out != null) try { out.close(); } catch (IOException ignored) {}
        }
    }

    /** Стереть всё (кнопка в летописи). */
    public void reset() {
        best = 0; reigns = 0; names.clear(); ach.clear(); ends.clear(); kings.clear(); last = null; game = null;
    }

    static int intOf(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }
    static List<String> split(String s, char sep) {
        List<String> r = new ArrayList<String>();
        if (s == null || s.isEmpty()) return r;
        for (String part : s.split(java.util.regex.Pattern.quote(String.valueOf(sep)))) if (!part.isEmpty()) r.add(part);
        return r;
    }
    static String join(Iterable<String> it) {
        StringBuilder sb = new StringBuilder();
        for (String s : it) { if (sb.length() > 0) sb.append(','); sb.append(s); }
        return sb.toString();
    }
}
