package com.markadrii.seals.client.gui;

import com.markadrii.seals.bond.BondManager;
import com.markadrii.seals.client.ClientSealData;
import com.markadrii.seals.network.C2SSealActionPacket;
import com.markadrii.seals.network.ModNetwork;
import com.markadrii.seals.client.KeyBindings;
import com.markadrii.seals.seal.AgeStage;
import com.markadrii.seals.seal.SealAbility;
import com.markadrii.seals.seal.SealData;
import com.markadrii.seals.seal.SealDefinition;
import com.markadrii.seals.seal.SealTheme;
import com.markadrii.seals.seal.SealVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * Interfaz del vínculo (tecla U).
 * <ul>
 *   <li>Izquierda: criatura sellada, estrellas, barras de afinidad / estabilidad / edad y,
 *       abajo, el texto que explica lo que tengas bajo el ratón.</li>
 *   <li>Centro: guía de habilidades y detalle de la seleccionada.</li>
 *   <li>Derecha: vista 3D del jugador (arrastra para girar).</li>
 * </ul>
 * Los colores y glifos salen del {@link SealTheme} del mob sellado.
 */
public class SealScreen extends Screen {
    private static final int HEADER = 16;
    private static final int FOOTER = 24;
    private static final int GAP = 6;
    private static final int ROW = 18;

    private int x0, y0, w, h;
    private int lx, lw, mx, mw, rx, rw, top, bottom;

    private int selected;
    private float rotation = 20.0F;
    private boolean dragging;

    // Qué explica el panel de abajo a la izquierda
    private enum Info { BOND, STARS, AFFINITY, STABILITY, AGE }
    private Info info = Info.BOND;

    public SealScreen() {
        super(Component.translatable("screen.seals.title"));
    }

