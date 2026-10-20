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
    private static final float[] BLOOD = {0.85F, 0.05F, 0.05F};
    private static final float[] EMBER = {1.00F, 0.25F, 0.10F};
    private static final float[] FUR = {0.30F, 0.27F, 0.24F};

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
            case DOOR_DUST -> doorDust(level, pos);
            case TOUCH_GROUND -> touchGround(level, pos);
            case BOND -> bond(level, pos);
            case RELEASE -> release(level, pos);
            case INSTINCT -> instinct(level, pos);
            case WEREWOLF -> werewolf(level, pos);
            case CLAW -> claw(level, pos, entity);
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

    /** Polvo de luz dorada al abrirse una columna de la puerta. */
    private static void doorDust(Level level, Vec3 pos) {
        RandomSource r = level.random;
        for (int i = 0; i < 20; i++) {
            double y = pos.y + (r.nextDouble() - 0.5) * 18;
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.4F, 0F).build())
                    .setTransparencyData(GenericParticleData.create(0.8F, 0F).build())
                    .setColorData(color(GOLD, WHITE))
                    .setLifetime(30 + r.nextInt(20))
                    .setMotion((r.nextDouble() - 0.5) * 0.05, 0.01, -0.03 - r.nextDouble() * 0.05)
                    .enableNoClip()
                    .spawn(level, pos.x, y, pos.z);
        }
    }

    /** El jugador toca el suelo: ondas metálicas y chispas donde aparece el mini golem. */
    private static void touchGround(Level level, Vec3 pos) {
        Vec3 p = pos.add(0, 0.05, 0);
        ring(level, p, LodestoneParticleRegistry.WISP_PARTICLE, IRON, GOLD, 1.2, 24, 0.15F);
        burst(level, pos.add(0, 0.4, 0), LodestoneParticleRegistry.SPARKLE_PARTICLE, WHITE, IRON, 0.2F, 18, 0.08, 0.3, 16);
    }

    /** BOUND: columna de luz dorada que envuelve al jugador. */
    private static void bond(Level level, Vec3 pos) {
        RandomSource r = level.random;
        for (int i = 0; i < 60; i++) {
            double a = i / 60.0 * Math.PI * 6;
            double h = i / 60.0 * 2.5;
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.35F, 0F).build())
                    .setTransparencyData(GenericParticleData.create(0.9F, 0F).build())
                    .setColorData(color(GOLD, POPPY))
                    .setLifetime(30 + r.nextInt(10))
                    .setMotion(0, 0.03, 0)
                    .enableNoClip()
                    .spawn(level, pos.x + Math.cos(a) * 0.8, pos.y + h, pos.z + Math.sin(a) * 0.8);
        }
        levelUp(level, pos);
    }

    /** El sello se rompe en paz: pétalos y luz. */
    private static void release(Level level, Vec3 pos) {
        burst(level, pos.add(0, 1, 0), LodestoneParticleRegistry.WISP_PARTICLE, GOLD, WHITE, 0.4F, 40, 0.06, 1.0, 40);
        burst(level, pos.add(0, 1, 0), LodestoneParticleRegistry.SPARKLE_PARTICLE, POPPY, GOLD, 0.25F, 40, 0.08, 1.2, 30);
    }

    // ---------------------------------------------------------------------------------------
    // Lobo
    // ---------------------------------------------------------------------------------------

    /** Instinto primal: un latido rojo que se expande. */
    private static void instinct(Level level, Vec3 pos) {
        ring(level, pos.add(0, 0.1, 0), LodestoneParticleRegistry.WISP_PARTICLE, BLOOD, EMBER, 3.0, 40, 0.2F);
        ring(level, pos.add(0, 1.0, 0), LodestoneParticleRegistry.WISP_PARTICLE, EMBER, BLOOD, 2.2, 30, 0.12F);
    }

    /** Transformación en hombre lobo: humo oscuro, chispas rojas y pelo que sale volando. */
    private static void werewolf(Level level, Vec3 pos) {
        Vec3 c = pos.add(0, 1.1, 0);
        burst(level, c, LodestoneParticleRegistry.SMOKE_PARTICLE, FUR, IRON_DARK, 0.8F, 40, 0.07, 0.9, 30);
        burst(level, c, LodestoneParticleRegistry.SPARK_PARTICLE, EMBER, BLOOD, 0.25F, 20, 0.3, 0.4, 30);
        burst(level, c, LodestoneParticleRegistry.WISP_PARTICLE, FUR, FUR, 0.15F, 30, 0.12, 0.6, 25);
    }

    /** Zarpazo: tres tajos rojos en diagonal. */
    private static void claw(Level level, Vec3 pos, Entity target) {
        Vec3 c = pos.add(0, target != null ? target.getBbHeight() * 0.6 : 1.0, 0);
        RandomSource r = level.random;
        for (int line = -1; line <= 1; line++) {
            for (int i = 0; i < 6; i++) {
                double t = i / 5.0 - 0.5;
                WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                        .setScaleData(GenericParticleData.create(0.15F, 0F).build())
                        .setTransparencyData(GenericParticleData.create(1F, 0F).build())
                        .setColorData(color(EMBER, BLOOD))
                        .setLifetime(8 + r.nextInt(4))
                        .enableNoClip()
                        .spawn(level, c.x + t * 0.8 + line * 0.15, c.y - t * 0.8, c.z + line * 0.15);
            }
        }
    }

    /** Estela de los ojos rojos (dos puntos de luz que se quedan atrás al moverse). */
    public static void eyeTrail(Player player) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = Vec3.directionFromRotation(0, player.getYHeadRot());
        Vec3 right = new Vec3(-look.z, 0, look.x);
        double scale = com.markadrii.seals.seal.SealVisuals.werewolf(player) ? 1.1 : 1.0;
        for (int side = -1; side <= 1; side += 2) {
            Vec3 p = eye.add(look.scale(0.27 * scale)).add(right.scale(0.11 * side * scale)).add(0, 0.03, 0);
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.07F, 0.0F).build())
                    .setTransparencyData(GenericParticleData.create(0.9F, 0F).build())
                    .setColorData(color(EMBER, BLOOD))
                    .setLifetime(10)
                    .setFullBrightLighting()
                    .enableNoClip()
                    .spawn(level, p.x, p.y, p.z);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Mundo interior
    // ---------------------------------------------------------------------------------------

    /** Niebla dorada y rayos de luz que salen de la puerta. */
    public static void innerAmbient(Level level, net.minecraft.core.BlockPos door, Player player) {
        RandomSource r = level.random;
        double floorY = door.getY() - 10;

        // Nubes doradas: grandes bolas de humo que flotan despacio por encima
        for (int i = 0; i < 2; i++) {
            double x = player.getX() + (r.nextDouble() - 0.5) * 70;
            double z = player.getZ() + (r.nextDouble() - 0.5) * 70;
            double y = floorY + 14 + r.nextDouble() * 10;
            // cada nube es un pequeño grupo de bolas para que tenga forma
            for (int j = 0; j < 3; j++) {
                WorldParticleBuilder.create(LodestoneParticleRegistry.SMOKE_PARTICLE)
                        .setScaleData(GenericParticleData.create(3.0F, 5.5F, 4.0F).build())
                        .setTransparencyData(GenericParticleData.create(0F, 0.22F, 0F).build())
                        .setColorData(color(GOLD, RUST))
                        .setSpinData(SpinParticleData.createRandomDirection(r, 0.01F).build())
                        .setLifetime(220 + r.nextInt(80))
                        .setMotion(0.02 + r.nextDouble() * 0.01, 0, 0.005)
                        .setFullBrightLighting()
                        .enableNoClip()
                        .spawn(level, x + (r.nextDouble() - 0.5) * 6, y + (r.nextDouble() - 0.5) * 2, z + (r.nextDouble() - 0.5) * 6);
            }
        }

        // Niebla baja alrededor del jugador, a ras del agua
        for (int i = 0; i < 2; i++) {
            double x = player.getX() + (r.nextDouble() - 0.5) * 40;
            double z = player.getZ() + (r.nextDouble() - 0.5) * 40;
            WorldParticleBuilder.create(LodestoneParticleRegistry.SMOKE_PARTICLE)
                    .setScaleData(GenericParticleData.create(2.0F, 3.5F).build())
                    .setTransparencyData(GenericParticleData.create(0F, 0.10F, 0F).build())
                    .setColorData(color(GOLD, IRON_DARK))
                    .setLifetime(120 + r.nextInt(60))
                    .setMotion((r.nextDouble() - 0.5) * 0.01, 0.001, (r.nextDouble() - 0.5) * 0.01)
                    .setFullBrightLighting()
                    .enableNoClip()
                    .spawn(level, x, floorY + 0.3 + r.nextDouble() * 1.2, z);
        }

        // Niebla baja sobre el agua, más densa cerca de la puerta
        for (int i = 0; i < 3; i++) {
            double x = door.getX() + (r.nextDouble() - 0.5) * 30;
            double z = door.getZ() - r.nextDouble() * 40;
            float near = (float) Math.max(0.15, 1.0 - (door.getZ() - z) / 40.0);
            WorldParticleBuilder.create(LodestoneParticleRegistry.SMOKE_PARTICLE)
                    .setScaleData(GenericParticleData.create(1.5F, 2.5F).build())
                    .setTransparencyData(GenericParticleData.create(0F, 0.12F * near, 0F).build())
                    .setColorData(color(GOLD, RUST))
                    .setLifetime(80 + r.nextInt(40))
                    .setMotion((r.nextDouble() - 0.5) * 0.01, 0.002, -0.01)
                    .enableNoClip()
                    .spawn(level, x, door.getY() - 9.5 + r.nextDouble() * 1.5, z);
        }
        // Destellos que flotan delante de la puerta
        if (r.nextInt(2) == 0) {
            double x = door.getX() + (r.nextDouble() - 0.5) * 14;
            double y = door.getY() + (r.nextDouble() - 0.5) * 18;
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.25F, 0F).build())
                    .setTransparencyData(GenericParticleData.create(0F, 0.7F, 0F).build())
                    .setColorData(color(GOLD, WHITE))
                    .setLifetime(60 + r.nextInt(30))
                    .setMotion(0, 0.005, -0.04)
                    .enableNoClip()
                    .spawn(level, x, y, door.getZ() - 0.5);
        }
    }

    /** Onda en el agua bajo los pies al caminar. */
    public static void footRipple(Player player) {
        Level level = player.level();
        double y = Math.floor(player.getY()) + 0.02;
        for (int i = 0; i < 12; i++) {
            double a = i / 12.0 * Math.PI * 2;
            double cos = Math.cos(a), sin = Math.sin(a);
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.12F, 0.05F).build())
                    .setTransparencyData(GenericParticleData.create(0.5F, 0F).build())
                    .setColorData(color(GOLD, RUST))
                    .setLifetime(20)
                    .setMotion(cos * 0.05, 0, sin * 0.05)
                    .enableNoClip()
                    .spawn(level, player.getX() + cos * 0.2, y, player.getZ() + sin * 0.2);
        }
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
