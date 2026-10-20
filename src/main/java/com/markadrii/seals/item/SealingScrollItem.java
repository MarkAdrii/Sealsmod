package com.markadrii.seals.item;

import com.markadrii.seals.network.S2CFxPacket;
import com.markadrii.seals.seal.SealCapability;
import com.markadrii.seals.seal.SealData;
import com.markadrii.seals.seal.SealDefinition;
import com.markadrii.seals.seal.SealManager;
import com.markadrii.seals.seal.SealRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pergamino de sello: clic derecho sobre un mob debilitado (25% de vida o menos) para sellarlo dentro de ti.
 */
public class SealingScrollItem extends Item {

    public SealingScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        SealDefinition def = SealRegistry.forEntity(target.getType());
        if (def == null) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer) || !(player.level() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }

        SealData data = SealCapability.get(serverPlayer);
        if (data.hasSeal()) {
            fail(serverPlayer, Component.translatable("message.seals.already_sealed",
                    data.definition() != null ? data.definition().name() : Component.literal("?")));
            return InteractionResult.FAIL;
        }
        if (!def.canSeal(target)) {
            fail(serverPlayer, Component.translatable("message.seals.too_strong", def.name(),
                    Math.round(def.sealHealthThreshold() * 100)));
            return InteractionResult.FAIL;
        }

        // Sellado
        int age = def.minAge() + level.random.nextInt(def.maxAge() - def.minAge() + 1);
        data.seal(def.id(), age, SealManager.currentDay(serverPlayer.server));

        SealManager.fx(level, S2CFxPacket.Type.SEAL, target.getX(), target.getY() + target.getBbHeight() / 2,
                target.getZ(), serverPlayer.getId());
        level.playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.2F, 0.6F);
        level.playSound(null, target.blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.8F);
        target.discard();
        SealManager.screenEffect(serverPlayer, com.markadrii.seals.network.S2CScreenEffectPacket.Effect.SEAL);

        if (!serverPlayer.getAbilities().instabuild) stack.shrink(1);

        serverPlayer.sendSystemMessage(Component.translatable("message.seals.sealed", def.name(), age)
                .withStyle(ChatFormatting.GOLD));
        serverPlayer.sendSystemMessage(Component.translatable("message.seals.sealed_hint")
                .withStyle(ChatFormatting.GRAY));
        SealManager.sync(serverPlayer);
        return InteractionResult.CONSUME;
    }

    private static void fail(ServerPlayer player, Component message) {
        player.displayClientMessage(message.copy().withStyle(ChatFormatting.RED), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.6F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.seals.sealing_scroll.tooltip").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