    @Override
    protected void init() {
        ClientSealData.setLastLine(null);
        w = Math.min(width - 12, 500);
        h = Math.min(height - 12, 280);
        x0 = (width - w) / 2;
        y0 = (height - h) / 2;

        int inner = w - 12;
        lw = (int) (inner * 0.34F);
        mw = (int) (inner * 0.36F);
        rw = inner - lw - mw - GAP * 2;
        lx = x0 + 6;
        mx = lx + lw + GAP;
        rx = mx + mw + GAP;
        top = y0 + HEADER + 6;
        bottom = y0 + h - FOOTER;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------------------------------
    // Dibujo
    // ---------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        SealData data = ClientSealData.get();
        SealDefinition def = data.definition();
        if (def == null) {
            onClose();
            return;
        }
        SealTheme t = def.theme();

        renderBackground(g);

        // Marco general y glifos de fondo
        g.fill(x0 - 2, y0 - 2, x0 + w + 2, y0 + h + 2, t.border());
        g.fill(x0, y0, x0 + w, y0 + h, t.background());
        drawGlyphWatermark(g, t);

        drawHeader(g, data, def, t);

        info = Info.BOND;
        drawLeft(g, data, def, t, mouseX, mouseY);
        drawMiddle(g, data, def, t, mouseX, mouseY);
        drawRight(g, data, def, t);
        drawInfoBox(g, data, def, t);
        drawFooter(g, t, mouseX, mouseY);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawGlyphWatermark(GuiGraphics g, SealTheme t) {
        int step = 36;
        int i = 0;
        for (int gy = y0 + HEADER + 4; gy < y0 + h - 16; gy += step) {
            for (int gx = x0 + 4 + ((gy / step) % 2) * (step / 2); gx < x0 + w - 16; gx += step) {
                GuiDraw.sprite(g, t.glyphs(), gx, gy, 16, 16, (i % 4) * 16, 0, 64, 16, 0.10F);
                i++;
            }
        }
    }

    private void drawHeader(GuiGraphics g, SealData data, SealDefinition def, SealTheme t) {
        g.fill(x0, y0, x0 + w, y0 + HEADER, t.header());
        g.fill(x0, y0 + HEADER, x0 + w, y0 + HEADER + 1, t.accent());
        GuiDraw.sprite(g, t.glyphs(), x0 + 4, y0, 16, 16, 16, 0, 64, 16, 1.0F);
        Component title = Component.translatable("screen.seals.header", def.name());
        g.drawString(font, title, x0 + 22, y0 + 4, t.headerText(), false);

        // Estrellas a la derecha
        int stars = def.maxStars();
        int sx = x0 + w - 8 - stars * 9;
        for (int i = 0; i < stars; i++) {
            GuiDraw.star(g, sx + i * 9, y0 + 4, i < data.level() - 1 ? t.starOn() : t.starOff());
        }
        Component lvl = Component.translatable("screen.seals.level", data.level(), def.maxLevel());
        g.drawString(font, lvl, sx - 6 - font.width(lvl), y0 + 4, t.headerText(), false);
    }

    private void panel(GuiGraphics g, int x, int y, int pw, int ph, SealTheme t) {
        g.fill(x - 1, y - 1, x + pw + 1, y + ph + 1, t.border());
        g.fill(x, y, x + pw, y + ph, t.panel());
        GuiDraw.rivets(g, x, y, pw, ph, t);
    }

    // --- Columna izquierda ---------------------------------------------------------------

    private int infoTop;

    private void drawLeft(GuiGraphics g, SealData data, SealDefinition def, SealTheme t, int mouseX, int mouseY) {
        panel(g, lx, top, lw, bottom - top, t);
        int x = lx + 6;
        int bw = lw - 12;
        int y = top + 6;

        // Emblema y nombre
        g.fill(x - 1, y - 1, x + 25, y + 25, t.border());
        g.blit(t.emblem(), x, y, 24, 24, 0, 0, 32, 32, 32, 32);
        g.drawString(font, Component.translatable("screen.seals.sealed_creature"), x + 30, y + 2, t.textMuted(), false);
        g.drawString(font, def.name(), x + 30, y + 13, t.text(), false);
        y += 30;

        // Estrellas
        int starsY = y;
        g.drawString(font, Component.translatable("screen.seals.affection"), x, y, t.text(), false);
        int stars = def.maxStars();
        for (int i = 0; i < stars; i++) {
            GuiDraw.star(g, x + bw - stars * 9 + i * 9, y, i < data.level() - 1 ? t.starOn() : t.starOff());
        }
        y += 12;
        if (hover(mouseX, mouseY, x, starsY - 1, bw, 10)) info = Info.STARS;

        // Afinidad
        boolean maxed = data.level() >= def.maxLevel() && data.affinity() >= 100;
        String aff = maxed ? Component.translatable("screen.seals.max").getString() : data.affinity() + " / 100";
        y = labeledBar(g, x, y, bw, Component.translatable("screen.seals.affinity"), aff,
                data.affinity() / 100F, t.affinityBar(), t);
        if (hover(mouseX, mouseY, x, y - 20, bw, 19)) info = Info.AFFINITY;

        // Estabilidad
        int stabColor = data.stability() <= 30 ? 0xFFB02020 : t.stabilityBar();
        y = labeledBar(g, x, y, bw, Component.translatable("screen.seals.stability"), data.stability() + " / 100",
                data.stability() / 100F, stabColor, t);
        if (hover(mouseX, mouseY, x, y - 20, bw, 19)) info = Info.STABILITY;

        // Edad y etapa (Joven, Adulto, Anciano...)
        AgeStage stage = data.ageStage();
        Component ageLabel = Component.translatable("screen.seals.age").append(" · ").append(stage.displayName());
        String years = Component.translatable("screen.seals.age_years", data.ageYears()).getString();
        g.drawString(font, ageLabel, x, y, t.text(), false);
        g.drawString(font, years, x + bw - font.width(years), y, t.text(), false);
        drawStageBar(g, x, y + 10, bw, data, t);
        y += 20;
        if (hover(mouseX, mouseY, x, y - 20, bw, 19)) info = Info.AGE;

        // Estado
        g.drawString(font, Component.translatable("screen.seals.phase", phase(data, def)), x, y, t.textMuted(), false);
        y += 12;

        infoTop = y;
    }

    private int labeledBar(GuiGraphics g, int x, int y, int bw, Component label, String value, float progress,
                           int color, SealTheme t) {
        g.drawString(font, label, x, y, t.text(), false);
        g.drawString(font, value, x + bw - font.width(value), y, t.text(), false);
        GuiDraw.bar(g, x, y + 10, bw, 6, progress, color, t);
        return y + 20;
    }

    /** Etapas visibles: las 6 normales y, solo si eres el Oldest One, la oculta. */
    private static List<AgeStage> visibleStages(SealData data) {
        List<AgeStage> list = new ArrayList<>();
        for (AgeStage s : AgeStage.values()) {
            if (!s.hidden() || data.oldestOne()) list.add(s);
        }
        return list;
    }

    /** Barra dividida en etapas: las pasadas llenas, la actual según su progreso. */
    private void drawStageBar(GuiGraphics g, int x, int y, int bw, SealData data, SealTheme t) {
        List<AgeStage> stages = visibleStages(data);
        AgeStage current = data.ageStage();
        int currentIndex = stages.indexOf(current);
        int n = stages.size();
        int gap = 1;
        int segW = (bw - gap * (n - 1)) / n;
        for (int i = 0; i < n; i++) {
            AgeStage s = stages.get(i);
            int sx = x + i * (segW + gap);
            int w = i == n - 1 ? bw - (sx - x) : segW;
            float progress = i < currentIndex ? 1F : i == currentIndex ? s.progress(data.ageMonths()) : 0F;
            GuiDraw.bar(g, sx, y, w, 6, progress, s.color(), t);
        }
    }

    private Component phase(SealData data, SealDefinition def) {
        List<Component> active = new ArrayList<>();
        for (int i = 0; i < def.abilities().size(); i++) {
            SealAbility a = def.abilities().get(i);
            if (a.kind() != SealAbility.Kind.PASSIVE && def.abilityActive(data, i)) active.add(a.name());
        }
        if (data.boostTicks() > 0) {
            active.add(0, Component.translatable("screen.seals.phase.boost", (data.boostTicks() + 1199) / 1200));
        }
        if (active.isEmpty()) return Component.translatable("screen.seals.phase.rest");
        Component out = active.get(0).copy();
        for (int i = 1; i < active.size(); i++) out = out.copy().append(" + ").append(active.get(i));
        return out;
    }

    /** Caja de explicación abajo a la izquierda. */
    private void drawInfoBox(GuiGraphics g, SealData data, SealDefinition def, SealTheme t) {
        int x = lx + 4;
        int y = infoTop;
        int bw = lw - 8;
        int bh = bottom - 4 - y;
        if (bh < 24) return;
        g.fill(x, y, x + bw, y + bh, t.panelLight());
        GuiDraw.outline(g, x, y, bw, bh, t.border());

        Component title;
        List<Component> paragraphs = new ArrayList<>();
        List<Component> tight = new ArrayList<>(); // líneas sin espacio extra debajo
        String k = def.textKey();
        switch (info) {
            case STARS -> {
                title = Component.translatable("info.seals.stars.title");
                paragraphs.add(Component.translatable("info.seals.stars", def.maxStars(), def.maxLevel()));
                for (SealAbility a : def.abilities()) {
                    int need = a.unlockLevel() - 1;
                    paragraphs.add(need == 0
                            ? Component.translatable("info.seals.stars.start", a.name())
                            : Component.translatable("info.seals.stars.unlock", need, a.name()));
                }
            }
            case AFFINITY -> {
                title = Component.translatable("info.seals.affinity.title");
                paragraphs.add(Component.translatable("info.seals.affinity"));
                paragraphs.add(Component.translatable(k + ".affinity"));
            }
            case STABILITY -> {
                title = Component.translatable("info.seals.stability.title");
                paragraphs.add(Component.translatable("info.seals.stability"));
                paragraphs.add(Component.translatable(k + ".stability"));
            }
            case AGE -> {
                title = Component.translatable("info.seals.age.title");
                int bonus = Math.round((data.ageScale() - 1F) * 100F);
                paragraphs.add(Component.translatable("info.seals.age.months", data.ageYears(), data.ageMonths() % 12,
                        bonus));
                AgeStage current = data.ageStage();
                for (AgeStage s : visibleStages(data)) {
                    Component line = Component.translatable("info.seals.age.stage", s.displayName(), s.minYears(), s.maxYears());
                    if (s == current) line = line.copy().append(Component.translatable("info.seals.age.current"));
                    paragraphs.add(line);
                    tight.add(line);
                }
                paragraphs.add(Component.translatable("info.seals.age", def.minAge(), def.maxAge()));
            }
            default -> {
                Component said = ClientSealData.lastLine();
                if (said != null) {
                    title = Component.translatable("info.seals.said", def.name());
                    paragraphs.add(Component.literal("«").append(said).append("»"));
                    paragraphs.add(Component.translatable("info.seals.talk.hint"));
                    break;
                }
                title = Component.translatable("info.seals.bond.title");
                paragraphs.add(Component.translatable(k + ".bond"));
                paragraphs.add(Component.translatable("info.seals.bond.hint"));
            }
        }

        int ty = y + 4;
        g.drawString(font, title, x + 4, ty, t.accent(), false);
        ty += 11;
        int maxY = y + bh - 9;
        for (Component p : paragraphs) {
            for (FormattedCharSequence line : font.split(p, bw - 8)) {
                if (ty > maxY) return;
                g.drawString(font, line, x + 4, ty, t.text(), false);
                ty += 9;
            }
            if (!tight.contains(p)) ty += 3;
        }
    }

    // --- Columna central ---------------------------------------------------------------

    private void drawMiddle(GuiGraphics g, SealData data, SealDefinition def, SealTheme t, int mouseX, int mouseY) {
        panel(g, mx, top, mw, bottom - top, t);
        int x = mx + 6;
        int bw = mw - 12;
        int y = top + 6;

        List<SealAbility> abilities = def.abilities();
        long unlocked = abilities.stream().filter(a -> a.unlocked(data)).count();
        g.drawString(font, Component.translatable("screen.seals.abilities"), x, y, t.accent(), false);
        y += 10;
        g.drawString(font, Component.translatable("screen.seals.abilities.count", unlocked, abilities.size()), x, y, t.textMuted(), false);
        y += 13;

        for (int i = 0; i < abilities.size(); i++) {
            SealAbility a = abilities.get(i);
            boolean isUnlocked = a.unlocked(data);
            boolean sel = i == selected;
            boolean hov = hover(mouseX, mouseY, x, y, bw, ROW - 2);
            int bg = sel ? t.header() : hov ? 0xFFE6E1D4 : t.panelLight();
            g.fill(x, y, x + bw, y + ROW - 2, bg);
            GuiDraw.outline(g, x, y, bw, ROW - 2, t.border());
            GuiDraw.icon(g, a.icon(), x + 1, y, isUnlocked ? 1.0F : 0.35F);
            int textColor = sel ? t.headerText() : isUnlocked ? t.text() : t.textMuted();
            g.drawString(font, a.name(), x + 21, y + 4, textColor, false);

            if (!isUnlocked) {
                int need = a.unlockLevel() - 1;
                for (int s = 0; s < need; s++) GuiDraw.star(g, x + bw - 4 - (need - s) * 8, y + 4, t.starOff());
            } else if (def.abilityActive(data, i)) {
                g.fill(x + bw - 9, y + 5, x + bw - 4, y + 10, 0xFF62C450);
            }
            y += ROW;
        }

        // Detalle de la habilidad seleccionada
        y += 2;
        int dh = bottom - 4 - y;
        if (dh < 30 || selected >= abilities.size()) return;
        SealAbility a = abilities.get(selected);
        g.fill(x, y, x + bw, y + dh, t.panelLight());
        GuiDraw.outline(g, x, y, bw, dh, t.border());

        int ty = y + 4;
        int maxY = y + dh - 9;
        g.drawString(font, a.name(), x + 4, ty, t.accent(), false);
        Component kind = a.kindName();
        g.drawString(font, kind, x + bw - 4 - font.width(kind), ty, t.textMuted(), false);
        ty += 11;

        Component status;
        if (a.unlocked(data)) {
            status = Component.translatable("screen.seals.ability.unlocked", selected + 1);
        } else {
            status = Component.translatable("screen.seals.ability.locked", a.unlockLevel() - 1);
        }
        for (FormattedCharSequence line : font.split(status, bw - 8)) {
            g.drawString(font, line, x + 4, ty, a.unlocked(data) ? 0xFF3C7A2E : 0xFF9A3B2B, false);
            ty += 9;
        }
        ty += 3;

        for (FormattedCharSequence line : font.split(a.description(), bw - 8)) {
            if (ty > maxY) return;
            g.drawString(font, line, x + 4, ty, t.text(), false);
            ty += 9;
        }
        ty += 3;
        for (Component stat : def.abilityStats(data, selected)) {
            if (ty > maxY) return;
            g.drawString(font, Component.literal("• ").append(stat), x + 4, ty, t.textMuted(), false);
            ty += 9;
        }
    }

    // --- Columna derecha ---------------------------------------------------------------

    private void drawRight(GuiGraphics g, SealData data, SealDefinition def, SealTheme t) {
        panel(g, rx, top, rw, bottom - top, t);
        int x = rx + 6;
        int bw = rw - 12;
        g.drawString(font, Component.translatable("screen.seals.view"), x, top + 6, t.textMuted(), false);

        int boxY = top + 18;
        int boxH = bottom - 6 - boxY - 22;
        g.fill(x, boxY, x + bw, boxY + boxH, 0x33000000);
        GuiDraw.outline(g, x, boxY, bw, boxH, t.border());

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && boxH > 30) {
            float form = SealVisuals.golemForm(mc.player) ? 1.5F : 1.0F;
            int scale = (int) Math.min(bw / 1.4F, boxH / 2.4F / form);
            g.enableScissor(x + 1, boxY + 1, x + bw - 1, boxY + boxH - 1);
            renderPlayer(g, x + bw / 2, boxY + boxH - 8, scale, rotation, mc.player);
            g.disableScissor();
        }

        // Pista
        int ty = boxY + boxH + 4;
        Component hint = Component.translatable("screen.seals.combat_hint", KeyBindings.COMBAT_MODE.getTranslatedKeyMessage());
        for (FormattedCharSequence line : font.split(hint, bw)) {
            if (ty > bottom - 10) break;
            g.drawString(font, line, x, ty, t.textMuted(), false);
            ty += 9;
        }
    }

