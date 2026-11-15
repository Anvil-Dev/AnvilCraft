package dev.dubhe.anvilcraft.block.cfa.item;

import dev.anvilcraft.lib.v2.registrum.util.CreativeVariantPickerItem;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyPresets;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.client.init.ModCreativeVariantGroups;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.block.SimpleMultiPartBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class CelestialForgingAnvilBlockItem extends SimpleMultiPartBlockItem<Cube323PartHalf>
    implements CreativeVariantPickerItem {
    public static final String ITEM_RENDER_DATA = "itemRenderData";

    public CelestialForgingAnvilBlockItem(SimpleMultiPartBlock<Cube323PartHalf> block, Properties properties) {
        super(block, equipment(properties));
    }

    @Override
    public List<ItemStack> createCreativePickerVariants(ItemStack source) {
        if (FMLLoader.getCurrent().getDist() != Dist.CLIENT) {
            return List.of();
        }
        return ModCreativeVariantGroups.celestialPresets();
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        String preset = CelestialBodyPresets.preset(context.getItemInHand());
        if (preset != null && CelestialBodyPresets.preview(preset, context.getLevel()) == null) {
            return false;
        }
        return super.placeBlock(context, state);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(
        BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state
    ) {
        boolean updated = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        String preset = CelestialBodyPresets.preset(stack);
        BlockPos mainPos = ModBlocks.CELESTIAL_FORGING_ANVIL.get().getMainPartPos(pos, state);
        if (preset != null && level instanceof ServerLevel serverLevel
            && level.getBlockEntity(mainPos) instanceof CelestialForgingAnvilBlockEntity anvil) {
            return CelestialBodyPresets.apply(preset, anvil, serverLevel);
        }
        return updated;
    }

    private static Properties equipment(Properties properties) {
        var id = AnvilCraft.of("celestial_forging_anvil_helmet");
        return properties.enchantable(ArmorMaterials.NETHERITE.enchantmentValue())
            .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
                .setEquipSound(ArmorMaterials.NETHERITE.equipSound()).build())
            .attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(id, 5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
                .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, 4, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HEAD)
                .add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(id, 0.1, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HEAD)
                .build());
    }

    public static void saveRenderData(CompoundTag blockEntityTag, long gameTime) {
        CompoundTag renderData = new CompoundTag();
        for (String key : blockEntityTag.keySet()) {
            if (key.startsWith("activeMegastructure") || key.startsWith("stellar") || key.startsWith("accelerator")
                || key.equals("excavatorLaserActive") || key.equals("penroseSphereLaserActive")) {
                var value = blockEntityTag.get(key);
                if (value != null) renderData.put(key, value.copy());
            }
        }
        if (renderData.getLongOr("acceleratorPausedSinceGameTime", -1) < 0) {
            renderData.putLong("acceleratorPausedSinceGameTime", gameTime);
        }
        blockEntityTag.put(ITEM_RENDER_DATA, renderData);
    }
}
