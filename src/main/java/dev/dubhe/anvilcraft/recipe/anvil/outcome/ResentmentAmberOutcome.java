package dev.dubhe.anvilcraft.recipe.anvil.outcome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.recipe.cache.TagCache;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.v2.recipe.outcome.SpawnItem;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeOutcomeTypes;
import dev.dubhe.anvilcraft.item.property.component.SavedEntity;
import dev.dubhe.anvilcraft.util.ResentmentUtil;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public record ResentmentAmberOutcome(Vec3 offset, ResourceLocation savedEntityPath)
    implements IRecipeOutcome<ResentmentAmberOutcome> {

    @Override
    public Type getType() {
        return ModRecipeOutcomeTypes.RESENTMENT_AMBER.get();
    }

    @Override
    public void accept(InWorldRecipeContext context) {
        Tag tag = context.computeIfAbsent(TagCache.TAG_CACHE).getTag(this.savedEntityPath);
        if (tag == null) return;
        Optional<SavedEntity> optional = SavedEntity.CODEC.parse(context.getNbtRegistryOps(), tag).result();
        if (optional.isEmpty()) return;

        SavedEntity savedEntity = optional.get();
        int resentment = ResentmentUtil.getResentment(savedEntity, context.getLevel());
        boolean resentful = context.getLevel().getRandom().nextFloat() < resentment / 100.0f;
        ItemStack result = resentful
            ? ModBlocks.RESENTFUL_AMBER_BLOCK.asStack()
            : ModBlocks.MOB_AMBER_BLOCK.asStack();
        result.set(ModComponents.SAVED_ENTITY, savedEntity);
        SpawnItem.builder().item(result).offset(this.offset).build().accept(context);
    }

    public static class Type implements IRecipeOutcome.Type<ResentmentAmberOutcome> {
        public static final MapCodec<ResentmentAmberOutcome> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                Vec3.CODEC.fieldOf("offset").forGetter(ResentmentAmberOutcome::offset),
                ResourceLocation.CODEC.fieldOf("saved_entity_path").forGetter(ResentmentAmberOutcome::savedEntityPath)
            ).apply(instance, ResentmentAmberOutcome::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, ResentmentAmberOutcome> STREAM_CODEC =
            StreamCodec.composite(
                StreamCodecUtil.VEC3,
                ResentmentAmberOutcome::offset,
                ResourceLocation.STREAM_CODEC,
                ResentmentAmberOutcome::savedEntityPath,
                ResentmentAmberOutcome::new
            );

        @Override
        public MapCodec<ResentmentAmberOutcome> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ResentmentAmberOutcome> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
