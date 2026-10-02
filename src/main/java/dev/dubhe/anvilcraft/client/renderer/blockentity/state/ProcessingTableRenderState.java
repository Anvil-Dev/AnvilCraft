package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import dev.dubhe.anvilcraft.client.selection.SelectionModel;
import dev.dubhe.anvilcraft.client.support.ProcessingModelShape;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessingTableRenderState extends BlockEntityRenderState {
    public float progress;
    public int itemCount;
    public final List<DisplayedItem> items = new ArrayList<>();
    public final Map<SelectionModel, BlockModelRenderState> models = new HashMap<>();

    public static class DisplayedItem {
        public final ItemModelState item = new ItemModelState();
        public final List<Matrix4f> poses = new ArrayList<>();
        public int poseCount;
        public boolean blockModel;

        public void addPose(Matrix4f pose) {
            if (this.poseCount == this.poses.size()) this.poses.add(new Matrix4f());
            this.poses.get(this.poseCount++).set(pose);
        }
    }

    public static class ItemModelState extends ItemStackRenderState {
        private boolean knownGeometry;
        private boolean threeDimensional;

        @Override
        public void clear() {
            super.clear();
            this.knownGeometry = false;
            this.threeDimensional = false;
        }

        @Override
        public void appendModelIdentityElement(Object element) {
            if (!this.knownGeometry && element instanceof ProcessingModelShape shape) {
                this.knownGeometry = true;
                this.threeDimensional = shape.anvilcraft$isThreeDimensional();
            }
        }

        public boolean isThreeDimensional() {
            return this.knownGeometry ? this.threeDimensional : this.usesBlockLight();
        }
    }
}
