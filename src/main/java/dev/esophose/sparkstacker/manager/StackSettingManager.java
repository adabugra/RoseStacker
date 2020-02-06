package dev.esophose.sparkstacker.manager;

import dev.esophose.sparkstacker.SparkStacker;
import dev.esophose.sparkstacker.config.CommentedFileConfiguration;
import dev.esophose.sparkstacker.stack.settings.BlockStackSettings;
import dev.esophose.sparkstacker.stack.settings.EntityStackSettings;
import dev.esophose.sparkstacker.stack.settings.ItemStackSettings;
import dev.esophose.sparkstacker.stack.settings.SpawnerStackSettings;
import dev.esophose.sparkstacker.utils.ClassUtils;
import dev.esophose.sparkstacker.utils.StackerUtils;
import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

public class StackSettingManager extends Manager {

    private static final String PACKAGE_PATH = "dev.esophose.sparkstacker.stack.settings.entity";

    private Map<Material, BlockStackSettings> blockSettings;
    private Map<EntityType, EntityStackSettings> entitySettings;
    private Map<Material, ItemStackSettings> itemSettings;
    private Map<EntityType, SpawnerStackSettings> spawnerSettings;

    public StackSettingManager(SparkStacker sparkStacker) {
        super(sparkStacker);

        this.blockSettings = new HashMap<>();
        this.entitySettings = new HashMap<>();
        this.itemSettings = new HashMap<>();
        this.spawnerSettings = new HashMap<>();
    }

    @Override
    public void reload() {
        // Clear existing settings
        this.blockSettings.clear();
        this.entitySettings.clear();
        this.itemSettings.clear();
        this.spawnerSettings.clear();

        // Settings files
        File blockSettingsFile = new File(this.sparkStacker.getDataFolder(), "block_settings.yml");
        File entitySettingsFile = new File(this.sparkStacker.getDataFolder(), "entity_settings.yml");
        File itemSettingsFile = new File(this.sparkStacker.getDataFolder(), "item_settings.yml");
        File spawnerSettingsFile = new File(this.sparkStacker.getDataFolder(), "spawner_settings.yml");

        // Load block settings
        CommentedFileConfiguration blockSettingsConfiguration = CommentedFileConfiguration.loadConfiguration(this.sparkStacker, blockSettingsFile);
        Stream.of(Material.values()).filter(Material::isBlock).sorted(Comparator.comparing(Enum::name)).forEach(x -> {
            BlockStackSettings blockStackSettings = new BlockStackSettings(blockSettingsConfiguration, x);
            this.blockSettings.put(x, blockStackSettings);
        });

        // Load entity settings
        CommentedFileConfiguration entitySettingsConfiguration = CommentedFileConfiguration.loadConfiguration(this.sparkStacker, entitySettingsFile);
        try {
            List<Class<EntityStackSettings>> classes = ClassUtils.getClassesOf(this.sparkStacker, PACKAGE_PATH, EntityStackSettings.class);
            for (Class<EntityStackSettings> clazz : classes) {
                try {
                    EntityStackSettings entityStackSetting = clazz.getConstructor(CommentedFileConfiguration.class).newInstance(entitySettingsConfiguration);
                    this.entitySettings.put(entityStackSetting.getEntityType(), entityStackSetting);
                } catch (Exception e) {
                    // Log entity settings that failed to load
                    // This should only be caused by version incompatibilities
                    String className = clazz.getSimpleName();
                    this.sparkStacker.getLogger().warning("Ignored loading stack settings for entity: " + className.substring(0, className.length() - 13));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Load item settings
        CommentedFileConfiguration itemSettingsConfiguration = CommentedFileConfiguration.loadConfiguration(this.sparkStacker, itemSettingsFile);
        Stream.of(Material.values()).sorted(Comparator.comparing(Enum::name)).forEach(x -> {
            ItemStackSettings itemStackSettings = new ItemStackSettings(itemSettingsConfiguration, x);
            this.itemSettings.put(x, itemStackSettings);
        });

        // Load spawner settings
        CommentedFileConfiguration spawnerSettingsConfiguration = CommentedFileConfiguration.loadConfiguration(this.sparkStacker, spawnerSettingsFile);
        StackerUtils.getStackableEntityTypes().forEach(x -> {
            SpawnerStackSettings spawnerStackSettings = new SpawnerStackSettings(spawnerSettingsConfiguration, x);
            this.spawnerSettings.put(x, spawnerStackSettings);
        });

        // Save files
        blockSettingsConfiguration.save(true);
        entitySettingsConfiguration.save(true);
        itemSettingsConfiguration.save(true);
        spawnerSettingsConfiguration.save(true);
    }

    @Override
    public void disable() {
        this.blockSettings.clear();
        this.entitySettings.clear();
        this.itemSettings.clear();
        this.spawnerSettings.clear();
    }

    public BlockStackSettings getBlockStackSettings(Material material) {
        return this.blockSettings.get(material);
    }

    public BlockStackSettings getBlockStackSettings(Block block) {
        return this.getBlockStackSettings(block.getType());
    }

    public EntityStackSettings getEntityStackSettings(EntityType entityType) {
        return this.entitySettings.get(entityType);
    }

    public EntityStackSettings getEntityStackSettings(LivingEntity entity) {
        return this.getEntityStackSettings(entity.getType());
    }

    public EntityStackSettings getEntityStackSettings(Material material) {
        if (!StackerUtils.isSpawnEgg(material))
            return null;

        for (EntityType key : this.entitySettings.keySet()) {
            EntityStackSettings settings = this.entitySettings.get(key);
            if (settings.getSpawnEggMaterial() == material)
                return settings;
        }

        return null;
    }

    public ItemStackSettings getItemStackSettings(Material material) {
        return this.itemSettings.get(material);
    }

    public ItemStackSettings getItemStackSettings(Item item) {
        return this.getItemStackSettings(item.getItemStack().getType());
    }

    public SpawnerStackSettings getSpawnerStackSettings(EntityType entityType) {
        return this.spawnerSettings.get(entityType);
    }

    public SpawnerStackSettings getSpawnerStackSettings(CreatureSpawner creatureSpawner) {
        return this.getSpawnerStackSettings(creatureSpawner.getSpawnedType());
    }

    public Set<Material> getStackableBlockTypes() {
        return this.blockSettings.values().stream().filter(BlockStackSettings::isStackingEnabled).map(BlockStackSettings::getType).collect(Collectors.toSet());
    }

}