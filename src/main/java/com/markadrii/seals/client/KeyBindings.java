package com.markadrii.seals.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Teclas del mod (se pueden cambiar en Opciones → Controles → Seals). */
public final class KeyBindings {
    public static final String CATEGORY = "key.categories.seals";

    /** U: abre la interfaz del vínculo. */
    public static final KeyMapping OPEN_SEAL = new KeyMapping("key.seals.open_seal",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, CATEGORY);

    /** Alt: entra y sale del modo combate. */
    public static final KeyMapping COMBAT_MODE = new KeyMapping("key.seals.combat_mode",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);

    private KeyBindings() {}
}
