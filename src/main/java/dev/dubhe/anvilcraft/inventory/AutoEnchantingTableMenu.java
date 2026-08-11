package dev.dubhe.anvilcraft.inventory;

import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantments;
import dev.dubhe.anvilcraft.init.item.ModItems;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

@Getter
public class AutoEnchantingTableMenu extends AbstractContainerMenu {
    private final AutoEnchantingTableBlockEntity blockEntity;
    private final Container container;

    private final Level level;
    @Getter
    private List<Holder<Enchantment>> enchantmentList = new ObjectArrayList<>();
    @Getter
    private final IntSet selectedIndexes = new IntArraySet();

    final Slot prologueSlot;

    public AutoEnchantingTableMenu(
        @Nullable MenuType<?> menuType,
        int containerId,
        Inventory inventory,
        FriendlyByteBuf extraData
    ) {
        this(
            menuType,
            containerId,
            inventory,
            (AutoEnchantingTableBlockEntity) inventory.player.level().getBlockEntity(extraData.readBlockPos())
        );
    }

    public AutoEnchantingTableMenu(
        @Nullable MenuType<?> menuType,
        int containerId,
        Inventory inventory,
        AutoEnchantingTableBlockEntity blockEntity
    ) {
        super(menuType, containerId);
        this.level = inventory.player.level();
        this.blockEntity = blockEntity;
        this.container = blockEntity;
        this.blockEntity.setSlotChangedListener(this::slotsChanged);

        this.addPlayerInventory(inventory);
        this.addPlayerHotbar(inventory);

        this.addSlot(new Slot(this.container, 0, 7,  18) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public int getMaxStackSize(ItemStack itemStack) {
                return 1;
            }
        });
        this.addSlot(new Slot(this.container, 1, 7,  52) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public int getMaxStackSize(ItemStack itemStack) {
                return 1;
            }
        });
        this.prologueSlot = this.addSlot(new Slot(this.container, 2, 27,  18));
        if (!this.prologueSlot.getItem().isEmpty()) {
            this.enchantmentList = this.getEnchantmentList(this.level, this.prologueSlot.getItem());
        }
        this.initSelectedIndexes();
    }

    private void initSelectedIndexes() {
        IdMap<Holder<Enchantment>> idMap = this.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
        for (int id : this.blockEntity.getSelectedEnchantmentSet()) {
            for (int i = 0; i < this.enchantmentList.size(); i++) {
                if (idMap.getId(this.enchantmentList.get(i)) == id) {
                    this.selectedIndexes.add(i);
                    break;
                }
            }
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public void slotsChanged(Container container) {
        ItemStack prologueItem = this.prologueSlot.getItem();
        if (!prologueItem.is(this.blockEntity.getLastPrologueItem().getItem())) {
            this.blockEntity.setLastPrologueItem(prologueItem);
            this.enchantmentList = this.getEnchantmentList(this.level, prologueItem);
            this.selectedIndexes.clear();
            this.blockEntity.getSelectedEnchantmentSet().clear();
            this.blockEntity.getPrologueSlotUpdateListener().run();
        }
    }

    public void select(int index) {
        int size = this.selectedIndexes.size();
        this.selectedIndexes.add(index);
        if (this.selectedIndexes.size() != size) {
            this.refreshSelectedEnchantments();
        }
    }

    public void unselect(int index) {
        int size = this.selectedIndexes.size();
        this.selectedIndexes.remove(index);
        if (this.selectedIndexes.size() != size) {
            this.refreshSelectedEnchantments();
        }
    }

    public boolean hasSelectedEnchantment() {
        return !this.selectedIndexes.isEmpty();
    }

    private void refreshSelectedEnchantments() {
        Set<Integer> selectedEnchantmentSet = this.blockEntity.getSelectedEnchantmentSet();
        selectedEnchantmentSet.clear();
        IdMap<Holder<Enchantment>> idMap = this.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
        for (int selectedIndex : this.selectedIndexes) {
            ListUtil.safelyGet(this.enchantmentList, selectedIndex).ifPresent(
                enchantment -> selectedEnchantmentSet.add(idMap.getId(enchantment))
            );
        }
        this.blockEntity.onChange();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex == 36 || slotIndex == 37) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(slotIndex);
        if (slotIndex == 38) {
            ItemStack item = slot.getItem();
            if (!item.isEmpty()) {
                if (!this.moveItemStackTo(item, 0, 36, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }
        if (slotIndex >= 0 && slotIndex <= 35) {
            ItemStack item = slot.getItem();
            if (!item.isEmpty()) {
                if (!this.moveItemStackTo(item, 38, 39, true)) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!this.level.isClientSide()) {
            this.blockEntity.setOpenMenu(false);
        }
    }

    private List<Holder<Enchantment>> getEnchantmentList(Level level, ItemStack prologueItem) {
        Registry<Enchantment> enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        // 蓝宝石护符
        ObjectArrayList<Holder<Enchantment>> sapphireAmulet = new ObjectArrayList<>();
        enchantments.get(Enchantments.RESPIRATION).ifPresent(sapphireAmulet::add);
        enchantments.get(Enchantments.AQUA_AFFINITY).ifPresent(sapphireAmulet::add);
        enchantments.get(Enchantments.DEPTH_STRIDER).ifPresent(sapphireAmulet::add);

        // 红宝石护符
        ObjectArrayList<Holder<Enchantment>> rubyAmulet = new ObjectArrayList<>();
        enchantments.get(Enchantments.FIRE_PROTECTION).ifPresent(rubyAmulet::add);

        // 黄玉护符
        ObjectArrayList<Holder<Enchantment>> topazAmulet = new ObjectArrayList<>();
        enchantments.get(Enchantments.CHANNELING).ifPresent(topazAmulet::add);

        // 绿宝石护符
        ObjectArrayList<Holder<Enchantment>> emeraldAmulet = StreamSupport
            .stream(enchantments.getTagOrEmpty(EnchantmentTags.TRADEABLE).spliterator(), false)
            .filter((instance) -> !sapphireAmulet.contains(instance) && !rubyAmulet.contains(instance) && !topazAmulet.contains(instance))
            .collect(
                ObjectArrayList::new,
                ObjectArrayList::add,
                ObjectList::addAll
            );

        // 宝石护符
        ObjectArrayList<Holder<Enchantment>> gemAmulet = new ObjectArrayList<>();
        gemAmulet.addAll(sapphireAmulet);
        gemAmulet.addAll(rubyAmulet);
        gemAmulet.addAll(topazAmulet);
        gemAmulet.addAll(emeraldAmulet);

        if (prologueItem.is(ModItems.SAPPHIRE_AMULET)) {
            return List.copyOf(sapphireAmulet);
        } else if (prologueItem.is(ModItems.RUBY_AMULET)) {
            return List.copyOf(rubyAmulet);
        } else if (prologueItem.is(ModItems.TOPAZ_AMULET)) {
            return List.copyOf(topazAmulet);
        } else if (prologueItem.is(ModItems.EMERALD_AMULET)) {
            return List.copyOf(emeraldAmulet);
        } else if (prologueItem.is(ModItems.GEM_AMULET)) {
            return List.copyOf(gemAmulet);
        }

        // 羽毛护符
        ObjectArrayList<Holder<Enchantment>> featherAmulet = new ObjectArrayList<>();
        enchantments.get(Enchantments.FEATHER_FALLING).ifPresent(featherAmulet::add);

        // 寂静护符
        ObjectArrayList<Holder<Enchantment>> silenceAmulet = new ObjectArrayList<>();
        enchantments.get(Enchantments.PROTECTION).ifPresent(silenceAmulet::add);
        enchantments.get(Enchantments.SWIFT_SNEAK).ifPresent(silenceAmulet::add);

        // 猫护符
        ObjectArrayList<Holder<Enchantment>> catAmulet = new ObjectArrayList<>();
        enchantments.get(Enchantments.BLAST_PROTECTION).ifPresent(catAmulet::add);
        enchantments.get(Enchantments.THORNS).ifPresent(catAmulet::add);

        // 狗护符
        ObjectArrayList<Holder<Enchantment>> dogAmulet = new ObjectArrayList<>();
        enchantments.get(Enchantments.PROJECTILE_PROTECTION).ifPresent(dogAmulet::add);

        // 自然护符
        ObjectArrayList<Holder<Enchantment>> natureAmulet = new ObjectArrayList<>();
        natureAmulet.addAll(featherAmulet);
        natureAmulet.addAll(silenceAmulet);
        natureAmulet.addAll(catAmulet);
        natureAmulet.addAll(dogAmulet);

        if (prologueItem.is(ModItems.FEATHER_AMULET)) {
            return List.copyOf(featherAmulet);
        } else if (prologueItem.is(ModItems.SILENCE_AMULET)) {
            return List.copyOf(silenceAmulet);
        } else if (prologueItem.is(ModItems.CAT_AMULET)) {
            return List.copyOf(catAmulet);
        } else if (prologueItem.is(ModItems.DOG_AMULET)) {
            return List.copyOf(dogAmulet);
        } else if (prologueItem.is(ModItems.NATURE_AMULET)) {
            return List.copyOf(natureAmulet);
        }

        // 紫水晶
        ObjectArrayList<Holder<Enchantment>> amethystShard = new ObjectArrayList<>();
        enchantments.get(ModEnchantments.FELLING_KEY).ifPresent(amethystShard::add);
        enchantments.get(ModEnchantments.HARVEST_KEY).ifPresent(amethystShard::add);

        // 皇家钢锭
        ObjectArrayList<Holder<Enchantment>> royalSteel = new ObjectArrayList<>();
        enchantments.get(Enchantments.SILK_TOUCH).ifPresent(royalSteel::add);
        enchantments.get(Enchantments.UNBREAKING).ifPresent(royalSteel::add);
        enchantments.get(Enchantments.MENDING).ifPresent(royalSteel::add);

        // 余烬金属锭
        ObjectArrayList<Holder<Enchantment>> emberMetal = new ObjectArrayList<>();
        enchantments.get(ModEnchantments.SMELTING_KEY).ifPresent(emberMetal::add);
        enchantments.get(Enchantments.FIRE_ASPECT).ifPresent(emberMetal::add);
        enchantments.get(Enchantments.FLAME).ifPresent(emberMetal::add);

        // 浮霜金属锭
        ObjectArrayList<Holder<Enchantment>> frostMetal = new ObjectArrayList<>();
        enchantments.get(ModEnchantments.DISINTEGRATION_KEY).ifPresent(frostMetal::add);
        enchantments.get(Enchantments.FROST_WALKER).ifPresent(frostMetal::add);

        // 超限合金锭
        ObjectArrayList<Holder<Enchantment>> transcendium = new ObjectArrayList<>();
        enchantments.get(Enchantments.FORTUNE).ifPresent(transcendium::add);
        enchantments.get(Enchantments.LOOTING).ifPresent(transcendium::add);
        enchantments.get(ModEnchantments.BEHEADING_KEY).ifPresent(transcendium::add);
        enchantments.get(Enchantments.LUCK_OF_THE_SEA).ifPresent(transcendium::add);
        transcendium.addAll(amethystShard);
        transcendium.addAll(royalSteel);
        transcendium.addAll(emberMetal);
        transcendium.addAll(frostMetal);

        if (prologueItem.is(Items.AMETHYST_SHARD)) {
            return List.copyOf(amethystShard);
        } else if (prologueItem.is(ModItems.ROYAL_STEEL_INGOT)) {
            return List.copyOf(royalSteel);
        } else if (prologueItem.is(ModItems.EMBER_METAL_INGOT)) {
            return List.copyOf(emberMetal);
        } else if (prologueItem.is(ModItems.FROST_METAL_INGOT)) {
            return List.copyOf(frostMetal);
        } else if (prologueItem.is(ModItems.TRANSCENDIUM_INGOT)) {
            return List.copyOf(transcendium);
        }

        return List.of();
    }
}
