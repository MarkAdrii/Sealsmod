package com.markadrii.seals.client.anim;

import com.markadrii.seals.Seals;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Animaciones del jugador con Player Animator.
 * Los archivos están en assets/seals/player_animation/*.json (formato Emotecraft).
 */
public final class SealAnimations {
    private static final ResourceLocation LAYER = Seals.id("ability");

    private SealAnimations() {}

    /** Se llama una vez al arrancar el cliente. */
    public static void init() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 42, player -> new ModifierLayer<>());
    }

    @SuppressWarnings("unchecked")
    public static void play(AbstractClientPlayer player, String name) {
        IAnimation raw = PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER);
        if (!(raw instanceof ModifierLayer<?> layerRaw)) return;
        ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>) layerRaw;

        KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(Seals.id(name));
        if (animation == null) {
            Seals.LOGGER.warn("Animación no encontrada: {}", name);
            return;
        }
        layer.setAnimation(new KeyframeAnimationPlayer(animation));
    }
}
