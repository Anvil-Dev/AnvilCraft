package dev.dubhe.anvilcraft.client.selection;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.neoforged.neoforge.client.model.NeoForgeModelProperties;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** 暂存烘焙来源，构造完选择几何即释放；不消耗 AnvilLib 自己的捕获缓存。 */
public final class ModelSelectionCapture {
    private static final Map<BlockStateModelPart, Source> SOURCES = new IdentityHashMap<>();

    private ModelSelectionCapture() {
    }

    public static synchronized void remember(ResolvedModel model, ModelState state, BlockStateModelPart baked) {
        if (!(baked instanceof SimpleModelWrapper)) return;
        List<CuboidModelElement> elements;
        if (model.getTopGeometry() instanceof UnbakedCuboidGeometry geometry) {
            elements = geometry.elements();
        } else if (model.getTopGeometry() == UnbakedGeometry.EMPTY) {
            elements = List.of();
        } else {
            return;
        }
        var root = model.getTopAdditionalProperties().getOptional(NeoForgeModelProperties.TRANSFORM);
        ModelState transformed = root == null ? state : UnbakedElementsHelper.composeRootTransformIntoModelState(state, root);
        SOURCES.put(baked, new Source(elements, transformed));
    }

    static synchronized Map<BlockStateModelPart, Source> take() {
        Map<BlockStateModelPart, Source> sources = new IdentityHashMap<>(SOURCES);
        SOURCES.clear();
        return sources;
    }

    record Source(List<CuboidModelElement> elements, ModelState state) {
    }
}
