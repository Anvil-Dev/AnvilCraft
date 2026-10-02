package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.client.renderer.CreativeCrateRenderUtil;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class CreativeCrateItemRenderer extends BaseBlockItemRenderer<ItemStackRenderState> {
    private static final ThreadLocal<Set<ItemStack>> EXTRACTING = ThreadLocal.withInitial(
        () -> Collections.newSetFromMap(new IdentityHashMap<>()));

    public CreativeCrateItemRenderer() {
        super(ModBlocks.CREATIVE_CRATE.getDefaultState(), 0, 1);
    }

    @Override
    public @Nullable ItemStackRenderState extractArgument(ItemStack stack) {
        var stored = stack.get(ModComponents.DISPLAY_ITEM);
        if (stored == null || stored.stored().isEmpty()) return null;
        Set<ItemStack> extracting = EXTRACTING.get();
        if (!extracting.add(stack)) return null;
        try {
            var client = Minecraft.getInstance();
            var state = new ItemStackRenderState();
            client.getItemModelResolver().updateForTopItem(state, stored.stored(), ItemDisplayContext.FIXED, client.level, null, 0);
            return state;
        } finally {
            extracting.remove(stack);
            if (extracting.isEmpty()) EXTRACTING.remove();
        }
    }

    @Override
    public void submit(
        @Nullable ItemStackRenderState item, PoseStack pose, SubmitNodeCollector collector,
        int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor
    ) {
        this.submitShell(pose, collector, lightCoords, overlayCoords, hasFoil, outlineColor);
        if (item != null) CreativeCrateRenderUtil.submit(item, pose, collector, lightCoords, overlayCoords, outlineColor);
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<ItemStackRenderState> {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public CreativeCrateItemRenderer bake(BakingContext context) {
            return new CreativeCrateItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }
    }
}
