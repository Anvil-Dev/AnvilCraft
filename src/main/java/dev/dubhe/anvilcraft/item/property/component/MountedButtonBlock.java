package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.block.state.ButtonCap;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

/** 方块状态用于展示和调节，原物品的组件用于还原装饰及拆下后的掉落物。 */
public record MountedButtonBlock(BlockState state, ItemStackTemplate material, ChromaticButtonMode mode) {
    public static final Codec<MountedButtonBlock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BlockState.CODEC.fieldOf("state").forGetter(MountedButtonBlock::state),
        ItemStackTemplate.CODEC.fieldOf("material").forGetter(MountedButtonBlock::material),
        ChromaticButtonMode.CODEC.optionalFieldOf("mode", ChromaticButtonMode.FLOWING).forGetter(MountedButtonBlock::mode)
    ).apply(instance, MountedButtonBlock::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, MountedButtonBlock> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), MountedButtonBlock::state,
        ItemStackTemplate.STREAM_CODEC, MountedButtonBlock::material,
        ChromaticButtonMode.STREAM_CODEC, MountedButtonBlock::mode, MountedButtonBlock::new
    );

    public MountedButtonBlock {
        material = material.withCount(1);
    }

    public MountedButtonBlock(BlockState state, ItemStackTemplate material) {
        this(state, material, ChromaticButtonMode.FLOWING);
    }

    public MountedButtonBlock withMode(ChromaticButtonMode mode) {
        return new MountedButtonBlock(this.state, this.material, mode);
    }

    public static @Nullable MountedButtonBlock get(DataComponentGetter components) {
        MountedButtonBlock mounted = components.get(ModComponents.MOUNTED_BLOCK);
        if (mounted != null) {
            return mounted;
        }
        ButtonCap cap = components.get(ModComponents.BUTTON_CAP);
        return cap == null || cap == ButtonCap.BASE || cap.isHammerCap() ? null : Presets.BY_CAP.get(cap);
    }

    public @Nullable ButtonCap namedCap() {
        return Presets.BY_BLOCK.get(this.state.getBlock());
    }

    public boolean canAdjustWithHammer() {
        return this.state.is(ModBlocks.CHROMATIC_STONE)
            || AnvilHammerItem.canAdjustState(this.state) && AnvilHammerItem.findModifyableProperty(this.state) != null;
    }

    public boolean canAdjustWithHammer(Player player) {
        return player.mayBuild() && player.isHolding(stack -> stack.is(ModItemTags.ANVIL_HAMMER)) && this.canAdjustWithHammer();
    }

    public MountedButtonBlock withState(BlockState state) {
        return new MountedButtonBlock(state, this.material, this.mode());
    }

    private static final class Presets {
        private static final Map<ButtonCap, MountedButtonBlock> BY_CAP = new EnumMap<>(ButtonCap.class);
        private static final Map<Block, ButtonCap> BY_BLOCK = new IdentityHashMap<>();

        static {
            for (ButtonCap cap : ButtonCap.values()) {
                Block block = cap.mountedBlock();
                if (block != null) {
                    BY_CAP.put(cap, new MountedButtonBlock(block.defaultBlockState(), new ItemStackTemplate(block.asItem())));
                    BY_BLOCK.put(block, cap);
                }
            }
        }
    }
}
