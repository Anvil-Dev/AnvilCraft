package dev.dubhe.anvilcraft.integration.emi.stack;

import com.google.common.collect.Lists;
import dev.dubhe.anvilcraft.util.BlockStateRender;
import dev.emi.emi.EmiPort;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BlockStateEmiStack extends EmiStack {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private final BlockState state;

    public BlockStateEmiStack(BlockState state) {
        this.state = state;
    }

    @Override
    public EmiStack copy() {
        return new BlockStateEmiStack(this.state);
    }

    @Override
    public ItemStack getItemStack() {
        return this.state.getBlock().asItem().getDefaultInstance();
    }

    @Override
    public void render(@NotNull GuiGraphics draw, int x, int y, float delta, int flags) {
        BlockStateRender.render(this.state, draw, x, y);
    }

    @Override
    public boolean isEmpty() {
        return this.state.isAir();
    }

    @Override
    public CompoundTag getNbt() {
        return new CompoundTag();
    }

    @Override
    public Object getKey() {
        return this.state.getBlock();
    }

    @Override
    public ResourceLocation getId() {
        return EmiPort.getBlockRegistry().getKey(this.state.getBlock());
    }

    @Override
    public List<Component> getTooltipText() {
        return BlockStateEmiStack.getTooltipLines(this.state);
    }

    private static @NotNull List<Component> getTooltipLines(@NotNull BlockState state) {
        ArrayList<Component> list = Lists.newArrayList();
        ChatFormatting color = state.getBlock().asItem().getDefaultInstance().getRarity().color;
        Component name = Component.translatable(state.getBlock().getDescriptionId());
        list.add(Component.empty().append(name).withStyle(color));
        Component id = Component
            .literal(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())
            .withStyle(ChatFormatting.DARK_GRAY);
        list.add(id);
        return list;
    }

    @Override
    public List<ClientTooltipComponent> getTooltip() {
        return BlockStateEmiStack.getTooltipLines(this.state)
            .stream().map(EmiPort::ordered).map(ClientTooltipComponent::create).collect(Collectors.toList());
    }

    @Override
    public Component getName() {
        if (isEmpty()) {
            return EmiPort.literal("");
        }
        return EmiPort.translatable(this.state.getBlock().getDescriptionId());
    }
}
