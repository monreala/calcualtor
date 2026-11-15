package kingdom.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kingdom.game.Card;
import kingdom.game.Data;
import kingdom.game.Game;
import kingdom.game.Opt;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/** Экран игры: шкалы слева, карта по центру, эффекты справа, ответы по бокам карты. */
public class GuiKingdom extends GuiScreen {
    static final ResourceLocation ICONS = new ResourceLocation("kingdom", "textures/gui/icons.png");
    static final ResourceLocation BACK = new ResourceLocation("kingdom", "textures/gui/back.png");

    // палитра
    static final int BG = 0xE8111930, PANEL = 0x26FFFFFF, PANEL_LINE = 0x33FFFFFF;
    static final int INK = 0xFFF1E9D2, SOFT = 0xFFA4ADC6, GOLD = 0xFFE0B449, RED = 0xFFE0655B, GREEN = 0xFF63BD92;
    static final int DIM = 0xFF36426A, BTN = 0xFFF1E9D2, BTN_INK = 0xFF1B2440;
    static final int PARCH_TOP = 0xFFFCF5E1, PARCH_BOT = 0xFFF0E1BC, CARD_INK = 0xFF2B2117, FRAME = 0x558A622B;
    static final int NIGHT_TOP = 0xFF34467E, NIGHT_BOT = 0xFF1A2444, NIGHT_INK = 0xFFF3EBD3, NIGHT_FRAME = 0x8CE0B449;

    private Game game;
    private final float[] disp = new float[4];
    private final long[] flashAt = new long[4];
    private final int[] flashDir = new int[4];

    // перетаскивание и анимации
    private boolean dragging;
    private int dragStartX;
    private float dragDx;
    private long springStart;
    private float springFrom;
    private char flySide;
    private long flyStart;
    private float flyFrom;
    private long dealStart;
    private String lastCardId;

    // разметка
    private int cardX, cardY, cardW, cardH, colW;
    private final int[] rL = new int[4], rR = new int[4], rLog = new int[4];
    private List<String> linesL = new ArrayList<String>(), linesR = new ArrayList<String>();
    private final List<int[]> chipRects = new ArrayList<int[]>();
    private final List<String> chipIds = new ArrayList<String>();

    private static final Map<String, ItemStack> STACKS = new HashMap<String, ItemStack>();

    @Override
    public void initGui() {
        game = KingdomClient.game();
        for (int i = 0; i < 4; i++) disp[i] = clamp(game.st[i]);
        Keyboard.enableRepeatEvents(false);
        layout();
    }

    @Override
    public boolean doesGuiPauseGame() { return true; }

    private void layout() {
        cardH = Math.min(height - 62, 230);
        cardW = Math.round(cardH * 0.72f);
        int maxW = width - 2 * 104;
        if (cardW > maxW) { cardW = Math.max(90, maxW); cardH = Math.round(cardW / 0.72f); }
        cardX = (width - cardW) / 2;
        cardY = 26;
        colW = cardX - 16;
        String log = "Летопись";
        int lw = fontRendererObj.getStringWidth(log) + 14;
        set(rLog, width - 8 - lw, 5, lw, 14);
        layoutButtons();
    }

    private void layoutButtons() {
        Card c = game.cur;
        int bw = colW;
        int inner = bw - 22;
        linesL = wrap(label(c.l), inner);
        linesR = wrap(label(c.single() ? c.l : c.r), inner);
        int hL = Math.max(30, linesL.size() * 10 + 14), hR = Math.max(30, linesR.size() * 10 + 14);
        int bottom = cardY + cardH;
        set(rL, 8, bottom - hL, bw, hL);
        set(rR, width - 8 - bw, bottom - hR, bw, hR);
    }

    private String label(Opt o) { return o.t == null ? "" : o.t; }