    private static void renderPlayer(GuiGraphics g, int cx, int cy, int scale, float rot, LivingEntity entity) {
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX((float) Math.toRadians(-8));
        pose.mul(camera);

        float bodyRot = entity.yBodyRot;
        float yRot = entity.getYRot();
        float xRot = entity.getXRot();
        float headRotO = entity.yHeadRotO;
        float headRot = entity.yHeadRot;

        entity.yBodyRot = 180.0F + rot;
        entity.setYRot(180.0F + rot);
        entity.setXRot(0);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();

        InventoryScreen.renderEntityInInventory(g, cx, cy, scale, pose, camera, entity);

        entity.yBodyRot = bodyRot;
        entity.setYRot(yRot);
        entity.setXRot(xRot);
        entity.yHeadRotO = headRotO;
        entity.yHeadRot = headRot;
    }

    // --- Pie ---------------------------------------------------------------------------

    /** Botón del pie de la ventana. */
    private record Btn(String id, Component label, boolean enabled, Component tip, int x, int y, int w, int h) {
        int[] rect() { return new int[]{x, y, w, h}; }
    }

    private int[] closeBtn() {
        Component label = Component.translatable("screen.seals.button.close");
        int bw = Math.max(70, font.width(label) + 16);
        return new int[]{x0 + w - 6 - bw, y0 + h - FOOTER + 4, bw, 16};
    }

