package dev.dubhe.anvilcraft.init;

import com.mojang.datafixers.types.Type;
import dev.dubhe.anvilcraft.block.entity.CraftingMachineBlockEntity;
import dev.dubhe.anvilcraft.block.entity.InteractMachineBlockEntity;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;

public class ModBlockEntities {
    public static final BlockEntityType<InteractMachineBlockEntity> INTERACT_MACHINE = ModBlockEntities.register("interact_machine", BlockEntityType.Builder.of(InteractMachineBlockEntity::new, ModBlocks.INTERACT_MACHINE));
    public static final BlockEntityType<CraftingMachineBlockEntity> CRAFTING_MACHINE = ModBlockEntities.register("crefting_machine", BlockEntityType.Builder.of(CraftingMachineBlockEntity::new, ModBlocks.CRAFTING_MACHINE));

    public static void register() {}

    private static <T extends BlockEntity> @NotNull BlockEntityType<T> register(String key, @NotNull BlockEntityType.Builder<T> builder) {
        Type<?> type = Util.fetchChoiceType(References.BLOCK_ENTITY, key);
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key, builder.build(type));
    }
}
