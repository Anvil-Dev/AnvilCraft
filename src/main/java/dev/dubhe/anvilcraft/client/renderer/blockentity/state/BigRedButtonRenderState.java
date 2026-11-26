package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import dev.dubhe.anvilcraft.client.renderer.blockentity.MountedBlockProjection;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.property.component.MountedButtonBlock;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

public class BigRedButtonRenderState extends BlockEntityRenderState {
    public @Nullable BlockModelRenderState model;
    public MountedBlockProjection.@Nullable Frame projection;
    public @Nullable MountedButtonBlock mounted;
    public float progress;
    public Direction facing = Direction.UP;
    public @Nullable StandaloneModelKey<BlockStateModel> capModel;

    public boolean squashes() {
        return this.mounted != null && !this.mounted.state().is(ModBlocks.CHROMATIC_STONE);
    }
}
