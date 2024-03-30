package dev.dubhe.anvilcraft.init;

import com.mojang.datafixers.types.Type;
import dev.dubhe.anvilcraft.block.entity.AutoCrafterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ChuteBlockEntity;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;

public class ModBlockEntities {
    public static final BlockEntityType<AutoCrafterBlockEntity> AUTO_CRAFTER = ModBlockEntities.register("crafting_machine", BlockEntityType.Builder.of(AutoCrafterBlockEntity::new, ModBlocks.AUTO_CRAFTER));
    public static final BlockEntityType<ChuteBlockEntity> CHUTE = ModBlockEntities.register("chute", BlockEntityType.Builder.of(ChuteBlockEntity::new, ModBlocks.CHUTE));

    public static void register() {}

    private static <T extends BlockEntity> @NotNull BlockEntityType<T> register(String key, @NotNull BlockEntityType.Builder<T> builder) {
        Type<?> type = Util.fetchChoiceType(References.BLOCK_ENTITY, key);
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key, builder.build(type));
    }
}