    // ───────── отрисовка ─────────
    @Override
    public void drawScreen(int mx, int my, float partial) {
        long now = System.currentTimeMillis();
        stepAnimations(now);
        if (game.cur != null && !game.cur.id.equals(lastCardId)) { lastCardId = game.cur.id; layoutButtons(); }

        drawDefaultBackground();
        drawRect(0, 0, width, height, BG);

        char hover = 0;
        if (flySide == 0 && !dragging) {
            if (in(rL, mx, my)) hover = 'l';
            else if (in(rR, mx, my)) hover = 'r';
        }
        char pvSide = hover;
        if (dragging && Math.abs(dragDx) > 18) pvSide = dragDx < 0 ? 'l' : 'r';
        if (game.cur.single()) pvSide = 0;

        drawLeftColumn(pvSide, now);
        drawRightColumn(mx, my);
        drawCardArea(now);
        drawChoiceButton(rL, linesL, game.cur.l, hover == 'l' || pvSide == 'l', true);
        Opt rightOpt = game.cur.single() ? game.cur.l : game.cur.r;
        drawChoiceButton(rR, linesR, rightOpt, hover == 'r' || pvSide == 'r', false);
        drawToast();
        drawChipTooltip(mx, my);
        super.drawScreen(mx, my, partial);
    }

    private void drawLeftColumn(char pvSide, long now) {
        int x = 8;
        String title = "Тридесятое королевство";
        float ts = Math.min(1f, colW / (float) fontRendererObj.getStringWidth(title));
        GL11.glPushMatrix();
        GL11.glTranslatef(x, 7, 0);
        GL11.glScalef(ts, ts, 1);
        fontRendererObj.drawStringWithShadow(title, 0, 0, GOLD);
        GL11.glPopMatrix();

        int y = 22;
        for (String s : wrap(game.kingName(), colW)) { fontRendererObj.drawString(s, x, y, INK); y += 10; }
        for (String s : wrap(game.reignLine(), colW)) { fontRendererObj.drawString(s, x, y, SOFT); y += 10; }
        y += 6;

        Game.Pv[] pv = pvSide == 0 ? new Game.Pv[4] : game.preview(pvSide);
        boolean dirs = game.seesDirection();
        int panelBottom = y + 4 * 21 + 4;
        drawPanel(x - 3, y - 3, x + colW + 3, panelBottom);
        for (int i = 0; i < 4; i++) {
            int ry = y + i * 21;
            float f = disp[i] / 100f;
            int fill = INK;
            long fa = now - flashAt[i];
            if (fa < 900) fill = flashDir[i] > 0 ? GOLD : RED;
            // значок: тусклый целиком + заполненная часть снизу
            mc.getTextureManager().bindTexture(ICONS);
            color(DIM);
            blit(x, ry, 16, 16, i * 16, 0, 16, 16, 64);
            color(fill);
            float fh = 16 * f;
            blit(x, ry + 16 - fh, 16, fh, i * 16, 16 - fh, 16, fh, 64);
            GL11.glColor4f(1, 1, 1, 1);
            // подпись и полоска
            fontRendererObj.drawString(Data.STAT_NAMES[i], x + 21, ry, SOFT);
            int bx = x + 21, bw = colW - 21 - 14, by = ry + 11;
            drawRect(bx, by, bx + bw, by + 3, DIM);
            drawRect(bx, by, bx + Math.round(bw * f), by + 3, fill);
            // подсказка
            if (pv[i] != null) drawPreview(x + colW - 5, ry + 8, pv[i], dirs);
        }
    }

    private void drawPreview(int cx, int cy, Game.Pv p, boolean dirs) {
        if (p.kind == 3) {
            fontRendererObj.drawString("?", cx - fontRendererObj.getStringWidth("?") / 2, cy - 4, INK);
            return;
        }
        if (dirs) {
            int col = p.dir > 0 ? GREEN : RED;
            int rows = p.kind == 2 ? 4 : 3;
            for (int r = 0; r < rows; r++) {
                int half = r;
                int yy = p.dir > 0 ? cy - rows / 2 + r : cy + rows / 2 - r;
                drawRect(cx - half, yy, cx + half + 1, yy + 1, col);
            }
            return;
        }
        int s = p.kind == 2 ? 3 : 1;
        drawRect(cx - s, cy - s, cx + s + 1, cy + s + 1, INK);
    }

