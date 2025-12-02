package dev.dubhe.anvilcraft.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHandHeldItemTooltipProvider;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.inventory.StructureToolMenu;
import dev.dubhe.anvilcraft.item.property.component.StructureData;
import dev.dubhe.anvilcraft.network.StructureDataSyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class StructureToolItem extends Item implements IHandHeldItemTooltipProvider {
    public StructureToolItem(Properties properties) {
        super(properties);
    }

    private static final Component DEVELOPER_TOOLTIP =
        Component.translatable("tooltip.anvilcraft.item.structure_tool.line_1").withStyle(ChatFormatting.LIGHT_PURPLE);
    private static final Component SELECT_TOOLTIP =
        Component.translatable("tooltip.anvilcraft.item.structure_tool.line_2").withStyle(ChatFormatting.GOLD);
    private static final Component INPUT_TOOLTIP =
        Component.translatable("tooltip.anvilcraft.item.structure_tool.line_3").withStyle(ChatFormatting.GOLD);
    private static final Component SHIFT_TO_CLEAR_TOOLTIP =
        Component.translatable("tooltip.anvilcraft.item.structure_tool.shift_to_clear");

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack itemstack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        StructureData data;
        Player player = context.getPlayer();
        if (itemstack.has(ModComponents.STRUCTURE_DATA)) {
            data = itemstack.get(ModComponents.STRUCTURE_DATA);
        } else {
            data = new StructureData(pos);
        }
        if (data != null) {
            StructureData newData = data.addPos(pos);
            if (player != null) {
                player.displayClientMessage(
                    Component.translatable(
                        "tooltip.anvilcraft.item.structure_tool.size",
                        newData.getSizeX(),
                        newData.getSizeY(),
                        newData.getSizeZ()),
                    true);
            }
            itemstack.set(ModComponents.STRUCTURE_DATA, newData);
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemstack = player.getItemInHand(usedHand);
        if (player.isShiftKeyDown()) {
            if (itemstack.has(ModComponents.STRUCTURE_DATA)) {
                itemstack.remove(ModComponents.STRUCTURE_DATA);
                player.displayClientMessage(
                    Component.translatable("tooltip.anvilcraft.item.structure_tool.data_removed"), true);
                return InteractionResultHolder.success(itemstack);
            }
        } else {
            StructureData data = itemstack.get(ModComponents.STRUCTURE_DATA);
            if (data != null && !level.isClientSide) {
                if (!data.isCube()) {
                    player.displayClientMessage(
                        Component.translatable("tooltip.anvilcraft.item.structure_tool.must_cube")
                            .withStyle(ChatFormatting.RED),
                        false);
                    return InteractionResultHolder.fail(itemstack);
                }
                if (!data.isOddCubeWithinSize(15)) {
                    player.displayClientMessage(
                        Component.translatable("tooltip.anvilcraft.item.structure_tool.must_odd")
                            .withStyle(ChatFormatting.RED),
                        false);
                    return InteractionResultHolder.fail(itemstack);
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    ModMenuTypes.open(
                        serverPlayer,
                        new SimpleMenuProvider(
                            (invId, inv, p) ->
                                new StructureToolMenu(ModMenuTypes.STRUCTURE_TOOL.get(), invId, inv),
                            getDescription()));
                    PacketDistributor.sendToPlayer(serverPlayer, new StructureDataSyncPacket(data));
                }
                return InteractionResultHolder.success(itemstack);
            }
        }
        return super.use(level, player, usedHand);
    }

    @Override
    public void appendHoverText(
        ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        StructureData data = stack.get(ModComponents.STRUCTURE_DATA);
        if (data != null) {
            tooltipComponents.add(Component.translatable(
                "tooltip.anvilcraft.item.structure_tool.min_pos", data.minX(), data.minY(), data.minZ()));
            tooltipComponents.add(Component.translatable(
                "tooltip.anvilcraft.item.structure_tool.max_pos", data.maxX(), data.maxY(), data.maxZ()));
            tooltipComponents.add(SHIFT_TO_CLEAR_TOOLTIP);
        } else {
            tooltipComponents.add(DEVELOPER_TOOLTIP);
            tooltipComponents.add(SELECT_TOOLTIP);
            tooltipComponents.add(INPUT_TOOLTIP);
        }
    }

    @Override
    public boolean accepts(ItemStack itemStack) {
        return itemStack.is(this);
    }

    @Override
    public void render(
        PoseStack poseStack, VertexConsumer consumer, ItemStack itemStack, double camX, double camY, double camZ) {
        StructureData data = itemStack.get(ModComponents.STRUCTURE_DATA);
        if (data != null) {
            BlockPos minPos = data.minPos();
            BlockPos maxPos = data.maxPos();
            VoxelShape shape = Shapes.create(AABB.encapsulatingFullBlocks(minPos, maxPos));
            TooltipRenderHelper.renderOutline(poseStack, consumer, camX, camY, camZ, BlockPos.ZERO, shape, 0xFFFFFFFF);
        }
    }

    @Override
    public void renderTooltip(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
    }

    @Override
    public int priority() {
        return 0;
    }
}
