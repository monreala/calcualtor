package kingdom.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import kingdom.game.Data;
import kingdom.game.Game;
import kingdom.game.Store;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/** Летопись: события правления, свершения, концовки, династия. */
public class GuiChronicle extends GuiScreen {
    private static final int PAPER = 0xFFFBF4E0, INK = 0xFF2B2117, SOFT = 0xFF75654A, LOCKED = 0xFFA3937A, HEAD = 0xFF8A221D, RULE = 0x558A622B;

    private final GuiScreen parent;
    private Game game;
    private final List<Line> lines = new ArrayList<Line>();
    private int px, py, pw, ph, viewTop, viewH, contentH;
    private float scroll;
    private boolean armed;
    private final int[] rBack = new int[4], rReset = new int[4];

    private static final class Line {
        final String text; final int color, gap; final boolean head;
        Line(String text, int color, int gap, boolean head) { this.text = text; this.color = color; this.gap = gap; this.head = head; }
    }

    public GuiChronicle(GuiScreen parent) { this.parent = parent; }

    @Override
    public void initGui() {
        game = KingdomClient.game();
        pw = Math.min(320, width - 20);
        ph = height - 16;
        px = (width - pw) / 2;
        py = 8;
        viewTop = py + 30;
        viewH = ph - 30 - 30;
        int bw = fontRendererObj.getStringWidth("Назад") + 24;
        set(rBack, px + pw - 12 - bw, py + ph - 24, bw, 16);
        int rw = fontRendererObj.getStringWidth("Точно стереть всё?") + 20;
        set(rReset, px + 12, py + ph - 24, rw, 16);
        build();
    }

    private void build() {
        lines.clear();
        int w = pw - 28;
        Store s = game.store;
        head("Это правление");
        if (game.log.isEmpty()) para("Пока записывать нечего. Летописец скучает.", SOFT, w);
        for (Game.LogEntry e : game.log) para("Год " + e.year + ". " + e.text, INK, w);

        head("Свершения: " + count(s.ach, Data.ACH.keySet()) + " из " + Data.ACH.size());
        for (Map.Entry<String, String[]> e : Data.ACH.entrySet()) {
            boolean got = s.ach.contains(e.getKey());
            para((got ? "✔ " : "○ ") + e.getValue()[0], got ? INK : LOCKED, w);
            para("   " + e.getValue()[1], got ? SOFT : LOCKED, w);
        }

        head("Концовки: " + count(s.ends, Data.ENDINGS.keySet()) + " из " + Data.ENDINGS.size());
        for (Map.Entry<String, Data.Ending> e : Data.ENDINGS.entrySet())
            para(s.ends.contains(e.getKey()) ? "✔ " + e.getValue().name : "○ Не открыта", s.ends.contains(e.getKey()) ? INK : LOCKED, w);

        head("Династия");
        if (s.kings.isEmpty()) para("Вы первый в роду. Рекордов пока нет.", SOFT, w);
        for (Store.King k : s.kings) {
            para(k.name, INK, w);
            Data.Ending e = Data.ENDINGS.get(k.end);
            para("   " + k.years + " " + Game.plural(k.years, "год", "года", "лет") + " на троне. Конец: "
                + (e == null ? "?" : e.name.toLowerCase()), SOFT, w);
        }
        if (s.best > 0) para("Рекорд династии: " + s.best + " " + Game.plural(s.best, "год", "года", "лет") + ".", SOFT, w);

        contentH = 0;
        for (Line l : lines) contentH += l.gap + (l.head ? 13 : 10);
    }

    private static int count(Iterable<String> have, java.util.Set<String> all) {
        int n = 0;
        for (String id : have) if (all.contains(id)) n++;
        return n;
    }

    private void head(String t) { lines.add(new Line(t, HEAD, lines.isEmpty() ? 0 : 8, true)); }