    /** Hablar (siempre), Entrar (1 estrella) e Invocar (2 estrellas, si el mob tiene mini). */
    private List<Btn> footerButtons() {
        SealData data = ClientSealData.get();
        SealDefinition def = data.definition();
        List<Btn> list = new ArrayList<>();
        if (def == null) return list;
        long today = data.today();
        int bx = x0 + 6;
        int by = y0 + h - FOOTER + 4;

        // Hablar
        long talkLeft = BondManager.daysLeft(today, data.lastTalkDay(), BondManager.TALK_DAYS);
        Component talk = Component.translatable("screen.seals.button.talk");
        int tw = font.width(talk) + 16;
        list.add(new Btn("talk", talk, talkLeft == 0, Component.translatable("screen.seals.tip.talk"), bx, by, tw, 16));
        bx += tw + 6;

        // Entrar
        if (data.level() >= BondManager.ENTER_LEVEL) {
            long enterLeft = BondManager.daysLeft(today, data.lastEnterDay(), BondManager.ENTER_DAYS);
            Component enter = Component.translatable("screen.seals.button.enter");
            int ew = font.width(enter) + 16;
            Component tip = data.inInner() ? Component.translatable("screen.seals.tip.inside")
                    : Component.translatable("screen.seals.tip.days", enterLeft);
            list.add(new Btn("enter", enter, enterLeft == 0 && !data.inInner(), tip, bx, by, ew, 16));
            bx += ew + 6;
        }

        // Invocar / Desinvocar
        if (def.miniUnlockLevel() > 0 && data.level() >= def.miniUnlockLevel()) {
            Component mini = Component.translatable(data.miniOut() ? "screen.seals.button.unsummon" : "screen.seals.button.summon");
            int mw2 = font.width(mini) + 16;
            boolean ok = data.miniOut() || !data.inInner();
            list.add(new Btn("mini", mini, ok, Component.translatable("screen.seals.tip.inside"), bx, by, mw2, 16));
        }
        return list;
    }

