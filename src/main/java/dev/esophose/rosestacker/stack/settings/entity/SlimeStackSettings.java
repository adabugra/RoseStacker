package dev.esophose.rosestacker.stack.settings.entity;

import dev.esophose.rosestacker.config.CommentedFileConfiguration;
import dev.esophose.rosestacker.stack.StackedEntity;
import dev.esophose.rosestacker.stack.settings.EntityStackSettings;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Slime;

public class SlimeStackSettings extends EntityStackSettings {

    private boolean dontStackIfDifferentSize;

    public SlimeStackSettings(CommentedFileConfiguration entitySettingsFileConfiguration) {
        super(entitySettingsFileConfiguration);

        this.dontStackIfDifferentSize = this.entitySettingsConfiguration.getBoolean("dont-stack-if-different-size");
    }

    @Override
    protected boolean canStackWithInternal(StackedEntity stack1, StackedEntity stack2) {
        Slime slime1 = (Slime) stack1.getEntity();
        Slime slime2 = (Slime) stack2.getEntity();

        return !this.dontStackIfDifferentSize || (slime1.getSize() == slime2.getSize());
    }

    @Override
    protected void setDefaultsInternal() {
        this.setIfNotExists("dont-stack-if-different-size", false);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SLIME;
    }

}