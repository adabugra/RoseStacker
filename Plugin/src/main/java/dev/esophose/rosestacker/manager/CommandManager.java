package dev.esophose.rosestacker.manager;

import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.CommandCompletions;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.PaperCommandManager;
import co.aikar.locales.MessageKey;
import dev.esophose.rosestacker.RoseStacker;
import dev.esophose.rosestacker.command.RoseCommand;
import dev.esophose.rosestacker.command.RoseCommand.ClearallType;
import dev.esophose.rosestacker.command.RoseCommand.StackType;
import dev.esophose.rosestacker.manager.ConfigurationManager.Setting;
import dev.esophose.rosestacker.stack.settings.EntityStackSettings;
import dev.esophose.rosestacker.utils.StackerUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.entity.EntityType;

public class CommandManager extends Manager {

    private boolean loaded;

    public CommandManager(RoseStacker roseStacker) {
        super(roseStacker);

        this.loaded = false;
    }

    @Override
    public void reload() {
        if (!this.loaded) {
            LocaleManager localeManager = this.roseStacker.getManager(LocaleManager.class);
            ConversionManager conversionManager = this.roseStacker.getManager(ConversionManager.class);
            StackSettingManager stackSettingManager = this.roseStacker.getManager(StackSettingManager.class);
            PaperCommandManager commandManager = new PaperCommandManager(this.roseStacker);
            commandManager.registerCommand(new RoseCommand(this.roseStacker));

            // Load custom message strings
            Map<String, String> acfCoreMessages = localeManager.getAcfCoreMessages();
            Map<String, String> acfMinecraftMessages = localeManager.getAcfMinecraftMessages();
            for (String key : acfCoreMessages.keySet())
                commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKey.of("acf-core." + key), localeManager.getLocaleMessage("prefix") + acfCoreMessages.get(key));
            for (String key : acfMinecraftMessages.keySet())
                commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKey.of("acf-minecraft." + key), localeManager.getLocaleMessage("prefix") + acfMinecraftMessages.get(key));

            CommandCompletions<BukkitCommandCompletionContext> completions = commandManager.getCommandCompletions();
            completions.registerStaticCompletion("amount", () -> Arrays.asList("5", "16", "64", "256", "<amount>"));
            completions.registerStaticCompletion("stackableBlockMaterial", () -> stackSettingManager.getStackableBlockTypes().stream().map(Enum::name).map(String::toLowerCase).collect(Collectors.toSet()));
            completions.registerStaticCompletion("spawnableEntityType", () -> StackerUtils.getAlphabeticalStackableEntityTypes().stream().map(Enum::name).map(String::toLowerCase).collect(Collectors.toSet()));
            completions.registerStaticCompletion("blockStackAmounts", () -> {
                int maxStackAmount = Setting.BLOCK_MAX_STACK_SIZE.getInt();
                return Arrays.asList(String.valueOf(maxStackAmount), String.valueOf(maxStackAmount / 2), String.valueOf(maxStackAmount / 4), "<amount>");
            });
            completions.registerStaticCompletion("spawnerStackAmounts", () -> {
                int maxStackAmount = Setting.SPAWNER_MAX_STACK_SIZE.getInt();
                return Arrays.asList(String.valueOf(maxStackAmount), String.valueOf(maxStackAmount / 2), String.valueOf(maxStackAmount / 4), "<amount>");
            });
            completions.registerAsyncCompletion("entityStackAmounts", ctx -> {
                EntityType entityType = ctx.getContextValue(EntityType.class);
                if (entityType != null) {
                    EntityStackSettings entityStackSettings = stackSettingManager.getEntityStackSettings(entityType);
                    int maxStackAmount = entityStackSettings.getMaxStackSize();
                    return Arrays.asList(String.valueOf(maxStackAmount), String.valueOf(maxStackAmount / 2), String.valueOf(maxStackAmount / 4), "<amount>");
                }
                return Collections.emptySet();
            });
            completions.registerStaticCompletion("clearallType", () -> Stream.of(ClearallType.values()).map(Enum::name).map(String::toLowerCase).collect(Collectors.toSet()));
            completions.registerStaticCompletion("stackType", () -> Stream.of(StackType.values()).map(Enum::name).map(String::toLowerCase).collect(Collectors.toSet()));
            completions.registerAsyncCompletion("conversionType", ctx -> conversionManager.getEnabledConverters().stream().map(Enum::name).collect(Collectors.toSet()));

            commandManager.getCommandConditions().addCondition(int.class, "limits", (c, exec, value) -> {
                if (value == null)
                    return;

                if (c.hasConfig("min") && c.getConfigValue("min", 0) > value)
                    throw new ConditionFailedException(MessageKeys.PLEASE_SPECIFY_AT_LEAST, "{min}", String.valueOf(c.getConfigValue("min", 0)));

                if (c.hasConfig("max") && c.getConfigValue("max", Integer.MAX_VALUE) < value)
                    throw new ConditionFailedException(MessageKeys.PLEASE_SPECIFY_AT_MOST, "{max}", String.valueOf(c.getConfigValue("max", Integer.MAX_VALUE)));
            });

            this.loaded = true;
        }
    }

    @Override
    public void disable() {

    }

}