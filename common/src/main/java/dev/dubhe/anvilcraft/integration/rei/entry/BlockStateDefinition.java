package dev.dubhe.anvilcraft.integration.rei.entry;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.rei.client.renderer.BlockStateEntryRenderer;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import me.shedaniel.rei.api.client.entry.renderer.EntryRenderer;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import me.shedaniel.rei.api.common.entry.EntrySerializer;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.comparison.ComparisonContext;
import me.shedaniel.rei.api.common.entry.type.EntryDefinition;
import me.shedaniel.rei.api.common.entry.type.EntryType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

public class BlockStateDefinition implements EntryDefinition<BlockState>, EntrySerializer<BlockState> {
    @Environment(EnvType.CLIENT)
    private static class Client {
        private static final EntryRenderer<BlockState> RENDERER = new BlockStateEntryRenderer();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public EntryRenderer<BlockState> getRenderer() {
        return Client.RENDERER;
    }

    private static final ReferenceSet<Block> SEARCH_BLACKLISTED = new ReferenceOpenHashSet<>();

    @Override
    public Class<BlockState> getValueType() {
        return BlockState.class;
    }

    @Override
    public EntryType<BlockState> getType() {
        return EntryTypes.BLOCK;
    }

    @Override
    public @Nullable ResourceLocation getIdentifier(EntryStack<BlockState> entry, @NotNull BlockState value) {
        return BuiltInRegistries.BLOCK.getKey(value.getBlock());
    }

    @Override
    public boolean isEmpty(EntryStack<BlockState> entry, @NotNull BlockState value) {
        return value.isAir();
    }

    @Override
    public BlockState copy(EntryStack<BlockState> entry, BlockState value) {
        return value;
    }

    @Override
    public BlockState normalize(EntryStack<BlockState> entry, @NotNull BlockState value) {
        return value.getBlock().defaultBlockState();
    }

    @Override
    public BlockState wildcard(EntryStack<BlockState> entry, @NotNull BlockState value) {
        return value.getBlock().defaultBlockState();
    }

    @Override
    public long hash(EntryStack<BlockState> entry, @NotNull BlockState value, ComparisonContext context) {
        int code = 1;
        code = 31 * code + System.identityHashCode(value.getBlock());
        return code;
    }

    @Override
    public boolean equals(@NotNull BlockState o1, @NotNull BlockState o2, ComparisonContext context) {
        if (o1.getBlock() != o2.getBlock()) return false;
        return o1.equals(o2);
    }

    @Override
    public @Nullable EntrySerializer<BlockState> getSerializer() {
        return this;
    }

    @Override
    public Component asFormattedText(EntryStack<BlockState> entry, BlockState value) {
        return asFormattedText(entry, value, TooltipContext.of());
    }

    @Override
    public Component asFormattedText(EntryStack<BlockState> entry, @NotNull BlockState value, TooltipContext context) {
        if (!SEARCH_BLACKLISTED.contains(value.getBlock()))
            try {
                return Component.literal(value.getBlock().getDescriptionId());
            } catch (Throwable e) {
                if (context != null && context.isSearch()) throw e;
                AnvilCraft.LOGGER.error(e.getMessage(), e);
                SEARCH_BLACKLISTED.add(value.getBlock());
            }
        try {
            return Component.literal(
                I18n.get(
                    "item."
                        + BuiltInRegistries.BLOCK
                        .getKey(value.getBlock())
                        .toString()
                        .replace(":", ".")
                )
            );
        } catch (Throwable e) {
            AnvilCraft.LOGGER.error(e.getMessage(), e);
        }
        return Component.literal("ERROR");
    }

    @Override
    public Stream<? extends TagKey<?>> getTagsFor(EntryStack<BlockState> entry, @NotNull BlockState value) {
        return value.getTags();
    }

    @Override
    public boolean supportSaving() {
        return true;
    }

    @Override
    public boolean supportReading() {
        return true;
    }

    @Override
    public CompoundTag save(EntryStack<BlockState> entry, @NotNull BlockState value) {
        CompoundTag tag = new CompoundTag();
        tag.putString("block", BuiltInRegistries.BLOCK.getKey(value.getBlock()).toString());
        CompoundTag data = new CompoundTag();
        for (Property<?> property : value.getProperties()) {
            Comparable<?> value1 = value.getValue(property);
            data.putString(property.getName(), value1.toString());
        }
        tag.put("states", data);
        return tag;
    }

    @Override
    public BlockState read(@NotNull CompoundTag tag) {
        return this.getBlockState(tag);
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>, V extends T> @NotNull BlockState getBlockState(@NotNull CompoundTag tag) {
        BlockState state = BuiltInRegistries.BLOCK
            .get(new ResourceLocation(tag.getString("block")))
            .defaultBlockState();
        CompoundTag data = tag.getCompound("states");
        for (Property<?> property : state.getProperties()) {
            String string = data.getString(property.getName());
            if (string.isEmpty()) continue;
            Optional<? extends Property.Value<?>> first = property.getAllValues()
                .filter(value -> value.value().toString().equals(string))
                .findFirst();
            if (first.isEmpty()) continue;
            state.setValue((Property<T>) property, (V) first.get().value());
        }
        return state;
    }
}
