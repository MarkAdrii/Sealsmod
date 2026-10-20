package com.markadrii.seals.client.gui;

import com.markadrii.seals.bond.DialogOption;
import com.markadrii.seals.client.ClientSealData;
import com.markadrii.seals.network.C2SDialogOptionPacket;
import com.markadrii.seals.network.ModNetwork;
import com.markadrii.seals.network.S2CDialogPacket;
import com.markadrii.seals.seal.SealDefinition;
import com.markadrii.seals.seal.SealTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Conversación con el mob en el mundo interior. Aparece abajo, como un cuadro de diálogo,
 * para que se siga viendo al mob detrás. El texto aparece letra a letra.
 */
public class InnerDialogScreen extends Screen {
    private static final int OPTION_H = 16;

    private Component speaker;
    private Component line;
    private List<DialogOption> options = new ArrayList<>();
    private int ticks;

    public InnerDialogScreen(Component speaker, Component line, List<Integer> options) {
        super(Component.translatable("screen.seals.dialog"));
        update(speaker, line, options);
    }

    private void update(Component speaker, Component line, List<Integer> ids) {
        this.speaker = speaker;
        this.line = line;
        this.options = new ArrayList<>();
        for (int id : ids) {
            DialogOption o = DialogOption.byId(id);
            if (o != null) options.add(o);
        }
        this.ticks = 0;
    }

    /** Llega un paquete del servidor: abre, actualiza o cierra el diálogo. */
    public static void receive(S2CDialogPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (msg.close()) {
            if (mc.screen instanceof InnerDialogScreen) mc.setScreen(null);
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("").append(msg.speaker()).append(": ").append(msg.line()), true);
            }
            return;
        }
        if (mc.screen instanceof InnerDialogScreen screen) {
            screen.update(msg.speaker(), msg.line(), msg.options());
        } else {
            mc.setScreen(new InnerDialogScreen(msg.speaker(), msg.line(), msg.options()));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        ticks++;
    }

    private int boxW() { return Math.min(width - 20, 380); }
    private int boxX() { return (width - boxW()) / 2; }

    private int boxH() {
        return 64 + options.size() * (OPTION_H + 2);
    }

    private int boxY() { return height - boxH() - 10; }

    private int optionY(int i) {
        return boxY() + 54 + i * (OPTION_H + 2);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        SealDefinition def = ClientSealData.get().definition();
        SealTheme t = def != null ? def.theme() : null;
        int bg = t != null ? t.background() : 0xFF202020;
        int header = t != null ? t.header() : 0xFF101010;
        int border = t != null ? t.border() : 0xFF555555;
        int accent = t != null ? t.accent() : 0xFFAA8844;

        int x = boxX(), y = boxY(), w = boxW(), h = boxH();
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, border);
        g.fill(x, y, x + w, y + h, (bg & 0x00FFFFFF) | 0xE8000000);
        g.fill(x, y, x + w, y + 14, header);
        g.fill(x, y + 14, x + w, y + 15, accent);
        g.drawString(font, speaker, x + 6, y + 3, 0xFFF2C744, false);

        // Texto letra a letra
        String full = line.getString();
        int shown = Math.min(full.length(), (ticks + 1) * 2 + (int) (partialTick * 2));
        Component visible = Component.literal(full.substring(0, shown));
        int ty = y + 20;
        for (FormattedCharSequence l : font.split(visible, w - 12)) {
            if (ty > y + 48) break;
            g.drawString(font, l, x + 6, ty, 0xFFEDE8DC, false);
            ty += 10;
        }

        // Respuestas
        for (int i = 0; i < options.size(); i++) {
            int oy = optionY(i);
            boolean hov = mouseX >= x + 6 && mouseX < x + w - 6 && mouseY >= oy && mouseY < oy + OPTION_H;
            g.fill(x + 6, oy, x + w - 6, oy + OPTION_H, hov ? accent : 0x40FFFFFF);
            GuiDraw.outline(g, x + 6, oy, w - 12, OPTION_H, border);
            Component label = Component.literal((i + 1) + ". ").append(options.get(i).label());
            g.drawString(font, label, x + 12, oy + 4, hov ? 0xFFFFFFFF : 0xFFE0DACB, false);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = boxX(), w = boxW();
            for (int i = 0; i < options.size(); i++) {
                int oy = optionY(i);
                if (mouseX >= x + 6 && mouseX < x + w - 6 && mouseY >= oy && mouseY < oy + OPTION_H) {
                    choose(options.get(i));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Teclas 1-9 para elegir respuesta
        int n = keyCode - org.lwjgl.glfw.GLFW.GLFW_KEY_1;
        if (n >= 0 && n < options.size() && n < 9) {
            choose(options.get(n));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void choose(DialogOption option) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        if (option == DialogOption.BYE) {
            onClose();
        }
        ModNetwork.CHANNEL.sendToServer(new C2SDialogOptionPacket(option.ordinal()));
    }
}
