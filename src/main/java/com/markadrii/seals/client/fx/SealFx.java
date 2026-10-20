package com.markadrii.seals.client.fx;

import com.markadrii.seals.entity.HolyGolemEntity;
import com.markadrii.seals.network.S2CFxPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.easing.Easing;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.world.type.LodestoneWorldParticleType;

/** Efectos visuales del mod hechos con partículas de Lodestone (solo cliente). */
public final class SealFx {
    // Paleta (r, g, b de 0 a 1)
    private static final float[] IRON = {0.80F, 0.82F, 0.85F};
    private static final float[] IRON_DARK = {0.42F, 0.45F, 0.48F};
    private static final float[] RUST = {0.62F, 0.32F, 0.18F};
    private static final float[] POPPY = {0.85F, 0.18F, 0.15F};
    private static final float[] HOLY = {0.35F, 0.70F, 1.00F};
    private static final float[] HOLY_LIGHT = {0.80F, 0.93F, 1.00F};
    private static final float[] GOLD = {1.00F, 0.84F, 0.30F};
    private static final float[] WHITE = {1.00F, 1.00F, 1.00F};

    private SealFx() {}

    public static void play(S2CFxPacket msg) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity entity = level.getEntity(msg.entityId());
        Vec3 pos = new Vec3(msg.x(), msg.y(), msg.z());

