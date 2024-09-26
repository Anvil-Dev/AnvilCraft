package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.api.tooltip.providers.HandHeldItemTooltipProvider;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.StructureToolMenu;
import dev.dubhe.anvilcraft.network.StructureDataSyncPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import org.jetbrains.annotations.Contract;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class StructureToolItem extends Item implements HandHeldItemTooltipProvider {
    public StructureToolItem(Properties properties) {
        super(properties);
    }

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
                if (data.getSizeX() != data.getSizeY()
                        || data.getSizeX() != data.getSizeZ()
                        || data.getSizeY() != data.getSizeZ()) {
                    player.displayClientMessage(
                            Component.translatable("tooltip.anvilcraft.item.structure_tool.must_cube")
                                    .withStyle(ChatFormatting.RED),
                            false);
                    return InteractionResultHolder.fail(itemstack);
                }
                if (data.getSizeX() % 2 != 1 || data.getSizeX() > 15) {
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
                    "tooltip.anvilcraft.item.structure_tool.min_pos", data.minX, data.minY, data.minZ));
            tooltipComponents.add(Component.translatable(
                    "tooltip.anvilcraft.item.structure_tool.max_pos", data.maxX, data.maxY, data.maxZ));
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
            BlockPos minPos = data.getMinPos();
            BlockPos maxPos = data.getMaxPos();
            VoxelShape shape = Shapes.create(AABB.encapsulatingFullBlocks(minPos, maxPos));
            TooltipRenderHelper.renderOutline(poseStack, consumer, camX, camY, camZ, BlockPos.ZERO, shape, 0xFFFFFFFF);
        }
    }

    @Override
    public void renderTooltip(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {}

    @Override
    public int priority() {
        return 0;
    }

    @Getter
    public static class StructureData {
        public static final Codec<StructureData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                        Codec.INT.fieldOf("minX").forGetter(StructureData::getMinX),
                        Codec.INT.fieldOf("minY").forGetter(StructureData::getMinY),
                        Codec.INT.fieldOf("minZ").forGetter(StructureData::getMinZ),
                        Codec.INT.fieldOf("maxX").forGetter(StructureData::getMaxX),
                        Codec.INT.fieldOf("maxY").forGetter(StructureData::getMaxY),
                        Codec.INT.fieldOf("maxZ").forGetter(StructureData::getMaxZ))
                .apply(ins, StructureData::new));

        public static final StreamCodec<ByteBuf, StructureData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                StructureData::getMinX,
                ByteBufCodecs.VAR_INT,
                StructureData::getMinY,
                ByteBufCodecs.VAR_INT,
                StructureData::getMinZ,
                ByteBufCodecs.VAR_INT,
                StructureData::getMaxX,
                ByteBufCodecs.VAR_INT,
                StructureData::getMaxY,
                ByteBufCodecs.VAR_INT,
                StructureData::getMaxZ,
                StructureData::new);

        final int minX, minY, minZ;
        final int maxX, maxY, maxZ;

        public StructureData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        public StructureData(BlockPos initPos) {
            minX = initPos.getX();
            minY = initPos.getY();
            minZ = initPos.getZ();
            maxX = initPos.getX();
            maxY = initPos.getY();
            maxZ = initPos.getZ();
        }

        @Contract(" _ -> new")
        public StructureData addPos(BlockPos pos) {
            int newMinX, newMinY, newMinZ;
            int newMaxX, newMaxY, newMaxZ;
            newMinX = Math.min(minX, pos.getX());
            newMinY = Math.min(minY, pos.getY());
            newMinZ = Math.min(minZ, pos.getZ());
            newMaxX = Math.max(maxX, pos.getX());
            newMaxY = Math.max(maxY, pos.getY());
            newMaxZ = Math.max(maxZ, pos.getZ());
            return new StructureData(newMinX, newMinY, newMinZ, newMaxX, newMaxY, newMaxZ);
        }

        public BlockPos getMinPos() {
            return new BlockPos(minX, minY, minZ);
        }

        public BlockPos getMaxPos() {
            return new BlockPos(maxX, maxY, maxZ);
        }

        public int getSizeX() {
            return maxX - minX + 1;
        }

        public int getSizeY() {
            return maxY - minY + 1;
        }

        public int getSizeZ() {
            return maxZ - minZ + 1;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = Integer.hashCode(minX);
            result = prime * result + Integer.hashCode(minY);
            result = prime * result + Integer.hashCode(minZ);
            result = prime * result + Integer.hashCode(maxX);
            result = prime * result + Integer.hashCode(maxY);
            result = prime * result + Integer.hashCode(maxZ);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj instanceof StructureData data) {
                return minX == data.minX
                        && minY == data.minY
                        && minZ == data.minZ
                        && maxX == data.maxX
                        && maxY == data.maxY
                        && maxZ == data.maxZ;
            }
            return false;
        }
    }
}
