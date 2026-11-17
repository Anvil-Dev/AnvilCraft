package dev.dubhe.anvilcraft.block.cfa.item;

import dev.anvilcraft.lib.v2.registrum.util.CreativeVariantPickerItem;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyPresets;
import dev.dubhe.anvilcraft.block.item.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.client.init.ModCreativeVariantGroups;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class CelestialForgingAnvilBlockItem extends SimpleMultiPartBlockItem<Cube323PartHalf>
    implements Equipable, CreativeVariantPickerItem {
    public static final String ITEM_RENDER_DATA = "itemRenderData";
    private final ItemAttributeModifiers armorModifiers;

    public CelestialForgingAnvilBlockItem(
        SimpleMultiPartBlock<Cube323PartHalf> block, Properties properties
    ) {
        super(block, properties);
        var modifierId = AnvilCraft.of("celestial_forging_anvil_helmet");
        this.armorModifiers = ItemAttributeModifiers.builder()
            .add(Attributes.ARMOR,
                new AttributeModifier(modifierId, 5, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.HEAD)
            .add(Attributes.ARMOR_TOUGHNESS,
                new AttributeModifier(modifierId, 4, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.HEAD)
            .add(Attributes.KNOCKBACK_RESISTANCE,
                new AttributeModifier(modifierId, 0.1, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.HEAD)
            .build();
    }

    @Override
    public List<ItemStack> createCreativePickerVariants(ItemStack source) {
        if (FMLLoader.getDist() != Dist.CLIENT) {
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

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return ArmorMaterials.NETHERITE.value().equipSound();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return this.swapWithEquipmentSlot(this, level, player, hand);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return this.armorModifiers;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ArmorMaterials.NETHERITE.value().enchantmentValue();
    }

    public static void saveRenderData(CompoundTag blockEntityTag, long gameTime) {
        CompoundTag renderData = new CompoundTag();
        for (String key : blockEntityTag.getAllKeys()) {
            if (key.startsWith("activeMegastructure") || key.startsWith("stellar") || key.startsWith("accelerator")
                || key.equals("excavatorLaserActive") || key.equals("penroseSphereLaserActive")) {
                var value = blockEntityTag.get(key);
                if (value != null) renderData.put(key, value.copy());
            }
        }
        if (renderData.getLong("acceleratorPausedSinceGameTime") < 0
            || !renderData.contains("acceleratorPausedSinceGameTime")) {
            renderData.putLong("acceleratorPausedSinceGameTime", gameTime);
        }
        blockEntityTag.put(ITEM_RENDER_DATA, renderData);
    }
}
