package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 仓储合成面板的持久数据：① 切石机输入（单槽）、② 合成输入（9 宫格）、切石机配方选择，
 * 以及上次关闭界面时是否为合成模式（{@code lastOpened}）。
 *
 * <p>世界内打开时存于 {@link BaseStorage} 的 crafting 字段，终端打开时存于终端物品的
 * {@code anvilcraft:crafting} 数据组件。</p>
 */
public record CraftingStorage(
    ItemStack stonecutterInput,
    List<ItemStack> craftingInput,
    int stonecutterSelected,
    boolean lastOpened,
    boolean autoFill,
    boolean toStorage
) {
    public static final int CRAFTING_GRID_SIZE = 9;
    public static final CraftingStorage EMPTY = new CraftingStorage(
        ItemStack.EMPTY,
        CraftingStorage.emptyGrid(),
        0,
        false,
        false,
        false
    );
    public static final MapCodec<CraftingStorage> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        ItemStack.OPTIONAL_CODEC.fieldOf("stonecutter_input").forGetter(CraftingStorage::stonecutterInput),
        ItemStack.OPTIONAL_CODEC.listOf().fieldOf("crafting_input").forGetter(CraftingStorage::craftingInput),
        Codec.INT.optionalFieldOf("stonecutter_selected", 0).forGetter(CraftingStorage::stonecutterSelected),
        Codec.BOOL.optionalFieldOf("last_opened", false).forGetter(CraftingStorage::lastOpened),
        Codec.BOOL.optionalFieldOf("auto_fill", false).forGetter(CraftingStorage::autoFill),
        Codec.BOOL.optionalFieldOf("to_storage", false).forGetter(CraftingStorage::toStorage)
    ).apply(ins, CraftingStorage::decode));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingStorage> STREAM_CODEC = StreamCodec.composite(
        ItemStack.OPTIONAL_STREAM_CODEC,
        CraftingStorage::stonecutterInput,
        ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
        CraftingStorage::craftingInput,
        ByteBufCodecs.VAR_INT,
        CraftingStorage::stonecutterSelected,
        ByteBufCodecs.BOOL,
        CraftingStorage::lastOpened,
        ByteBufCodecs.BOOL,
        CraftingStorage::autoFill,
        ByteBufCodecs.BOOL,
        CraftingStorage::toStorage,
        CraftingStorage::decode
    );

    private static CraftingStorage decode(
        ItemStack stonecutterInput,
        List<ItemStack> craftingInput,
        int stonecutterSelected,
        boolean lastOpened,
        boolean autoFill,
        boolean toStorage
    ) {
        return new CraftingStorage(
            stonecutterInput,
            CraftingStorage.normalizeGrid(craftingInput),
            stonecutterSelected,
            lastOpened,
            autoFill,
            toStorage
        );
    }

    /** 补齐 / 截断为固定 9 格合成输入。 */
    private static List<ItemStack> normalizeGrid(List<ItemStack> input) {
        List<ItemStack> grid = new ArrayList<>(input);
        while (grid.size() < CraftingStorage.CRAFTING_GRID_SIZE) {
            grid.add(ItemStack.EMPTY);
        }
        return List.copyOf(grid.subList(0, CraftingStorage.CRAFTING_GRID_SIZE));
    }

    private static List<ItemStack> emptyGrid() {
        return Collections.nCopies(CraftingStorage.CRAFTING_GRID_SIZE, ItemStack.EMPTY);
    }

    /** ① 切石机输入。 */
    public CraftingStorage withStonecutterInput(ItemStack input) {
        return new CraftingStorage(input, this.craftingInput, this.stonecutterSelected, this.lastOpened, this.autoFill, this.toStorage);
    }

    /** ② 合成 9 宫格（整体替换）。 */
    public CraftingStorage withCraftingInput(List<ItemStack> input) {
        return new CraftingStorage(this.stonecutterInput, input, this.stonecutterSelected, this.lastOpened, this.autoFill, this.toStorage);
    }

    /** ② 指定槽位替换。 */
    public CraftingStorage withCraftingSlot(int slot, ItemStack stack) {
        List<ItemStack> grid = new ArrayList<>(this.craftingInput);
        grid.set(slot, stack);
        return new CraftingStorage(this.stonecutterInput, grid, this.stonecutterSelected, this.lastOpened, this.autoFill, this.toStorage);
    }

    /** 切石机选中配方索引。 */
    public CraftingStorage withStonecutterSelected(int selected) {
        return new CraftingStorage(this.stonecutterInput, this.craftingInput, selected, this.lastOpened, this.autoFill, this.toStorage);
    }

    /** 上次关闭界面时是否为合成模式。 */
    public CraftingStorage withLastOpened(boolean opened) {
        return new CraftingStorage(
            this.stonecutterInput,
            this.craftingInput,
            this.stonecutterSelected,
            opened,
            this.autoFill,
            this.toStorage
        );
    }

    /** Auto-refill flag. */
    public CraftingStorage withAutoFill(boolean autoFill) {
        return new CraftingStorage(
            this.stonecutterInput,
            this.craftingInput,
            this.stonecutterSelected,
            this.lastOpened,
            autoFill,
            this.toStorage
        );
    }

    /** Shift-craft destination flag. */
    public CraftingStorage withToStorage(boolean toStorage) {
        return new CraftingStorage(
            this.stonecutterInput,
            this.craftingInput,
            this.stonecutterSelected,
            this.lastOpened,
            this.autoFill,
            toStorage
        );
    }
}
