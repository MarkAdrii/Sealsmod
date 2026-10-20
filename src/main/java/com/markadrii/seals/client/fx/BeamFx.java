package com.markadrii.seals.client.fx;

import com.markadrii.seals.network.S2CBeamPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rayo abisal: una línea de luz entre el jugador y su objetivo que se carga durante 2 segundos,
 * pasando de azul a amarillo, como el rayo de los guardianes.
 */
public final class BeamFx {
    private record Beam(int targetId, long endTick, int total) {}

    private static final Map<Integer, Beam> BEAMS = new ConcurrentHashMap<>();

    private BeamFx() {}

    public static void receive(S2CBeamPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (msg.targetId() < 0 || msg.ticks() <= 0) {
            BEAMS.remove(msg.playerId());
        } else {
            BEAMS.put(msg.playerId(), new Beam(msg.targetId(), mc.level.getGameTime() + msg.ticks(), msg.ticks()));
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || BEAMS.isEmpty()) return;
        long now = level.getGameTime();
        RandomSource r = level.random;

        Iterator<Map.Entry<Integer, Beam>> it = BEAMS.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            Beam beam = e.getValue();
            Entity source = level.getEntity(e.getKey());
            Entity target = level.getEntity(beam.targetId());
            if (source == null || target == null || now > beam.endTick()) {
                it.remove();
                continue;
            }
            float progress = 1.0F - (beam.endTick() - now) / (float) beam.total();
            // Azul → amarillo, como el guardián
            float red = 0.25F + 0.75F * progress;
            float green = 0.85F + 0.15F * progress;
            float blue = 1.0F - 0.75F * progress;

            Vec3 from = source.getEyePosition().subtract(0, 0.2, 0);
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
            Vec3 dir = to.subtract(from);
            double length = dir.length();
            int steps = (int) (length * 3);
            for (int i = 0; i < steps; i++) {
                double t = (i + r.nextDouble()) / steps;
                Vec3 p = from.add(dir.scale(t));
                WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                        .setScaleData(GenericParticleData.create(0.08F + 0.08F * progress, 0F).build())
                        .setTransparencyData(GenericParticleData.create(0.8F, 0F).build())
                        .setColorData(ColorParticleData.create(red, green, blue, 0.3F, 0.5F, 1.0F).build())
                        .setLifetime(3)
                        .setFullBrightLighting()
                        .enableNoClip()
                        .spawn(level, p.x, p.y, p.z);
            }
        }
    }

    public static void clear() {
        BEAMS.clear();
    }
}