    private void drawFooter(GuiGraphics g, SealTheme t, int mouseX, int mouseY) {
        Btn hovered = null;
        for (Btn b : footerButtons()) {
            if (b.enabled()) {
                button(g, b.rect(), b.label(), t, mouseX, mouseY);
            } else {
                g.fill(b.x() - 1, b.y() - 1, b.x() + b.w() + 1, b.y() + b.h() + 1, t.border());
                g.fill(b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), 0xFF55585B);
                g.drawString(font, b.label(), b.x() + b.w() / 2 - font.width(b.label()) / 2, b.y() + 4, 0xFF9A9A9A, false);
                if (hover(mouseX, mouseY, b.x(), b.y(), b.w(), b.h())) hovered = b;
            }
        }
        button(g, closeBtn(), Component.translatable("screen.seals.button.close"), t, mouseX, mouseY);
        if (hovered != null) g.renderTooltip(font, hovered.tip(), mouseX, mouseY);
    }

    private void button(GuiGraphics g, int[] r, Component label, SealTheme t, int mouseX, int mouseY) {
        boolean hov = hover(mouseX, mouseY, r[0], r[1], r[2], r[3]);
        g.fill(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1, t.border());
        g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], hov ? t.accent() : t.panelLight());
        g.drawString(font, label, r[0] + r[2] / 2 - font.width(label) / 2, r[1] + 4, hov ? 0xFFFFFFFF : t.text(), false);
    }

    // ---------------------------------------------------------------------------------------
    // Entrada
    // ---------------------------------------------------------------------------------------

    private static boolean hover(double mouseX, double mouseY, int x, int y, int bw, int bh) {
        return mouseX >= x && mouseX < x + bw && mouseY >= y && mouseY < y + bh;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        SealDefinition def = ClientSealData.get().definition();
        if (def != null && button == 0) {
            // Lista de habilidades
            int x = mx + 6;
            int y = top + 6 + 23;
            for (int i = 0; i < def.abilities().size(); i++) {
                if (hover(mouseX, mouseY, x, y + i * ROW, mw - 12, ROW - 2)) {
                    selected = i;
                    click();
                    return true;
                }
            }
            int[] c = closeBtn();
            if (hover(mouseX, mouseY, c[0], c[1], c[2], c[3])) {
                click();
                onClose();
                return true;
            }
            for (Btn b : footerButtons()) {
                if (!b.enabled() || !hover(mouseX, mouseY, b.x(), b.y(), b.w(), b.h())) continue;
                click();
                switch (b.id()) {
                    case "talk" -> ModNetwork.CHANNEL.sendToServer(new C2SSealActionPacket(C2SSealActionPacket.Action.TALK));
                    case "enter" -> {
                        ModNetwork.CHANNEL.sendToServer(new C2SSealActionPacket(C2SSealActionPacket.Action.ENTER));
                        onClose();
                    }
                    case "mini" -> {
                        boolean summoning = !ClientSealData.get().miniOut();
                        ModNetwork.CHANNEL.sendToServer(new C2SSealActionPacket(C2SSealActionPacket.Action.TOGGLE_MINI));
                        if (summoning) onClose(); // se cierra para ver la animación
                    }
                    default -> {}
                }
                return true;
            }
            // Arrastrar el modelo 3D
            if (hover(mouseX, mouseY, rx, top, rw, bottom - top)) {
                dragging = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            rotation += (float) dragX * 2.0F;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.OPEN_SEAL.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