    private void drawRightColumn(int mx, int my) {
        // кнопка летописи
        boolean h = in(rLog, mx, my);
        drawRect(rLog[0], rLog[1], rLog[0] + rLog[2], rLog[1] + rLog[3], h ? 0x40FFFFFF : PANEL);
        outline(rLog, PANEL_LINE);
        String log = "Летопись";
        fontRendererObj.drawString(log, rLog[0] + (rLog[2] - fontRendererObj.getStringWidth(log)) / 2, rLog[1] + 3, INK);

        // эффекты
        int x = width - 8 - colW;
        int y = 26;
        chipRects.clear();
        chipIds.clear();
        for (Map.Entry<String, Integer> e : game.status.entrySet()) {
            Data.Status d = Data.STATUS.get(e.getKey());
            if (d == null) continue;
            String name = d.name + (e.getValue() > 0 ? " (" + e.getValue() + ")" : "");
            int w = Math.min(colW, fontRendererObj.getStringWidth(name) + 18);
            int[] r = {x, y, w, 12};
            boolean hv = in(r, mx, my);
            drawRect(r[0], r[1], r[0] + r[2], r[1] + r[3], hv ? 0x40FFFFFF : PANEL);
            drawItem(d.item, x + 2, y + 2, 0.5f);
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(name, colW - 16), x + 14, y + 2, INK);
            chipRects.add(r);
            chipIds.add(e.getKey());
            y += 14;
        }
        // подсказка (на первом правлении и в конце)
        if (game.hint != null && !game.hint.isEmpty()) {
            y += 4;
            int limit = rR[1] - 6;
            for (String s : wrap(game.hint, colW)) {
                if (y + 9 > limit) break;
                fontRendererObj.drawString(s, x, y, SOFT);
                y += 10;
            }
        }
    }

    private void drawChipTooltip(int mx, int my) {
        for (int i = 0; i < chipRects.size(); i++) {
            if (!in(chipRects.get(i), mx, my)) continue;
            Data.Status d = Data.STATUS.get(chipIds.get(i));
            List<String> lines = wrap(d.desc, 150);
            int w = 0;
            for (String s : lines) w = Math.max(w, fontRendererObj.getStringWidth(s));
            int h = lines.size() * 10 + 6;
            int tx = Math.min(mx + 8, width - w - 12), ty = Math.min(my + 10, height - h - 4);
            tx = Math.max(4, tx);
            GL11.glPushMatrix();
            GL11.glTranslatef(0, 0, 400);
            drawRect(tx - 4, ty - 3, tx + w + 4, ty + h - 3, 0xF01B2440);
            outline(new int[]{tx - 4, ty - 3, w + 8, h}, 0x80E0B449);
            for (int k = 0; k < lines.size(); k++) fontRendererObj.drawString(lines.get(k), tx, ty + k * 10, INK);
            GL11.glPopMatrix();
        }
    }

    private void drawToast() {
        if (game.toast == null || game.toast.isEmpty()) return;
        int y = cardY + cardH + 7;
        int maxW = Math.min(width - 16, 380);
        List<String> lines = wrap(game.toast, maxW);
        for (int i = 0; i < lines.size() && y + 9 <= height; i++, y += 10) {
            String s = "§o" + lines.get(i);
            fontRendererObj.drawString(s, (width - fontRendererObj.getStringWidth(s)) / 2, y, INK);
        }
    }

    private void drawChoiceButton(int[] r, List<String> lines, Opt opt, boolean hot, boolean left) {
        if (r[2] < 20) return;
        int bg = hot ? BTN : PANEL;
        int ink = hot ? BTN_INK : INK;
        drawRect(r[0] + 1, r[1], r[0] + r[2] - 1, r[1] + r[3], bg);
        drawRect(r[0], r[1] + 1, r[0] + r[2], r[1] + r[3] - 1, bg);
        if (!hot) outline(r, PANEL_LINE);
        int ty = r[1] + (r[3] - lines.size() * 10) / 2 + 1;
        String arrow = left ? "‹" : "›";
        int ay = r[1] + r[3] / 2 - 4;
        if (left) fontRendererObj.drawString(arrow, r[0] + 6, ay, ink);
        else fontRendererObj.drawString(arrow, r[0] + r[2] - 6 - fontRendererObj.getStringWidth(arrow), ay, ink);
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i);
            int w = fontRendererObj.getStringWidth(s);
            int tx = left ? r[0] + 14 : r[0] + r[2] - 14 - w;
            fontRendererObj.drawString(s, tx, ty + i * 10, ink);
        }
        if (opt.risky() && !game.cur.single()) {
            mc.getTextureManager().bindTexture(ICONS);
            GL11.glColor4f(1, 1, 1, 1);
            int dx = left ? r[0] + r[2] - 13 : r[0] + 4;
            blit(dx, r[1] + 4, 9, 9, 0, 48, 9, 9, 64);
        }
    }

    // ───────── карта ─────────
    private void drawCardArea(long now) {
        Card c = game.cur;
        // имя над картой
        String who = fontRendererObj.trimStringToWidth(game.cardTitle(), Math.max(cardW + 40, 160));
        fontRendererObj.drawStringWithShadow(who, (width - fontRendererObj.getStringWidth(who)) / 2, cardY - 13, INK);

        float cx = cardX + cardW / 2f, cy = cardY + cardH / 2f;
        // колода позади
        drawBack(cx + 5, cy + 6, 2.2f);
        drawBack(cx - 4, cy + 3, -1.6f);

        float dx = dragDx, dy = 0, sc = 1;
        if (flySide != 0) {
            float t = Math.min(1, (now - flyStart) / 220f);
            float target = (flySide == 'l' ? -1 : 1) * (width * 0.75f);
            dx = flyFrom + (target - flyFrom) * t * t;
        } else if (!dragging && springStart > 0) {
            float t = Math.min(1, (now - springStart) / 180f);
            dx = springFrom * (1 - ease(t));
            if (t >= 1) { springStart = 0; dragDx = 0; }
        }
        if (dealStart > 0) {
            float t = Math.min(1, (now - dealStart) / 260f);
            float e = ease(t);
            dy = -10 * (1 - e);
            sc = 0.95f + 0.05f * e;
            if (t >= 1) dealStart = 0;
        }
        float rot = Math.max(-30, Math.min(30, dx / 18f));

        GL11.glPushMatrix();
        GL11.glTranslatef(cx + dx, cy + dy, 0);
        GL11.glRotatef(rot, 0, 0, 1);
        GL11.glScalef(sc, sc, 1);
        drawCardFace(c);
        // ярлык выбора при перетаскивании
        if (!c.single() || flySide != 0 || dragging) {
            char side = dx < -18 ? 'l' : dx > 18 ? 'r' : 0;
            if (side != 0 && (dragging || flySide != 0)) {
                Opt o = c.single() ? c.l : (side == 'l' ? c.l : c.r);
                String s = label(o);
                if (!s.isEmpty()) {
                    GL11.glTranslatef(0, 0, 300);
                    int w = Math.min(fontRendererObj.getStringWidth(s), cardW - 24);
                    s = fontRendererObj.trimStringToWidth(s, w);
                    int a = (int) Math.min(255, Math.abs(dx) / 70f * 255);
                    drawRect(-w / 2 - 8, -10, w / 2 + 8, 8, (a << 24) | 0x1B2440);
                    if (a > 40) fontRendererObj.drawString(s, -w / 2, -5, (a << 24) | 0xF8F1DC);
                }
            }
        }
        GL11.glPopMatrix();
    }

    private void drawBack(float cx, float cy, float rot) {
        GL11.glPushMatrix();
        GL11.glTranslatef(cx, cy, 0);
        GL11.glRotatef(rot, 0, 0, 1);
        int x0 = -cardW / 2, y0 = -cardH / 2, x1 = x0 + cardW, y1 = y0 + cardH;
        drawRect(x0 + 1, y0 + 3, x1 + 1, y1 + 3, 0x50000000);
        drawRounded(x0, y0, x1, y1, 0xFFE0B449);
        mc.getTextureManager().bindTexture(BACK);
        GL11.glColor4f(1, 1, 1, 1);
        blit(x0 + 2, y0 + 2, cardW - 4, cardH - 4, 0, 0, (cardW - 4) * 2, (cardH - 4) * 2, 32);
        GL11.glPopMatrix();
    }

    private void drawCardFace(Card c) {
        int x0 = -cardW / 2, y0 = -cardH / 2, x1 = x0 + cardW, y1 = y0 + cardH;
        boolean dark = game.cardDark();
        drawRect(x0 + 2, y0 + 4, x1 + 2, y1 + 5, 0x60000000);
        drawRounded(x0, y0, x1, y1, dark ? NIGHT_BOT : PARCH_BOT);
        drawGradientRect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, dark ? NIGHT_TOP : PARCH_TOP, dark ? NIGHT_BOT : PARCH_BOT);
        outline(new int[]{x0 + 5, y0 + 5, cardW - 10, cardH - 10}, dark ? NIGHT_FRAME : FRAME);

        if (game.cardStory()) {
            mc.getTextureManager().bindTexture(ICONS);
            GL11.glColor4f(1, 1, 1, 1);
            blit(x1 - 16, y0 + 8, 9, 9, 16, 48, 9, 9, 64);
        }

        // текст и портрет делят высоту карты
        int ink = dark ? NIGHT_INK : CARD_INK;
        int innerW = cardW - 26;
        int avail = cardH - 30;
        float ts = 1f;
        List<String> lines = wrapText(game.cardText(), innerW);
        int shield = game.cardShield();
        int portrait = Math.round(Math.min(cardW * 0.42f, avail - lines.size() * 10 - 8));
        if (portrait < 30) {
            ts = 0.8f;
            lines = wrapText(game.cardText(), Math.round(innerW / ts));
            portrait = Math.round(Math.min(cardW * 0.42f, avail - lines.size() * 10 * ts - 8));
        }
        portrait = Math.max(22, portrait);

        GL11.glPushMatrix();
        GL11.glTranslatef(0, y0 + 15, 0);
        GL11.glScalef(ts, ts, 1);
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i);
            fontRendererObj.drawString(s, -fontRendererObj.getStringWidth(s) / 2, i * 10, ink);
        }
        GL11.glPopMatrix();

        int py = y1 - 12 - portrait;
        if (shield != 0) {
            mc.getTextureManager().bindTexture(ICONS);
            color(shield);
            blit(-portrait / 2f, py, portrait, portrait, 0, 16, 32, 32, 64);
            GL11.glColor4f(1, 1, 1, 1);
            blit(-portrait / 2f, py, portrait, portrait, 32, 16, 32, 32, 64);
            float isz = portrait * 0.5f;
            drawItem(game.cardItem(), -isz / 2, py + portrait * 0.2f, isz / 16f);
        } else {
            float isz = portrait * 0.75f;
            drawItem(game.cardItem(), -isz / 2, py + (portrait - isz) / 2, isz / 16f);
        }
    }

    private void drawRounded(int x0, int y0, int x1, int y1, int col) {
        drawRect(x0 + 2, y0, x1 - 2, y1, col);
        drawRect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, col);
        drawRect(x0, y0 + 2, x1, y1 - 2, col);
    }

    private void drawPanel(int x0, int y0, int x1, int y1) {
        drawRect(x0, y0, x1, y1, PANEL);
        outline(new int[]{x0, y0, x1 - x0, y1 - y0}, PANEL_LINE);
    }

    private void outline(int[] r, int col) {
        int x0 = r[0], y0 = r[1], x1 = r[0] + r[2], y1 = r[1] + r[3];
        drawRect(x0 + 1, y0, x1 - 1, y0 + 1, col);
        drawRect(x0 + 1, y1 - 1, x1 - 1, y1, col);
        drawRect(x0, y0 + 1, x0 + 1, y1 - 1, col);
        drawRect(x1 - 1, y0 + 1, x1, y1 - 1, col);
    }

    // ───────── ввод ─────────
    @Override
    protected void keyTyped(char ch, int key) {
        if (key == Keyboard.KEY_LEFT || key == Keyboard.KEY_A) { startFly('l', dragDx); return; }
        if (key == Keyboard.KEY_RIGHT || key == Keyboard.KEY_D) { startFly('r', dragDx); return; }
        if (key == Keyboard.KEY_L) { mc.displayGuiScreen(new GuiChronicle(this)); return; }
        if (ClientProxy.openKey != null && key == ClientProxy.openKey.getKeyCode()) { mc.displayGuiScreen(null); return; }
        super.keyTyped(ch, key);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        super.mouseClicked(mx, my, button);
        if (button != 0) return;
        if (in(rLog, mx, my)) { click(); mc.displayGuiScreen(new GuiChronicle(this)); return; }
        if (in(rL, mx, my)) { startFly('l', dragDx); return; }
        if (in(rR, mx, my)) { startFly('r', dragDx); return; }
        if (flySide == 0 && mx >= cardX && mx <= cardX + cardW && my >= cardY && my <= cardY + cardH) {
            dragging = true;
            dragStartX = mx;
            dragDx = 0;
            springStart = 0;
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) {
        super.mouseClickMove(mx, my, button, time);
        if (dragging) dragDx = mx - dragStartX;
    }

    @Override
    protected void mouseMovedOrUp(int mx, int my, int which) {
        super.mouseMovedOrUp(mx, my, which);
        if (which != 0 || !dragging) return;
        dragging = false;
        float th = Math.min(50, cardW * 0.3f);
        if (Math.abs(dragDx) > th) startFly(dragDx < 0 ? 'l' : 'r', dragDx);
        else if (dragDx != 0) { springFrom = dragDx; springStart = System.currentTimeMillis(); }
    }

    private void startFly(char side, float from) {
        if (flySide != 0 || game.cur == null) return;
        dragging = false;
        springStart = 0;
        flySide = side;
        flyFrom = from;
        flyStart = System.currentTimeMillis();
        click();
    }

    private void stepAnimations(long now) {
        if (flySide != 0 && now - flyStart >= 220) {
            char side = flySide;
            int[] before = game.st.clone();
            boolean wasEnded = game.ended;
            game.choose(side);
            for (int i = 0; i < 4; i++) {
                int d = game.st[i] - before[i];
                if (d != 0 && !wasEnded) { flashAt[i] = now; flashDir[i] = d; }
            }
            if (wasEnded) for (int i = 0; i < 4; i++) disp[i] = clamp(game.st[i]);
            if (!game.unlocked.isEmpty()) sound("random.levelup", 1.0f);
            if (game.ended && !wasEnded) sound("random.anvil_land", 0.6f);
            flySide = 0;
            dragDx = 0;
            dealStart = now;
            layoutButtons();
        }
        for (int i = 0; i < 4; i++) {
            float target = clamp(game.st[i]);
            disp[i] += (target - disp[i]) * 0.18f;
            if (Math.abs(target - disp[i]) < 0.05f) disp[i] = target;
        }
    }

    // ───────── помощники ─────────
    private void click() { sound("gui.button.press", 1.0f); }

    private void sound(String name, float pitch) {
        mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation(name), pitch));
    }

    void drawItem(String id, float x, float y, float scale) {
        ItemStack st = stackFor(id);
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 50);
        GL11.glScalef(scale, scale, 1);
        GL11.glColor4f(1, 1, 1, 1);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        itemRender.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), st, 0, 0);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    static ItemStack stackFor(String id) {
        ItemStack st = STACKS.get(id);
        if (st != null) return st;
        String name = id;
        int meta = 0;
        int at = id.indexOf('@');
        if (at > 0) { name = id.substring(0, at); meta = Integer.parseInt(id.substring(at + 1)); }
        Object o = Item.itemRegistry.getObject(name);
        Item item = o instanceof Item ? (Item) o : Items.paper;
        st = new ItemStack(item, 1, meta);
        STACKS.put(id, st);
        return st;
    }

    void blit(float x, float y, float w, float h, float u, float v, float uw, float vh, float tex) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(x, y + h, zLevel, u / tex, (v + vh) / tex);
        t.addVertexWithUV(x + w, y + h, zLevel, (u + uw) / tex, (v + vh) / tex);
        t.addVertexWithUV(x + w, y, zLevel, (u + uw) / tex, v / tex);
        t.addVertexWithUV(x, y, zLevel, u / tex, v / tex);
        t.draw();
    }

    static void color(int argb) {
        GL11.glColor4f((argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f, (argb & 255) / 255f, 1f);
    }

    List<String> wrap(String s, int w) {
        List<String> out = new ArrayList<String>();
        if (s == null || s.isEmpty()) return out;
        for (Object o : fontRendererObj.listFormattedStringToWidth(s, Math.max(20, w))) out.add((String) o);
        return out;
    }

    /** Перенос с учётом пустых строк между абзацами. */
    private List<String> wrapText(String s, int w) {
        List<String> out = new ArrayList<String>();
        for (String par : s.split("\n", -1)) {
            if (par.isEmpty()) out.add("");
            else out.addAll(wrap(par, w));
        }
        return out;
    }

    private static float ease(float t) { return 1 - (1 - t) * (1 - t) * (1 - t); }
    private static float clamp(int v) { return Math.max(0, Math.min(100, v)); }
    private static void set(int[] r, int x, int y, int w, int h) { r[0] = x; r[1] = y; r[2] = w; r[3] = h; }
    static boolean in(int[] r, int x, int y) { return x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3]; }

    static void rect(int x0, int y0, int x1, int y1, int c) { Gui.drawRect(x0, y0, x1, y1, c); }
}
