package dev.dubhe.anvilcraft.init;

import com.mojang.datafixers.types.Type;
import dev.dubhe.anvilcraft.block.entity.AutoCrafterBlockEntity;
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
    public static final BlockEntityType<AutoCrafterBlockEntity> AUTO_CRAFTER = ModBlockEntities.register("crefting_machine", BlockEntityType.Builder.of(AutoCrafterBlockEntity::new, ModBlocks.AUTO_CRAFTER));

    public static void register() {}

    private static <T extends BlockEntity> @NotNull BlockEntityType<T> register(String key, @NotNull BlockEntityType.Builder<T> builder) {
        Type<?> type = Util.fetchChoiceType(References.BLOCK_ENTITY, key);
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key, builder.build(type));
    }
}
