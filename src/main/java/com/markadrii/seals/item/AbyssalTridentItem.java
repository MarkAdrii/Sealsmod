package com.markadrii.seals.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.markadrii.seals.entity.AbyssalTridentEntity;
import com.markadrii.seals.registry.ModItems;
import com.markadrii.seals.seal.SealCapability;
import com.markadrii.seals.seal.SealData;
import com.markadrii.seals.seal.elder.ElderGuardianSeal;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Tridente abisal (4 estrellas del Elder Guardian). Hace mucho daño y tiene lealtad y propulsión
 * siempre, incluso fuera del agua:
 * <ul>
 *   <li>Mantener clic derecho y soltar: propulsión (riptide) en la dirección a la que miras.</li>
 *   <li>Con shift: lo lanzas y vuelve a tu mano (lealtad).</li>
 * </ul>
 * Solo existe mientras llevas al Elder Guardian sellado con 4 estrellas.
 */
public class AbyssalTridentItem extends TridentItem {
    public static final float MELEE_DAMAGE = 13.0F;
    public static final float THROWN_BONUS = 7.0F;
    private static final int RIPTIDE = 3;

    private final Multimap<Attribute, AttributeModifier> modifiers;

    public AbyssalTridentItem(Properties properties) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier",
                MELEE_DAMAGE - 1.0, AttributeModifier.Operation.ADDITION));
        b.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier",
                -2.8, AttributeModifier.Operation.ADDITION));
        this.modifiers = b.build();
    }

    /** Crea un tridente para este jugador (con lealtad incluida). */
    public static ItemStack create(Player owner) {
        ItemStack stack = new ItemStack(ModItems.ABYSSAL_TRIDENT.get());
        stack.enchant(Enchantments.LOYALTY, 3);
        stack.getOrCreateTag().putUUID("SealsOwner", owner.getUUID());
        stack.getOrCreateTag().putBoolean("Unbreakable", true);
        return stack;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? modifiers : ImmutableMultimap.of();
    }

    /** A diferencia del tridente normal, siempre se puede cargar (también fuera del agua). */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        int used = getUseDuration(stack) - timeLeft;
        if (used < 10) return;
        player.awardStat(Stats.ITEM_USED.get(this));

        if (player.isShiftKeyDown()) {
            // Lanzarlo: vuelve solo
            if (!level.isClientSide) {
                AbyssalTridentEntity thrown = new AbyssalTridentEntity(level, player, stack);
                thrown.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 1.0F);
                if (player.getAbilities().instabuild) thrown.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                level.addFreshEntity(thrown);
                level.playSound(null, thrown, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 0.8F);
                if (!player.getAbilities().instabuild) player.getInventory().removeItem(stack);
            }
            return;
        }

        // Propulsión (riptide), funciona en cualquier sitio
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        float x = -Mth.sin(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD);
        float y = -Mth.sin(pitch * Mth.DEG_TO_RAD);
        float z = Mth.cos(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD);
        float len = Mth.sqrt(x * x + y * y + z * z);
        float power = 3.0F * ((1.0F + RIPTIDE) / 4.0F);
        player.push(x * power / len, y * power / len, z * power / len);
        player.startAutoSpinAttack(20);
        if (player.onGround()) player.move(MoverType.SELF, new Vec3(0.0, 1.2, 0.0));
        level.playSound(null, player, SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** Si ya no llevas al Elder Guardian con 4 estrellas, el tridente se deshace. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        SealData data = SealCapability.get(player);
        boolean allowed = data.definition() instanceof ElderGuardianSeal && data.level() >= 5;
        if (!allowed && !player.getAbilities().instabuild) {
            stack.setCount(0);
            player.displayClientMessage(Component.translatable("message.seals.elder.trident_gone")
                    .withStyle(ChatFormatting.DARK_AQUA), true);
            return;
        }
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LOYALTY, stack) < 3) {
            stack.enchant(Enchantments.LOYALTY, 3);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.seals.abyssal_trident.tooltip").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("item.seals.abyssal_trident.tooltip2").withStyle(ChatFormatting.GRAY));
    }
}