    private void para(String t, int color, int w) {
        boolean first = true;
        for (Object o : fontRendererObj.listFormattedStringToWidth(t, w)) {
            lines.add(new Line((String) o, color, first ? 2 : 0, false));
            first = false;
        }
    }

    @Override
    public void drawScreen(int mx, int my, float partial) {
        drawDefaultBackground();
        drawRect(0, 0, width, height, 0xB0111930);
        drawRect(px + 3, py + 5, px + pw + 3, py + ph + 5, 0x60000000);
        drawRect(px, py, px + pw, py + ph, PAPER);

        GL11.glPushMatrix();
        GL11.glTranslatef(px + 14, py + 9, 0);
        GL11.glScalef(1.5f, 1.5f, 1);
        fontRendererObj.drawString("Летопись", 0, 0, HEAD);
        GL11.glPopMatrix();

        int max = Math.max(0, contentH - viewH);
        scroll = Math.max(0, Math.min(max, scroll));

        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int f = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(px * f, mc.displayHeight - (viewTop + viewH) * f, pw * f, viewH * f);
        int y = viewTop - Math.round(scroll);
        for (Line l : lines) {
            y += l.gap;
            if (y > viewTop - 12 && y < viewTop + viewH) {
                fontRendererObj.drawString(l.text, px + 14, y, l.color);
                if (l.head) drawRect(px + 14, y + 10, px + pw - 14, y + 11, RULE);
            }
            y += l.head ? 13 : 10;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (max > 0) {
            int barH = Math.max(16, viewH * viewH / contentH);
            int barY = viewTop + Math.round((viewH - barH) * (scroll / max));
            drawRect(px + pw - 6, viewTop, px + pw - 4, viewTop + viewH, 0x20000000);
            drawRect(px + pw - 6, barY, px + pw - 4, barY + barH, 0x808A622B);
        }

        button(rBack, "Назад", true, in(rBack, mx, my));
        button(rReset, armed ? "Точно стереть всё?" : "Стереть летопись", false, in(rReset, mx, my));
        super.drawScreen(mx, my, partial);
    }

    private void button(int[] r, String s, boolean solid, boolean hover) {
        int bg = solid ? (hover ? 0xFF4A3B2A : INK) : (hover ? 0x228A221D : 0);
        if (bg != 0) drawRect(r[0], r[1], r[0] + r[2], r[1] + r[3], bg);
        if (!solid) {
            int c = 0x998A221D;
            drawRect(r[0], r[1], r[0] + r[2], r[1] + 1, c);
            drawRect(r[0], r[1] + r[3] - 1, r[0] + r[2], r[1] + r[3], c);
            drawRect(r[0], r[1], r[0] + 1, r[1] + r[3], c);
            drawRect(r[0] + r[2] - 1, r[1], r[0] + r[2], r[1] + r[3], c);
        }
        fontRendererObj.drawString(s, r[0] + (r[2] - fontRendererObj.getStringWidth(s)) / 2, r[1] + 4, solid ? PAPER : HEAD);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0) scroll -= Math.signum(d) * 20;
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        super.mouseClicked(mx, my, button);
        if (button != 0) return;
        if (in(rBack, mx, my)) { mc.displayGuiScreen(parent); return; }
        if (in(rReset, mx, my)) {
            if (!armed) { armed = true; return; }
            game.store.reset();
            game.newReign();
            armed = false;
            build();
            scroll = 0;
        }
    }

    @Override
    protected void keyTyped(char ch, int key) {
        if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_L) { mc.displayGuiScreen(parent); return; }
        if (key == Keyboard.KEY_UP) scroll -= 20;
        if (key == Keyboard.KEY_DOWN) scroll += 20;
    }

    @Override
    public boolean doesGuiPauseGame() { return true; }

    private static void set(int[] r, int x, int y, int w, int h) { r[0] = x; r[1] = y; r[2] = w; r[3] = h; }
    private static boolean in(int[] r, int x, int y) { return x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3]; }
}
