package dev.rosewood.rosestacker.nms.v1_16_R3;

import com.google.common.collect.Lists;
import dev.rosewood.rosestacker.nms.NMSAdapter;
import dev.rosewood.rosestacker.nms.NMSHandler;
import dev.rosewood.rosestacker.nms.spawner.StackedSpawnerTile;
import dev.rosewood.rosestacker.nms.storage.StackedEntityDataEntry;
import dev.rosewood.rosestacker.nms.storage.StackedEntityDataStorage;
import dev.rosewood.rosestacker.nms.util.ReflectionUtils;
import dev.rosewood.rosestacker.nms.v1_16_R3.entity.SoloEntitySpider;
import dev.rosewood.rosestacker.nms.v1_16_R3.entity.SoloEntityStrider;
import dev.rosewood.rosestacker.nms.v1_16_R3.spawner.StackedSpawnerTileImpl;
import dev.rosewood.rosestacker.nms.v1_16_R3.storage.NBTStackedEntityDataEntry;
import dev.rosewood.rosestacker.nms.v1_16_R3.storage.NBTStackedEntityDataStorage;
import dev.rosewood.rosestacker.stack.StackedSpawner;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.Chunk;
import net.minecraft.server.v1_16_R3.ChunkStatus;
import net.minecraft.server.v1_16_R3.ControllerMove;
import net.minecraft.server.v1_16_R3.DataWatcher;
import net.minecraft.server.v1_16_R3.DataWatcher.Item;
import net.minecraft.server.v1_16_R3.DataWatcherObject;
import net.minecraft.server.v1_16_R3.DataWatcherRegistry;
import net.minecraft.server.v1_16_R3.Entity;
import net.minecraft.server.v1_16_R3.EntityCreeper;
import net.minecraft.server.v1_16_R3.EntityHuman;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.EntityLiving;
import net.minecraft.server.v1_16_R3.EntitySpider;
import net.minecraft.server.v1_16_R3.EntityStrider;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.EntityZombie;
import net.minecraft.server.v1_16_R3.EnumMobSpawn;
import net.minecraft.server.v1_16_R3.GroupDataEntity;
import net.minecraft.server.v1_16_R3.IChatBaseComponent;
import net.minecraft.server.v1_16_R3.IChunkAccess;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.MathHelper;
import net.minecraft.server.v1_16_R3.MobSpawnerAbstract;
import net.minecraft.server.v1_16_R3.MovingObjectPosition;
import net.minecraft.server.v1_16_R3.NBTBase;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.NBTTagDouble;
import net.minecraft.server.v1_16_R3.NBTTagFloat;
import net.minecraft.server.v1_16_R3.NBTTagList;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_16_R3.PathfinderGoalFloat;
import net.minecraft.server.v1_16_R3.PathfinderGoalSelector;
import net.minecraft.server.v1_16_R3.PathfinderGoalWrapped;
import net.minecraft.server.v1_16_R3.RayTrace;
import net.minecraft.server.v1_16_R3.TileEntity;
import net.minecraft.server.v1_16_R3.TileEntityMobSpawner;
import net.minecraft.server.v1_16_R3.Vec3D;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftCreeper;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftTurtle;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftChatMessage;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftNamespacedKey;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Turtle;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.inventory.ItemStack;
import sun.misc.Unsafe;

@SuppressWarnings("unchecked")
public class NMSHandlerImpl implements NMSHandler {

    private static Field field_PacketPlayOutEntityMetadata_a; // Field to set the entity ID for the packet, normally private
    private static Field field_PacketPlayOutEntityMetadata_b; // Field to set the datawatcher changes for the packet, normally private

    private static Method method_WorldServer_registerEntity; // Method to register an entity into a world

    private static DataWatcherObject<Boolean> value_EntityCreeper_d; // DataWatcherObject that determines if a creeper is ignited, normally private
    private static Field field_EntityCreeper_fuseTicks; // Field to set the remaining fuse ticks of a creeper, normally private

    private static Field field_PathfinderGoalSelector_d; // Field to get a PathfinderGoalSelector of an insentient entity, normally private
    private static Field field_EntityInsentient_moveController; // Field to set the move controller of an insentient entity, normally protected

    private static Field field_Entity_spawnReason; // Spawn reason field (only on Paper servers, will be null for Spigot)

