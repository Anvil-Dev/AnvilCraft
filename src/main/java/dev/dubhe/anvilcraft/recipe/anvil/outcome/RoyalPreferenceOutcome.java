package dev.dubhe.anvilcraft.recipe.anvil.outcome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.recipe.component.ChanceItemStack;
import dev.anvilcraft.lib.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.recipe.util.InWorldRecipeContext;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeOutcomeTypes;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public record RoyalPreferenceOutcome(ChanceItemStack result) implements IRecipeOutcome<RoyalPreferenceOutcome> {

    @Override
    public IRecipeOutcome.Type<RoyalPreferenceOutcome> getType() {
        return ModRecipeOutcomeTypes.ROYAL_PREFERENCE.get();
    }

    @Override
    public void accept(InWorldRecipeContext context) {
        ServerLevel level = context.getLevel();
        Vec3 pos = context.getPos();
        List<Item> gems = new ArrayList<>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(ModItemTags.GEMS)) {
            if (holder.value() != Items.EMERALD) {
                gems.add(holder.value());
            }
        }
        if (gems.isEmpty()) return;
        Random random = new Random(level.getSeed());
        Item preferredGem = gems.get(random.nextInt(gems.size()));
        AABB searchBox = new AABB(pos, pos).inflate(1.0);
        List<ItemEntity> itemEntities = level.getEntitiesOfClass(ItemEntity.class, searchBox);
        boolean found = false;
        for (ItemEntity itemEntity : itemEntities) {
            if (itemEntity.getItem().is(preferredGem)) {
                found = true;
                break;
            }
        }
        if (found) {
            int count = context.getInt(result.count());
            ItemStack stackToDrop = result.stack().copyWithCount(count);
            AnvilUtil.dropItems(List.of(stackToDrop), level, pos);
        }
    }

    public static class Type implements IRecipeOutcome.Type<RoyalPreferenceOutcome> {
        public static final MapCodec<RoyalPreferenceOutcome> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ChanceItemStack.CODEC.fieldOf("result").forGetter(RoyalPreferenceOutcome::result)
        ).apply(instance, RoyalPreferenceOutcome::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, RoyalPreferenceOutcome> STREAM_CODEC = StreamCodec.composite(
            ChanceItemStack.STREAM_CODEC,
            RoyalPreferenceOutcome::result,
            RoyalPreferenceOutcome::new
        );

        @Override
        public MapCodec<RoyalPreferenceOutcome> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RoyalPreferenceOutcome> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
