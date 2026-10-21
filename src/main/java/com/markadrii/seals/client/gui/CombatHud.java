package com.markadrii.seals.client.gui;

import com.markadrii.seals.client.ClientSealData;
import com.markadrii.seals.client.KeyBindings;
import com.markadrii.seals.seal.SealAbility;
import com.markadrii.seals.seal.SealData;
import com.markadrii.seals.seal.SealDefinition;
import com.markadrii.seals.seal.SealTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

/**
 * Hotbar del modo combate: sustituye a la hotbar normal y muestra las habilidades del mob sellado.
 * Cada hueco se activa con su número (1-9).
 */
public final class CombatHud {
    public static final int SLOT = 22;

    public static final IGuiOverlay OVERLAY = CombatHud::render;

    private CombatHud() {}

    private static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientSealData.combatMode() || mc.options.hideGui || mc.player == null) return;
        SealData data = ClientSealData.get();
        SealDefinition def = data.definition();
        if (def == null) return;

        SealTheme theme = def.theme();
        Font font = mc.font;
        List<SealAbility> abilities = def.abilities();
        List<Integer> slots = def.combatSlots(); // solo las que se activan (las pasivas no van aquí)

        int count = slots.size();
        if (count == 0) return;
        int barW = count * SLOT + 4;
        int x0 = width / 2 - barW / 2;
        int y0 = height - SLOT - 4;

        // Marco
        g.fill(x0 - 1, y0 - 1, x0 + barW + 1, y0 + SLOT + 3, theme.border());
        g.fill(x0, y0, x0 + barW, y0 + SLOT + 2, theme.background());
        GuiDraw.rivets(g, x0, y0, barW, SLOT + 2, theme);

        // Título encima
        Component title = Component.translatable("hud.seals.combat", def.name());
        int tw = font.width(title);
        g.fill(width / 2 - tw / 2 - 4, y0 - 13, width / 2 + tw / 2 + 4, y0 - 2, theme.header());
        g.drawString(font, title, width / 2 - tw / 2, y0 - 11, theme.headerText(), false);

        for (int k = 0; k < count; k++) {
            int i = slots.get(k);
            SealAbility ability = abilities.get(i);
            int sx = x0 + 2 + k * SLOT;
            int sy = y0 + 2;
            boolean unlocked = ability.unlocked(data);
            boolean active = def.abilityActive(data, i);
            int cd = def.abilityCooldown(data, i);
            int maxCd = def.abilityMaxCooldown(i);

            // Fondo del hueco
            int border = active && unlocked ? theme.starOn() : theme.border();
            g.fill(sx, sy, sx + SLOT - 2, sy + SLOT - 2, border);
            g.fill(sx + 1, sy + 1, sx + SLOT - 3, sy + SLOT - 3, unlocked ? theme.panelLight() : 0xFF3A3D40);

            // Icono
            GuiDraw.icon(g, ability.icon(), sx + 2, sy + 2, unlocked ? 1.0F : 0.35F);

            // Recarga: oscurece de abajo arriba
            if (unlocked && cd > 0 && maxCd > 0) {
                int h = (int) ((SLOT - 4) * Math.min(1.0F, cd / (float) maxCd));
                g.fill(sx + 1, sy + SLOT - 3 - h, sx + SLOT - 3, sy + SLOT - 3, 0xAA000000);
                String sec = String.valueOf((cd + 19) / 20);
                g.drawString(font, sec, sx + (SLOT - 2) / 2 - font.width(sec) / 2, sy + 6, 0xFFFFFFFF, true);
            }

            // Candado
            if (!unlocked) {
                GuiDraw.lock(g, sx + SLOT - 10, sy + SLOT - 11);
            }

            // Número de tecla
            String key = String.valueOf(k + 1);
            g.drawString(font, key, sx + 2, sy + 1, 0xFFFFFFFF, true);
        }

        // Tiempo restante de la forma de golem
        if (data.golemForm()) {
            Component t = Component.translatable("hud.seals.form_time", (data.golemFormTicks() + 19) / 20);
            g.drawString(font, t, width / 2 - font.width(t) / 2, y0 - 24, theme.starOn(), true);
        }

        // Ayuda para salir
        Component hint = Component.translatable("hud.seals.exit", KeyBindings.COMBAT_MODE.getTranslatedKeyMessage());
        g.drawString(font, hint, x0 + barW + 6, y0 + SLOT / 2 - 3, 0xFFBFBFBF, true);
    }
}