    private static Unsafe unsafe;
    private static long field_SpawnerBlockEntity_spawner_offset; // Field offset for modifying SpawnerBlockEntity's spawner field

    static {
        try {
            field_PacketPlayOutEntityMetadata_a = ReflectionUtils.getFieldByName(PacketPlayOutEntityMetadata.class, "a");
            field_PacketPlayOutEntityMetadata_b = ReflectionUtils.getFieldByName(PacketPlayOutEntityMetadata.class, "b");

            method_WorldServer_registerEntity = ReflectionUtils.getMethodByName(WorldServer.class, "registerEntity", Entity.class);

            Field field_EntityCreeper_d = ReflectionUtils.getFieldByName(EntityCreeper.class, "d");
            value_EntityCreeper_d = (DataWatcherObject<Boolean>) field_EntityCreeper_d.get(null);
            field_EntityCreeper_fuseTicks = ReflectionUtils.getFieldByName(EntityCreeper.class, "fuseTicks");

            field_PathfinderGoalSelector_d = ReflectionUtils.getFieldByName(PathfinderGoalSelector.class, "d");
            field_EntityInsentient_moveController = ReflectionUtils.getFieldByName(EntityInsentient.class, "moveController");

            if (NMSAdapter.isPaper())
                field_Entity_spawnReason = ReflectionUtils.getFieldByPositionAndType(Entity.class, 0, SpawnReason.class);

            Field field_SpawnerBlockEntity_spawner = ReflectionUtils.getFieldByPositionAndType(TileEntityMobSpawner.class, 0, MobSpawnerAbstract.class);
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (Unsafe) unsafeField.get(null);
            field_SpawnerBlockEntity_spawner_offset = unsafe.objectFieldOffset(field_SpawnerBlockEntity_spawner);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public StackedEntityDataEntry<?> getEntityAsNBT(LivingEntity livingEntity) {
        NBTTagCompound nbt = new NBTTagCompound();
        EntityLiving nmsEntity = ((CraftLivingEntity) livingEntity).getHandle();
        nmsEntity.save(nbt);
        return new NBTStackedEntityDataEntry(nbt);
    }

    private void setTag(NBTTagList tag, int index, NBTBase value) {
        if (index >= tag.size()) {
            tag.b(index, value);
        } else {
            tag.a(index, value);
        }
    }

    @Override
    public LivingEntity createEntityFromNBT(StackedEntityDataEntry<?> serialized, Location location, boolean addToWorld, EntityType entityType) {
        try {
            NBTTagCompound nbt = (NBTTagCompound) serialized.get();

            NBTTagList positionTagList = nbt.getList("Pos", 6);
            if (positionTagList == null)
                positionTagList = new NBTTagList();
            this.setTag(positionTagList, 0, NBTTagDouble.a(location.getX()));
            this.setTag(positionTagList, 1, NBTTagDouble.a(location.getY()));
            this.setTag(positionTagList, 2, NBTTagDouble.a(location.getZ()));
            nbt.set("Pos", positionTagList);
            NBTTagList rotationTagList = nbt.getList("Rotation", 5);
            if (rotationTagList == null)
                rotationTagList = new NBTTagList();
            this.setTag(rotationTagList, 0, NBTTagFloat.a(location.getYaw()));
            this.setTag(rotationTagList, 1, NBTTagFloat.a(location.getPitch()));
            nbt.set("Rotation", rotationTagList);
            nbt.a("UUID", UUID.randomUUID()); // Reset the UUID to resolve possible duplicates

            Optional<EntityTypes<?>> optionalEntity = EntityTypes.a(entityType.getKey().getKey());
            if (optionalEntity.isPresent()) {
                WorldServer world = ((CraftWorld) location.getWorld()).getHandle();

                Entity entity = this.createCreature(
                        optionalEntity.get(),
                        world,
                        nbt,
                        null,
                        null,
                        new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                        EnumMobSpawn.COMMAND
                );

                if (entity == null)
                    throw new NullPointerException("Unable to create entity from NBT");

                // Load NBT
                entity.load(nbt);

                if (addToWorld) {
                    IChunkAccess ichunkaccess = world.getChunkAt(MathHelper.floor(entity.locX() / 16.0D), MathHelper.floor(entity.locZ() / 16.0D), ChunkStatus.FULL, true);
                    if (!(ichunkaccess instanceof Chunk))
                        throw new NullPointerException("Unable to spawn entity from NBT, couldn't get chunk");

                    ichunkaccess.a(entity);
                    method_WorldServer_registerEntity.invoke(world, entity);
                    entity.noDamageTicks = 0;
                }

                return (LivingEntity) entity.getBukkitEntity();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public LivingEntity createNewEntityUnspawned(EntityType entityType, Location location, SpawnReason spawnReason) {
        World world = location.getWorld();
        if (world == null)
            return null;

        Class<? extends org.bukkit.entity.Entity> entityClass = entityType.getEntityClass();
        if (entityClass == null || !LivingEntity.class.isAssignableFrom(entityClass))
            throw new IllegalArgumentException("EntityType must be of a LivingEntity");

        EntityTypes<? extends Entity> nmsEntityType = IRegistry.ENTITY_TYPE.get(CraftNamespacedKey.toMinecraft(entityType.getKey()));
        Entity nmsEntity = this.createCreature(
                nmsEntityType,
                ((CraftWorld) world).getHandle(),
                null,
                null,
                null,
                new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                this.toNmsSpawnReason(spawnReason)
        );

        return nmsEntity == null ? null : (LivingEntity) nmsEntity.getBukkitEntity();
    }

    /**
     * Duplicate of {@link EntityTypes#createCreature(WorldServer, NBTTagCompound, IChatBaseComponent, EntityHuman, BlockPosition, EnumMobSpawn, boolean, boolean)}.
     * Contains a patch to prevent chicken jockeys from spawning and to not play the mob sound upon creation.
     */
    private <T extends Entity> T createCreature(EntityTypes<T> entityTypes, WorldServer worldserver, NBTTagCompound nbttagcompound, IChatBaseComponent ichatbasecomponent, EntityHuman entityhuman, BlockPosition blockposition, EnumMobSpawn enummobspawn) {
        T newEntity;
        if (entityTypes == EntityTypes.SPIDER) {
            newEntity = (T) new SoloEntitySpider((EntityTypes<? extends EntitySpider>) entityTypes, worldserver);
        } else if (entityTypes == EntityTypes.STRIDER) {
            newEntity = (T) new SoloEntityStrider((EntityTypes<? extends EntityStrider>) entityTypes, worldserver);
        } else {
            newEntity = entityTypes.a(worldserver);
        }

        if (newEntity == null) {
            return null;
        } else {
            if (field_Entity_spawnReason != null) {
                try {
                    field_Entity_spawnReason.set(newEntity, this.toBukkitSpawnReason(enummobspawn));
                } catch (IllegalAccessException ignored) { }
            }

            newEntity.setPositionRotation(blockposition.getX() + 0.5D, blockposition.getY(), blockposition.getZ() + 0.5D, MathHelper.g(worldserver.random.nextFloat() * 360.0F), 0.0F);
            if (newEntity instanceof EntityInsentient) {
                EntityInsentient entityinsentient = (EntityInsentient)newEntity;
                entityinsentient.aC = entityinsentient.yaw;
                entityinsentient.aA = entityinsentient.yaw;

                GroupDataEntity groupDataEntity = null;
                if (entityTypes == EntityTypes.DROWNED
                        || entityTypes == EntityTypes.HUSK
                        || entityTypes == EntityTypes.ZOMBIE_VILLAGER
                        || entityTypes == EntityTypes.ZOMBIFIED_PIGLIN
                        || entityTypes == EntityTypes.ZOMBIE) {
                    // Don't allow chicken jockeys to spawn
                    groupDataEntity = new EntityZombie.GroupDataZombie(EntityZombie.a(worldserver.getRandom()), false);
                }

                entityinsentient.prepare(worldserver, worldserver.getDamageScaler(entityinsentient.getChunkCoordinates()), enummobspawn, groupDataEntity, nbttagcompound);
            }

            if (ichatbasecomponent != null && newEntity instanceof EntityLiving) {
                newEntity.setCustomName(ichatbasecomponent);
            }

            try {
                EntityTypes.a(worldserver, entityhuman, newEntity, nbttagcompound);
            } catch (Throwable ignored) { }

            return newEntity;
        }
    }

    @Override
    public void spawnExistingEntity(LivingEntity entity, SpawnReason spawnReason, boolean bypassSpawnEvent) {
        Location location = entity.getLocation();
        World world = location.getWorld();
        if (world == null)
            throw new IllegalArgumentException("Entity is not in a loaded world");

        if (bypassSpawnEvent) {
            IChunkAccess ichunkaccess = ((CraftWorld) world).getHandle().getChunkAt(MathHelper.floor(entity.getLocation().getX() / 16.0D), MathHelper.floor(entity.getLocation().getZ() / 16.0D), ChunkStatus.FULL, false);
            if (!(ichunkaccess instanceof Chunk))
                return;

            ichunkaccess.a(((CraftEntity) entity).getHandle());
            ((CraftWorld) world).getHandle().addEntityChunk(((CraftEntity) entity).getHandle());
        } else {
            ((CraftWorld) world).addEntity(((CraftEntity) entity).getHandle(), spawnReason);
        }
    }

    @Override
    public void updateEntityNameTagForPlayer(Player player, org.bukkit.entity.Entity entity, String customName, boolean customNameVisible) {
        try {
            List<Item<?>> dataWatchers = new ArrayList<>();
            Optional<IChatBaseComponent> nameComponent = Optional.ofNullable(CraftChatMessage.fromStringOrNull(customName));
            dataWatchers.add(new DataWatcher.Item<>(DataWatcherRegistry.f.a(2), nameComponent));
            dataWatchers.add(new DataWatcher.Item<>(DataWatcherRegistry.i.a(3), customNameVisible));

            PacketPlayOutEntityMetadata packetPlayOutEntityMetadata = new PacketPlayOutEntityMetadata();
            field_PacketPlayOutEntityMetadata_a.set(packetPlayOutEntityMetadata, entity.getEntityId());
            field_PacketPlayOutEntityMetadata_b.set(packetPlayOutEntityMetadata, dataWatchers);

            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetPlayOutEntityMetadata);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateEntityNameTagVisibilityForPlayer(Player player, org.bukkit.entity.Entity entity, boolean customNameVisible) {
        try {
            PacketPlayOutEntityMetadata packetPlayOutEntityMetadata = new PacketPlayOutEntityMetadata();
            field_PacketPlayOutEntityMetadata_a.set(packetPlayOutEntityMetadata, entity.getEntityId());
            field_PacketPlayOutEntityMetadata_b.set(packetPlayOutEntityMetadata, Lists.newArrayList(new DataWatcher.Item<>(DataWatcherRegistry.i.a(3), customNameVisible)));

            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetPlayOutEntityMetadata);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unigniteCreeper(Creeper creeper) {
        EntityCreeper entityCreeper = ((CraftCreeper) creeper).getHandle();

        entityCreeper.getDataWatcher().set(value_EntityCreeper_d, false);
        try {
            field_EntityCreeper_fuseTicks.set(entityCreeper, entityCreeper.maxFuseTicks);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isTurtlePregnant(Turtle turtle) {
        return ((CraftTurtle) turtle).getHandle().hasEgg();
    }

    @Override
    public void removeEntityGoals(LivingEntity livingEntity) {
        EntityLiving nmsEntity = ((CraftLivingEntity) livingEntity).getHandle();
        if (!(nmsEntity instanceof EntityInsentient))
            return;

        try {
            EntityInsentient insentient = (EntityInsentient) nmsEntity;

            // Remove all goal AI other than floating in water
            Set<PathfinderGoalWrapped> goals = (Set<PathfinderGoalWrapped>) field_PathfinderGoalSelector_d.get(insentient.goalSelector);
            Iterator<PathfinderGoalWrapped> goalsIterator = goals.iterator();
            while (goalsIterator.hasNext()) {
                PathfinderGoalWrapped goal = goalsIterator.next();
                if (goal.j() instanceof PathfinderGoalFloat)
                    continue;

                goalsIterator.remove();
            }

            // Remove all targetting AI
            ((Set<PathfinderGoalWrapped>) field_PathfinderGoalSelector_d.get(insentient.targetSelector)).clear();

            // Forget any existing targets
            insentient.setGoalTarget(null);

            // Remove the move controller and replace it with a dummy one
            ControllerMove dummyMoveController = new ControllerMove(insentient) {
                @Override
                public void a() { }
            };

            field_EntityInsentient_moveController.set(insentient, dummyMoveController);
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public ItemStack setItemStackNBT(ItemStack itemStack, String key, String value) {
        net.minecraft.server.v1_16_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound tagCompound = nmsItem.getOrCreateTag();
        tagCompound.setString(key, value);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    @Override
    public ItemStack setItemStackNBT(ItemStack itemStack, String key, int value) {
        net.minecraft.server.v1_16_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound tagCompound = nmsItem.getOrCreateTag();
        tagCompound.setInt(key, value);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    @Override
    public String getItemStackNBTString(ItemStack itemStack, String key) {
        net.minecraft.server.v1_16_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound tagCompound = nmsItem.getOrCreateTag();
        return tagCompound.getString(key);
    }

    @Override
    public int getItemStackNBTInt(ItemStack itemStack, String key) {
        net.minecraft.server.v1_16_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound tagCompound = nmsItem.getOrCreateTag();
        return tagCompound.getInt(key);
    }

    @Override
    public void setLastHurtBy(LivingEntity livingEntity, Player player) {
        if (player != null)
            ((CraftLivingEntity) livingEntity).getHandle().killer = ((CraftPlayer) player).getHandle();
    }

    @Override
    public boolean hasLineOfSight(LivingEntity entity1, org.bukkit.entity.Entity entity2) {
        EntityLiving nmsEntity1 = ((CraftLivingEntity) entity1).getHandle();
        Entity nmsEntity2 = ((CraftEntity) entity2).getHandle();

        Vec3D vec3d = new Vec3D(nmsEntity1.locX(), nmsEntity1.getHeadY(), nmsEntity1.locZ());
        Vec3D vec3d1 = new Vec3D(nmsEntity2.locX(), nmsEntity2.getHeadY(), nmsEntity2.locZ());
        return nmsEntity1.world.rayTrace(new RayTrace(vec3d, vec3d1, RayTrace.BlockCollisionOption.VISUAL, RayTrace.FluidCollisionOption.NONE, nmsEntity1)).getType() == MovingObjectPosition.EnumMovingObjectType.MISS;
    }

    @Override
    public StackedEntityDataStorage createEntityDataStorage(LivingEntity livingEntity) {
        return new NBTStackedEntityDataStorage(livingEntity);
    }

    @Override
    public StackedEntityDataStorage deserializeEntityDataStorage(byte[] data) {
        return new NBTStackedEntityDataStorage(data);
    }

    @Override
    public StackedSpawnerTile injectStackedSpawnerTile(Object stackedSpawnerObj) {
        StackedSpawner stackedSpawner = (StackedSpawner) stackedSpawnerObj;
        Block block = stackedSpawner.getBlock();
        WorldServer level = ((CraftWorld) block.getWorld()).getHandle();
        TileEntity blockEntity = level.getTileEntity(new BlockPosition(block.getX(), block.getY(), block.getZ()));
        if (blockEntity instanceof TileEntityMobSpawner) {
            TileEntityMobSpawner spawnerBlockEntity = (TileEntityMobSpawner) blockEntity;
            if (!(spawnerBlockEntity.getSpawner() instanceof StackedSpawnerTileImpl)) {
                StackedSpawnerTile stackedSpawnerTile = new StackedSpawnerTileImpl(spawnerBlockEntity.getSpawner(), spawnerBlockEntity, stackedSpawner);
                unsafe.putObject(spawnerBlockEntity, field_SpawnerBlockEntity_spawner_offset, stackedSpawnerTile);
                return stackedSpawnerTile;
            } else {
                StackedSpawnerTileImpl spawnerTile = (StackedSpawnerTileImpl) spawnerBlockEntity.getSpawner();
                spawnerTile.updateStackedSpawner(stackedSpawner);
                return spawnerTile;
            }
        }
        return null;
    }

    private SpawnReason toBukkitSpawnReason(EnumMobSpawn mobSpawnType) {
        switch (mobSpawnType) {
            case SPAWN_EGG:
                return SpawnReason.SPAWNER_EGG;
            case SPAWNER:
                return SpawnReason.SPAWNER;
            default:
                return SpawnReason.CUSTOM;
        }
    }

    private EnumMobSpawn toNmsSpawnReason(SpawnReason spawnReason) {
        switch (spawnReason) {
            case SPAWNER_EGG:
                return EnumMobSpawn.SPAWN_EGG;
            case SPAWNER:
                return EnumMobSpawn.SPAWNER;
            default:
                return EnumMobSpawn.COMMAND;
        }
    }

}
