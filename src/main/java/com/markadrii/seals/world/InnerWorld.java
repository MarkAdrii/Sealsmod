package com.markadrii.seals.world;

import com.markadrii.seals.Seals;
import com.markadrii.seals.entity.InnerMob;
import net.minecraft.world.entity.Mob;
import com.markadrii.seals.network.ModNetwork;
import com.markadrii.seals.network.S2CFxPacket;
import com.markadrii.seals.network.S2CInnerWorldPacket;
import com.markadrii.seals.registry.ModBlocks;
import com.markadrii.seals.registry.ModEntities;
import com.markadrii.seals.seal.SealData;
import com.markadrii.seals.seal.SealDefinition;
import com.markadrii.seals.seal.SealManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El mundo interior: una dimensión negra con suelo de ondas de agua y una gran puerta dorada.
 * Cada jugador tiene su propia zona. Detrás de la puerta espera el mob sellado.
 *
 * <pre>
 *   z = -4 ........ jugador aparece en z = 0, mirando hacia la puerta
 *   z = 48 ........ puerta (13 de ancho, 20 de alto)
 *   z = 55 ........ el mob sellado
 *   z = 60 ........ fondo de la sala
 * </pre>
 */
public final class InnerWorld {
    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION, Seals.id("inner_world"));

    public static final int FLOOR_Y = 63;
    public static final int HALF_WIDTH = 16;
    public static final int MIN_Z = -4;
    public static final int MAX_Z = 60;
    public static final int DOOR_Z = 48;
    public static final int DOOR_HALF = 6;   // paneles de x = -6 a 6 (con pilar central en x = 0)
    public static final int DOOR_HEIGHT = 20;
    public static final int GOLEM_Z = 55;
    public static final int REGION_SPACING = 2048;

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    /** Una visita en curso. */
    private static final class Session {
        final BlockPos origin;
        UUID golemId;
        int doorStep = -1;   // -1 = cerrada, 0..DOOR_HALF = abriéndose, > DOOR_HALF = abierta
        int doorTimer;
        boolean prompted;    // ya le ha pedido la llave en esta visita

        Session(BlockPos origin) {
            this.origin = origin;
        }
    }

    private InnerWorld() {}

    public static boolean isInside(Entity entity) {
        return entity.level().dimension() == KEY;
    }

    public static boolean isSessionGolem(UUID owner, UUID golem) {
        Session s = SESSIONS.get(owner);
        return s != null && golem.equals(s.golemId);
    }

    public static boolean hasSession(UUID owner) {
        return SESSIONS.containsKey(owner);
    }

    @Nullable
    public static InnerMob golemOf(ServerPlayer player) {
        Session s = SESSIONS.get(player.getUUID());
        ServerLevel level = player.server.getLevel(KEY);
        if (s == null || level == null || s.golemId == null) return null;
        return level.getEntity(s.golemId) instanceof InnerMob g ? g : null;
    }

    private static BlockPos originFor(MinecraftServer server, UUID player) {
        int index = InnerWorldData.get(server).regionOf(player);
        return new BlockPos((index + 1) * REGION_SPACING, FLOOR_Y + 1, 0);
    }

    public static BlockPos doorCenter(BlockPos origin) {
        return origin.offset(0, DOOR_HEIGHT / 2, DOOR_Z);
    }

    // ---------------------------------------------------------------------------------------
    // Entrar / salir
    // ---------------------------------------------------------------------------------------

    public static boolean enter(ServerPlayer player, SealData data) {
        SealDefinition def = data.definition();
        ServerLevel inner = player.server.getLevel(KEY);
        if (def == null || inner == null) {
            player.displayClientMessage(Component.translatable("message.seals.inner.unavailable").withStyle(ChatFormatting.RED), true);
            return false;
        }

        BlockPos origin = originFor(player.server, player.getUUID());
        cleanup(player.server, player.getUUID());
        build(inner, origin);

        // Punto de vuelta
        data.setReturn(player.level().dimension().location().toString(), player.getX(), player.getY(), player.getZ(), player.getYRot());
        data.setInInner(true);
        data.setFighting(false);
        data.setVisitFlags(0);

        // El mob sellado, detrás de la puerta
        Session session = new Session(origin);
        Mob golem = def.createInnerMob(inner);
        if (golem instanceof InnerMob innerMob) {
            Vec3 home = new Vec3(origin.getX() + 0.5, origin.getY(), origin.getZ() + GOLEM_Z + 0.5);
            float s = data.ageScale();
            innerMob.setupInner(player, home, def.innerHealth() * s, def.innerAttack() * s);
            golem.moveTo(home.x, home.y, home.z, 180.0F, 0.0F);
            golem.setYHeadRot(180.0F);
            golem.setYBodyRot(180.0F);
            inner.addFreshEntity(golem);
            session.golemId = golem.getUUID();
        }
        SESSIONS.put(player.getUUID(), session);

        player.teleportTo(inner, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0.0F, 0.0F);
        BlockPos door = doorCenter(origin);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new S2CInnerWorldPacket(true, door.getX(), door.getY(), door.getZ()));
        inner.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.5F);
        player.sendSystemMessage(Component.translatable("message.seals.inner.enter", def.name()).withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** Saca al jugador del mundo interior y le devuelve a donde estaba. */
    public static void exit(ServerPlayer player, SealData data) {
        cleanup(player.server, player.getUUID());
        data.setInInner(false);
        data.setFighting(false);

        ServerLevel dest = null;
        ResourceLocation dim = ResourceLocation.tryParse(data.returnDim());
        if (dim != null) dest = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dim));
        double x = data.returnX(), y = data.returnY(), z = data.returnZ();
        if (dest == null || dest.dimension() == KEY) {
            dest = player.server.overworld();
            BlockPos spawn = dest.getSharedSpawnPos();
            x = spawn.getX() + 0.5;
            y = spawn.getY();
            z = spawn.getZ() + 0.5;
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CInnerWorldPacket(false, 0, 0, 0));
        if (isInside(player)) {
            player.teleportTo(dest, x, y, z, data.returnYaw(), 0.0F);
        }
        SealManager.sync(player);
    }

    /** Termina la visita: quita al mob del mundo interior. */
    public static void cleanup(MinecraftServer server, UUID owner) {
        Session s = SESSIONS.remove(owner);
        if (s == null || s.golemId == null) return;
        ServerLevel inner = server.getLevel(KEY);
        if (inner != null) {
            Entity golem = inner.getEntity(s.golemId);
            if (golem != null) golem.discard();
        }
    }

    // ---------------------------------------------------------------------------------------
    // Puerta que se abre al acercarse
    // ---------------------------------------------------------------------------------------

    public static void tick(MinecraftServer server) {
        ServerLevel inner = server.getLevel(KEY);
        if (inner == null || SESSIONS.isEmpty()) return;

        for (Map.Entry<UUID, Session> e : SESSIONS.entrySet()) {
            Session s = e.getValue();
            if (s.doorStep > DOOR_HALF) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
            if (player == null || player.level() != inner) continue;

            BlockPos door = doorCenter(s.origin);
            if (s.doorStep < 0) {
                double dz = door.getZ() - player.getZ();
                if (dz < 16 && Math.abs(player.getX() - door.getX()) < HALF_WIDTH) {
                    // Algunos mobs no abren la puerta hasta que les traes algo (la primera vez)
                    com.markadrii.seals.seal.SealData d = com.markadrii.seals.seal.SealCapability.get(player);
                    SealDefinition def = d.definition();
                    if (def != null && def.innerKeyItem() != null && !d.innerKeyGiven()) {
                        if (!s.prompted) {
                            s.prompted = true;
                            com.markadrii.seals.bond.BondManager.promptKey(player);
                        }
                        continue;
                    }
                    s.doorStep = 0;
                    s.doorTimer = 0;
                    inner.playSound(null, door, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0F, 0.5F);
                    inner.playSound(null, door, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 2.0F, 0.4F);
                }
                continue;
            }

            if (++s.doorTimer % 5 != 0) continue;
            s.doorStep++;
            int x = s.doorStep;
            if (x <= DOOR_HALF) {
                // Abre una columna a cada lado, del centro hacia fuera
                for (int y = 0; y < DOOR_HEIGHT; y++) {
                    inner.setBlock(s.origin.offset(x, y, DOOR_Z), Blocks.AIR.defaultBlockState(), 3);
                    inner.setBlock(s.origin.offset(-x, y, DOOR_Z), Blocks.AIR.defaultBlockState(), 3);
                }
                SealManager.fx(inner, S2CFxPacket.Type.DOOR_DUST, s.origin.getX() + x + 0.5, door.getY(), door.getZ() + 0.5, -1);
                SealManager.fx(inner, S2CFxPacket.Type.DOOR_DUST, s.origin.getX() - x + 0.5, door.getY(), door.getZ() + 0.5, -1);
                inner.playSound(null, door, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.5F, 0.5F);
            }
            if (x == DOOR_HALF) {
                // El pilar central también se abre
                for (int y = 0; y < DOOR_HEIGHT; y++) {
                    inner.setBlock(s.origin.offset(0, y, DOOR_Z), Blocks.AIR.defaultBlockState(), 3);
                }
                inner.playSound(null, door, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.HOSTILE, 2.0F, 0.5F);
                player.displayClientMessage(Component.translatable("message.seals.inner.door_open")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
        }
    }

    /** Abre la puerta ya (por ejemplo, al entregar la llave). */
    public static void openDoor(ServerPlayer player) {
        Session s = SESSIONS.get(player.getUUID());
        ServerLevel inner = player.server.getLevel(KEY);
        if (s == null || inner == null || s.doorStep >= 0) return;
        s.doorStep = 0;
        s.doorTimer = 0;
        BlockPos door = doorCenter(s.origin);
        inner.playSound(null, door, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0F, 0.5F);
        inner.playSound(null, door, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 2.0F, 0.4F);
    }

    // ---------------------------------------------------------------------------------------
    // Construcción
    // ---------------------------------------------------------------------------------------

    private static void build(ServerLevel level, BlockPos o) {
        BlockState floor = ModBlocks.SPIRIT_FLOOR.get().defaultBlockState();
        BlockState door = ModBlocks.SPIRIT_DOOR.get().defaultBlockState();
        BlockState frame = ModBlocks.SPIRIT_FRAME.get().defaultBlockState();
        BlockState lattice = ModBlocks.SPIRIT_LATTICE.get().defaultBlockState();
        BlockState barrier = Blocks.BARRIER.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        int top = DOOR_HEIGHT + 3;

        for (int x = -HALF_WIDTH - 1; x <= HALF_WIDTH + 1; x++) {
            for (int z = MIN_Z - 1; z <= MAX_Z + 1; z++) {
                boolean edge = x == -HALF_WIDTH - 1 || x == HALF_WIDTH + 1 || z == MIN_Z - 1 || z == MAX_Z + 1;
                level.setBlock(o.offset(x, -1, z), edge ? barrier : floor, 2);
                for (int y = 0; y <= top; y++) {
                    BlockPos pos = o.offset(x, y, z);
                    BlockState want = edge ? barrier : air;
                    if (level.getBlockState(pos) != want) level.setBlock(pos, want, 2);
                }
            }
        }

        // Sala de la puerta: paredes negras que esconden al mob hasta que se abre
        int roomHalf = DOOR_HALF + 3;
        for (int x = -roomHalf; x <= roomHalf; x++) {
            for (int y = 0; y <= top; y++) {
                for (int z = DOOR_Z; z <= MAX_Z; z++) {
                    boolean wall = Math.abs(x) == roomHalf || z == MAX_Z || y == top || z == DOOR_Z;
                    if (wall) level.setBlock(o.offset(x, y, z), frame, 2);
                }
            }
        }

        // Puerta: dos paneles de luz con barrotes y un pilar central
        for (int y = 0; y < DOOR_HEIGHT; y++) {
            for (int x = -DOOR_HALF; x <= DOOR_HALF; x++) {
                level.setBlock(o.offset(x, y, DOOR_Z), x == 0 ? frame : door, 2);
            }
            // Celosías doradas a los lados
            level.setBlock(o.offset(DOOR_HALF + 1, y, DOOR_Z), lattice, 2);
            level.setBlock(o.offset(-DOOR_HALF - 1, y, DOOR_Z), lattice, 2);
        }
        // Dintel
        for (int x = -roomHalf; x <= roomHalf; x++) {
            level.setBlock(o.offset(x, DOOR_HEIGHT, DOOR_Z), frame, 2);
        }
    }
}
