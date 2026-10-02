package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BuildingPlan {
    private BuildingPlan() {
    }

    public record Cell(BlockPos pos, BlockState state, CompoundTag config, List<BlockEntityContentAdapter.SlotStack> contents)
        implements BuildingCommit.PlacedCell {
    }

    static final class Group {
        final List<Item> tools = new ArrayList<>();
        final List<BuildingMaterials.FluidPayment> fluidPayments = new ArrayList<>();
        int ignitions;
        int leads;
        boolean hammer;
        @Nullable EntityType<?> creature;
        final List<Cell> cells = new ArrayList<>();
        final List<ItemStack> materials = new ArrayList<>();
        final List<FluidStack> fluids = new ArrayList<>();
        final List<EntityBuildAdapter.Planned> entities = new ArrayList<>();
        final Map<BlockPos, ItemStack> blockMaterials = new LinkedHashMap<>();
        final List<ItemStack> returned = new ArrayList<>();
        boolean separateContents;
        boolean componentMismatch;
        boolean missingContents;
    }
}
