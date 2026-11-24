package dev.dubhe.anvilcraft.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PocketSlot extends Slot {
    private static final int HIDDEN_X = -10000;
    /**
     * 普通物品栏的 GUI 宽度与口袋面板顶位。创造模式界面尺寸不同（宽 195、顶位 40），
     * 由 {@code CreativePocketSlotMixin} 在这一基准上平移补偿，改动任意一侧都需同步另一侧。
     */
    public static final int SCREEN_IMAGE_WIDTH = 176;
    public static final int SCREEN_PANEL_TOP = 70;
    private final Player owner;
    private final int pocketIndex;
    private int cachedCapacity = -1;
    private PocketLayout leftLayout;
    private PocketLayout rightLayout;
    private boolean active;

    public PocketSlot(Player owner, int index) {
        super(new Access(owner), index, 0, 84 + index % 3 * 18);
        this.owner = owner;
        this.pocketIndex = index;
        this.updatePosition();
    }

    /**
     * 按当前容量重算槽位坐标。布局只在容量变化时重建，避免每帧重复分配记录。
     */
    public void updatePosition() {
        int capacity = PocketInventory.capacity(this.owner);
        if (this.cachedCapacity != capacity) {
            this.cachedCapacity = capacity;
            this.leftLayout = PocketLayout.of(capacity, 0, SCREEN_IMAGE_WIDTH, SCREEN_PANEL_TOP);
            this.rightLayout = PocketLayout.of(capacity, 1, SCREEN_IMAGE_WIDTH, SCREEN_PANEL_TOP);
        }
        this.active = this.pocketIndex < capacity;
        if (!this.active) {
            this.hide();
            return;
        }
        int perSide = (capacity + 1) / 2;
        int side = this.pocketIndex / perSide;
        int local = this.pocketIndex % perSide;
        PocketLayout layout = side == 0 ? this.leftLayout : this.rightLayout;
        if (layout == null || local >= layout.slots()) {
            this.active = false;
            this.hide();
            return;
        }
        this.x = layout.slotItemX(local);
        this.y = layout.slotItemY(local);
    }

    private void hide() {
        this.x = HIDDEN_X;
        this.y = 0;
    }

    @Override
    public boolean isActive() {
        int capacity = PocketInventory.capacity(this.owner);
        if (capacity != this.cachedCapacity) {
            this.updatePosition();
        }
        return this.active;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.isActive();
    }

    /**
     * 可视性由 {@code isActive} 与 {@code Access#stillValid} 双重约束，此处不再重复判定，
     * 避免在容器交互路径上多做一次玩家身份比较。
     */
    @Override
    public boolean mayPickup(Player player) {
        return this.isActive();
    }

    private record Access(Player owner) implements Container {
        @Override
        public int getContainerSize() {
            return PocketInventory.MAX_SIZE;
        }

        @Override
        public boolean isEmpty() {
            return PocketInventory.get(this.owner).isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return PocketInventory.get(this.owner).getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return PocketInventory.get(this.owner).removeItem(slot, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return PocketInventory.get(this.owner).removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            PocketInventory.get(this.owner).rememberLeggings(this.owner);
            PocketInventory.get(this.owner).setItem(slot, stack);
        }

        @Override
        public void setChanged() {
            PocketInventory.get(this.owner).setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return player == this.owner;
        }

        @Override
        public void clearContent() {
            PocketInventory.get(this.owner).clearContent();
        }
    }
}
