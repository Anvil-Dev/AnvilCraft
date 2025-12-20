package dev.dubhe.anvilcraft.data.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModDamageTypeTagProvider extends DamageTypeTagsProvider {

    public ModDamageTypeTagProvider(
        PackOutput output,
        CompletableFuture<HolderLookup.Provider> future,
        @Nullable ExistingFileHelper helper
    ) {
        super(output, future, AnvilCraft.MOD_ID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        this.tag(DamageTypeTags.BYPASSES_ARMOR).addOptional(ModDamageTypes.LOST_IN_TIME.location());
        this.tag(DamageTypeTags.BYPASSES_RESISTANCE).addOptional(ModDamageTypes.LOST_IN_TIME.location());
        this.tag(DamageTypeTags.NO_KNOCKBACK).addOptional(ModDamageTypes.LOST_IN_TIME.location());
        this.tag(Tags.DamageTypes.IS_MAGIC).addOptional(ModDamageTypes.LOST_IN_TIME.location());
    }
}