        switch (msg.type()) {
            case SEAL -> seal(level, pos, entity);
            case SEAL_BREAK -> sealBreak(level, pos);
            case LEVEL_UP -> levelUp(level, pos);
            case MANTO_ON -> mantoOn(level, pos);
            case MANTO_OFF -> burst(level, pos.add(0, 1, 0), LodestoneParticleRegistry.SMOKE_PARTICLE, IRON_DARK, IRON_DARK, 0.25F, 20, 0.04, 0.6, 12);
            case IRON_FEET -> ironFeet(level, pos);
            case GOLEM_FORM_ON -> golemFormOn(level, pos);
            case GOLEM_FORM_OFF -> burst(level, pos.add(0, 1.5, 0), LodestoneParticleRegistry.SMOKE_PARTICLE, IRON_DARK, RUST, 0.4F, 30, 0.06, 0.8, 25);
            case GOLEM_PUNCH -> golemPunch(level, pos);
            case HOLY_SUMMON -> holySummon(level, pos);
            case HOLY_VANISH -> burst(level, pos.add(0, 1.3, 0), LodestoneParticleRegistry.WISP_PARTICLE, HOLY_LIGHT, HOLY, 0.35F, 30, 0.08, 0.9, 40);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Efectos
    // ---------------------------------------------------------------------------------------

    /** El mob se convierte en una espiral de luz que entra en el jugador. */
    private static void seal(Level level, Vec3 from, Entity player) {
        burst(level, from, LodestoneParticleRegistry.WISP_PARTICLE, GOLD, RUST, 0.45F, 25, 0.1, 1.0, 40);
        if (player == null) return;
        Vec3 to = player.position().add(0, 1, 0);
        Vec3 dir = to.subtract(from);
        int steps = 40;
        for (int i = 0; i < steps; i++) {
            double t = i / (double) steps;
            double angle = t * Math.PI * 6;
            Vec3 p = from.add(dir.scale(t)).add(Math.cos(angle) * 0.6 * (1 - t), Math.sin(angle) * 0.6 * (1 - t), Math.sin(angle) * 0.6 * (1 - t));
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.3F, 0.05F).build())
                    .setTransparencyData(GenericParticleData.create(0.9F, 0F).build())
                    .setColorData(color(GOLD, IRON))
                    .setLifetime(12 + i / 2)
                    .setMotion(dir.scale(0.02))
                    .enableNoClip()
                    .spawn(level, p.x, p.y, p.z);
        }
        ring(level, to.subtract(0, 0.9, 0), LodestoneParticleRegistry.SPARKLE_PARTICLE, GOLD, POPPY, 1.2, 24, 0.12F);
    }

    private static void sealBreak(Level level, Vec3 pos) {
        Vec3 c = pos.add(0, 1, 0);
        burst(level, c, LodestoneParticleRegistry.SPARK_PARTICLE, POPPY, RUST, 0.35F, 25, 0.35, 0.4, 50);
        burst(level, c, LodestoneParticleRegistry.SMOKE_PARTICLE, IRON_DARK, IRON_DARK, 0.6F, 40, 0.08, 1.0, 30);
        ring(level, pos.add(0, 0.1, 0), LodestoneParticleRegistry.WISP_PARTICLE, POPPY, RUST, 2.5, 40, 0.3F);
    }

    private static void levelUp(Level level, Vec3 pos) {
        RandomSource r = level.random;
        for (int i = 0; i < 30; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            WorldParticleBuilder.create(LodestoneParticleRegistry.STAR_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.25F, 0.0F).setEasing(Easing.QUAD_OUT).build())
                    .setTransparencyData(GenericParticleData.create(1F, 0F).build())
                    .setColorData(color(GOLD, WHITE))
                    .setSpinData(SpinParticleData.createRandomDirection(r, 0.2F).build())
                    .setLifetime(30 + r.nextInt(15))
                    .setMotion(Math.cos(a) * 0.03, 0.08 + r.nextDouble() * 0.06, Math.sin(a) * 0.03)
                    .enableNoClip()
                    .spawn(level, pos.x + Math.cos(a) * 0.6, pos.y + 0.2, pos.z + Math.sin(a) * 0.6);
        }
    }

    private static void mantoOn(Level level, Vec3 pos) {
        Vec3 c = pos.add(0, 1, 0);
        burst(level, c, LodestoneParticleRegistry.SPARKLE_PARTICLE, WHITE, IRON, 0.3F, 20, 0.05, 0.7, 25);
        // Columna de chispas de hierro que sube por el cuerpo
        for (int i = 0; i < 16; i++) {
            double a = i / 16.0 * Math.PI * 2;
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.2F, 0.05F).build())
                    .setTransparencyData(GenericParticleData.create(0.8F, 0F).build())
                    .setColorData(color(IRON, IRON_DARK))
                    .setLifetime(20)
                    .setMotion(0, 0.1, 0)
                    .enableNoClip()
                    .spawn(level, pos.x + Math.cos(a) * 0.5, pos.y, pos.z + Math.sin(a) * 0.5);
        }
    }

    /** Aterrizaje: anillo metálico y chispas en los pies. */
    private static void ironFeet(Level level, Vec3 pos) {
        Vec3 p = pos.add(0, 0.05, 0);
        ring(level, p, LodestoneParticleRegistry.SPARK_PARTICLE, WHITE, IRON, 1.5, 28, 0.25F);
        ring(level, p, LodestoneParticleRegistry.SMOKE_PARTICLE, IRON_DARK, IRON_DARK, 1.0, 16, 0.4F);
    }

    private static void golemFormOn(Level level, Vec3 pos) {
        Vec3 c = pos.add(0, 1.5, 0);
        burst(level, c, LodestoneParticleRegistry.SPARK_PARTICLE, WHITE, RUST, 0.3F, 20, 0.4, 0.5, 40);
        burst(level, c, LodestoneParticleRegistry.SMOKE_PARTICLE, IRON_DARK, IRON_DARK, 0.7F, 40, 0.06, 1.2, 25);
        ring(level, pos.add(0, 0.1, 0), LodestoneParticleRegistry.WISP_PARTICLE, IRON, POPPY, 2.0, 36, 0.3F);
    }

    private static void golemPunch(Level level, Vec3 pos) {
        Vec3 c = pos.add(0, 0.8, 0);
        burst(level, c, LodestoneParticleRegistry.SPARK_PARTICLE, WHITE, IRON, 0.25F, 12, 0.35, 0.3, 18);
        burst(level, c, LodestoneParticleRegistry.SMOKE_PARTICLE, IRON_DARK, IRON_DARK, 0.4F, 20, 0.05, 0.4, 6);
    }

    private static void holySummon(Level level, Vec3 pos) {
        ring(level, pos.add(0, 0.1, 0), LodestoneParticleRegistry.WISP_PARTICLE, HOLY_LIGHT, HOLY, 1.8, 40, 0.15F);
        RandomSource r = level.random;
        for (int i = 0; i < 40; i++) {
            double a = i / 40.0 * Math.PI * 4;
            double h = i / 40.0 * 2.8;
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.35F, 0F).build())
                    .setTransparencyData(GenericParticleData.create(0.9F, 0F).build())
                    .setColorData(color(HOLY_LIGHT, HOLY))
                    .setLifetime(25 + r.nextInt(10))
                    .setMotion(0, 0.02, 0)
                    .enableNoClip()
                    .spawn(level, pos.x + Math.cos(a) * 1.0, pos.y + h, pos.z + Math.sin(a) * 1.0);
        }
        burst(level, pos.add(0, 1.4, 0), LodestoneParticleRegistry.STAR_PARTICLE, WHITE, HOLY, 0.3F, 30, 0.1, 0.8, 15);
    }

    // ---------------------------------------------------------------------------------------
    // Efectos continuos
    // ---------------------------------------------------------------------------------------

    /** Aura azul del Holy golem. */
    public static void holyAmbient(HolyGolemEntity golem) {
        Level level = golem.level();
        RandomSource r = level.random;
        if (golem.tickCount % 2 != 0) return;
        double x = golem.getX() + (r.nextDouble() - 0.5) * golem.getBbWidth();
        double y = golem.getY() + r.nextDouble() * golem.getBbHeight();
        double z = golem.getZ() + (r.nextDouble() - 0.5) * golem.getBbWidth();
        WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                .setScaleData(GenericParticleData.create(0.25F, 0F).build())
                .setTransparencyData(GenericParticleData.create(0.6F, 0F).build())
                .setColorData(color(HOLY_LIGHT, HOLY))
                .setLifetime(20 + r.nextInt(10))
                .setMotion(0, 0.03, 0)
                .enableNoClip()
                .spawn(level, x, y, z);
    }

    /** Destellos metálicos ocasionales sobre quien lleva el manto. */
    public static void mantoAmbient(Player player) {
        Level level = player.level();
        RandomSource r = level.random;
        if (r.nextInt(6) != 0) return;
        double x = player.getX() + (r.nextDouble() - 0.5) * player.getBbWidth() * 1.4;
        double y = player.getY() + r.nextDouble() * player.getBbHeight();
        double z = player.getZ() + (r.nextDouble() - 0.5) * player.getBbWidth() * 1.4;
        WorldParticleBuilder.create(LodestoneParticleRegistry.SPARKLE_PARTICLE)
                .setScaleData(GenericParticleData.create(0.12F, 0F).build())
                .setTransparencyData(GenericParticleData.create(0.9F, 0F).build())
                .setColorData(color(WHITE, IRON))
                .setLifetime(10 + r.nextInt(6))
                .enableNoClip()
                .spawn(level, x, y, z);
    }

    // ---------------------------------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------------------------------

    private static ColorParticleData color(float[] a, float[] b) {
        return ColorParticleData.create(a[0], a[1], a[2], b[0], b[1], b[2]).build();
    }

    private static void burst(Level level, Vec3 pos, RegistryObject<LodestoneWorldParticleType> type,
                              float[] from, float[] to, float scale, int lifetime, double speed, double offset, int count) {
        WorldParticleBuilder.create(type)
                .setScaleData(GenericParticleData.create(scale, 0F).build())
                .setTransparencyData(GenericParticleData.create(0.9F, 0F).build())
                .setColorData(color(from, to))
                .setLifetime(lifetime)
                .setRandomMotion(speed)
                .setRandomOffset(offset)
                .enableNoClip()
                .repeat(level, pos.x, pos.y, pos.z, count);
    }

    /** Anillo horizontal que se expande desde el centro. */
    private static void ring(Level level, Vec3 center, RegistryObject<LodestoneWorldParticleType> type,
                             float[] from, float[] to, double radius, int count, float scale) {
        for (int i = 0; i < count; i++) {
            double a = i / (double) count * Math.PI * 2;
            double cos = Math.cos(a), sin = Math.sin(a);
            WorldParticleBuilder.create(type)
                    .setScaleData(GenericParticleData.create(scale, 0F).build())
                    .setTransparencyData(GenericParticleData.create(0.9F, 0F).build())
                    .setColorData(color(from, to))
                    .setLifetime(18)
                    .setMotion(cos * radius * 0.08, 0.01, sin * radius * 0.08)
                    .enableNoClip()
                    .spawn(level, center.x + cos * 0.3, center.y, center.z + sin * 0.3);
        }
    }

    @SuppressWarnings("unused")
    private static float lerp(float a, float b, float t) {
        return Mth.lerp(t, a, b);
    }
}
