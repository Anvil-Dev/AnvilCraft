package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.workstation.AutoEnchantingTableBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantments;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.AutoEnchantingTableMenu;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.IdMap;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AutoEnchantingTableBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, IPowerConsumer {
    @Getter
    @Setter
    @Nullable
    private PowerGrid grid;
    /// 索引0：物品输入 1：物品输出 2：引物
    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);

    @Getter
    private final Map<Item, List<ResourceKey<Enchantment>>> enchantmentMap = new Object2ObjectOpenHashMap<>();
    private int enchantmentSeed = 0;
    private final RandomSource random = RandomSource.create();

    @Getter
    private final FluidStacksResourceHandler fluidHandler = new FluidStacksResourceHandler(1, 32_000) {
        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.is(ModFluids.EXP_FLUID);
        }

        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            AutoEnchantingTableBlockEntity.this.onChange();
        }
    };

    private int cooldown = 0;
    // region 控制书本的渲染
    public int time;
    public float rot;
    public float oldRot;
    public float targetRot;
    public float open;
    public float oldOpen;
    public float flip;
    public float oldFlip;
    public float flipT;
    public float flipA;
    // endregion

    public static Map<Item, List<ResourceKey<Enchantment>>> getEnchantmentMap(Level level) {
        Registry<Enchantment> enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        // 蓝宝石护符
        ObjectArrayList<ResourceKey<Enchantment>> sapphireAmulet = new ObjectArrayList<>();
        enchantments.get(Enchantments.BREACH).ifPresent((ref) -> sapphireAmulet.add(Objects.requireNonNull(ref.getKey())));
        enchantments.get(Enchantments.AQUA_AFFINITY).ifPresent((ref) -> sapphireAmulet.add(Objects.requireNonNull(ref.getKey())));
        enchantments.get(Enchantments.DEPTH_STRIDER).ifPresent((ref) -> sapphireAmulet.add(Objects.requireNonNull(ref.getKey())));

        // 红宝石护符
        ObjectArrayList<ResourceKey<Enchantment>> rubyAmulet = new ObjectArrayList<>();
        enchantments.get(Enchantments.FIRE_PROTECTION).ifPresent((ref) -> rubyAmulet.add(Objects.requireNonNull(ref.getKey())));

        // 黄玉护符
        ObjectArrayList<ResourceKey<Enchantment>> topazAmulet = new ObjectArrayList<>();
        enchantments.get(Enchantments.CHANNELING).ifPresent((ref) -> topazAmulet.add(Objects.requireNonNull(ref.getKey())));

        // 绿宝石护符
        Iterable<Holder<Enchantment>> tradeableEnchants = enchantments.getTagOrEmpty(EnchantmentTags.TRADEABLE);
        ObjectArrayList<ResourceKey<Enchantment>> emeraldAmulet = new ObjectArrayList<>();
        tradeableEnchants.forEach((holder) -> {
            ResourceKey<Enchantment> key = Objects.requireNonNull(holder.getKey());
            if (!sapphireAmulet.contains(key) && !rubyAmulet.contains(key) && !topazAmulet.contains(key)) {
                emeraldAmulet.add(key);
            }
        });

        // 宝石护符
        ObjectArrayList<ResourceKey<Enchantment>> gemAmulet = new ObjectArrayList<>();
        gemAmulet.addAll(sapphireAmulet);
        gemAmulet.addAll(rubyAmulet);
        gemAmulet.addAll(topazAmulet);
        gemAmulet.addAll(emeraldAmulet);

        // 羽毛护符
        ObjectArrayList<ResourceKey<Enchantment>> featherAmulet = new ObjectArrayList<>();
        enchantments.get(Enchantments.FEATHER_FALLING).ifPresent((ref) -> featherAmulet.add(Objects.requireNonNull(ref.getKey())));

        // 寂静护符
        ObjectArrayList<ResourceKey<Enchantment>> silenceAmulet = new ObjectArrayList<>();
        enchantments.get(Enchantments.PROTECTION).ifPresent((ref) -> silenceAmulet.add(Objects.requireNonNull(ref.getKey())));
        enchantments.get(Enchantments.SWIFT_SNEAK).ifPresent((ref) -> silenceAmulet.add(Objects.requireNonNull(ref.getKey())));

        // 猫护符
        ObjectArrayList<ResourceKey<Enchantment>> catAmulet = new ObjectArrayList<>();
        enchantments.get(Enchantments.BLAST_PROTECTION).ifPresent((ref) -> catAmulet.add(Objects.requireNonNull(ref.getKey())));
        enchantments.get(Enchantments.THORNS).ifPresent((ref) -> catAmulet.add(Objects.requireNonNull(ref.getKey())));

        // 狗护符
        ObjectArrayList<ResourceKey<Enchantment>> dogAmulet = new ObjectArrayList<>();
        enchantments.get(Enchantments.PROJECTILE_PROTECTION).ifPresent((ref) -> dogAmulet.add(Objects.requireNonNull(ref.getKey())));

        // 自然护符
        ObjectArrayList<ResourceKey<Enchantment>> natureAmulet = new ObjectArrayList<>();
        natureAmulet.addAll(featherAmulet);
        natureAmulet.addAll(silenceAmulet);
        natureAmulet.addAll(catAmulet);
        natureAmulet.addAll(dogAmulet);

        // 紫水晶
        ObjectArrayList<ResourceKey<Enchantment>> amethystShard = new ObjectArrayList<>();
        enchantments.get(ModEnchantments.FELLING_KEY).ifPresent((ref) -> amethystShard.add(Objects.requireNonNull(ref.getKey())));
        enchantments.get(ModEnchantments.HARVEST_KEY).ifPresent((ref) -> amethystShard.add(Objects.requireNonNull(ref.getKey())));

        // 皇家钢锭
        ObjectArrayList<ResourceKey<Enchantment>> royalSteelShard = new ObjectArrayList<>();
        enchantments.get(Enchantments.SILK_TOUCH).ifPresent((ref) -> royalSteelShard.add(Objects.requireNonNull(ref.getKey())));
        enchantments.get(Enchantments.UNBREAKING).ifPresent((ref) -> royalSteelShard.add(Objects.requireNonNull(ref.getKey())));
        enchantments.get(Enchantments.MENDING).ifPresent((ref) -> royalSteelShard.add(Objects.requireNonNull(ref.getKey())));

        // 余烬金属锭
        ObjectArrayList<ResourceKey<Enchantment>> emberMetal = new ObjectArrayList<>();
        enchantments.get(ModEnchantments.SMELTING_KEY).ifPresent((ref) -> emberMetal.add(Objects.requireNonNull(ref.getKey())));
        enchantments.get(Enchantments.FIRE_ASPECT).ifPresent((ref) -> emberMetal.add(Objects.requireNonNull(ref.getKey())));
        enchantments.get(Enchantments.FLAME).ifPresent((ref) -> emberMetal.add(Objects.requireNonNull(ref.getKey())));

        // 浮霜金属锭
        ObjectArrayList<ResourceKey<Enchantment>> frostMetal = new ObjectArrayList<>();
        enchantments.get(ModEnchantments.DISINTEGRATION_KEY).ifPresent((ref) -> frostMetal.add(Objects.requireNonNull(ref.getKey())));
        enchantments.get(Enchantments.FROST_WALKER).ifPresent((ref) -> frostMetal.add(Objects.requireNonNull(ref.getKey())));

        // 超限合金锭
        ObjectArrayList<ResourceKey<Enchantment>> transcendium = new ObjectArrayList<>();
        enchantments.get(Enchantments.FORTUNE).ifPresent((ref) -> transcendium.add(Objects.requireNonNull(ref.getKey())));
        enchantments.get(Enchantments.LOOTING).ifPresent((ref) -> transcendium.add(Objects.requireNonNull(ref.getKey())));
        enchantments.get(ModEnchantments.BEHEADING_KEY).ifPresent((ref) -> transcendium.add(Objects.requireNonNull(ref.getKey())));
        enchantments.get(Enchantments.LUCK_OF_THE_SEA).ifPresent((ref) -> transcendium.add(Objects.requireNonNull(ref.getKey())));
        transcendium.addAll(amethystShard);
        transcendium.addAll(royalSteelShard);
        transcendium.addAll(emberMetal);
        transcendium.addAll(frostMetal);

        return Map.copyOf(Util.make(new Object2ObjectOpenHashMap<>(), (map) -> {
            map.put(ModItems.SAPPHIRE_AMULET.get(), List.copyOf(sapphireAmulet));
            map.put(ModItems.RUBY_AMULET.get(), List.copyOf(rubyAmulet));
            map.put(ModItems.TOPAZ.get(), List.copyOf(topazAmulet));
            map.put(ModItems.EMERALD_AMULET.get(), List.copyOf(emeraldAmulet));
            map.put(ModItems.GEM_AMULET.get(), List.copyOf(gemAmulet));
            map.put(ModItems.FEATHER_AMULET.get(), List.copyOf(featherAmulet));
            map.put(ModItems.SILENCE_AMULET.get(), List.copyOf(silenceAmulet));
            map.put(ModItems.CAT_AMULET.get(), List.copyOf(catAmulet));
            map.put(ModItems.DOG_AMULET.get(), List.copyOf(dogAmulet));
            map.put(ModItems.NATURE_AMULET.get(), List.copyOf(natureAmulet));
            map.put(Items.AMETHYST_SHARD, List.copyOf(amethystShard));
            map.put(ModItems.ROYAL_STEEL_INGOT.get(), List.copyOf(royalSteelShard));
            map.put(ModItems.EMBER_METAL_INGOT.get(), List.copyOf(emberMetal));
            map.put(ModItems.FROST_METAL_INGOT.get(), List.copyOf(frostMetal));
            map.put(ModItems.TRANSCENDIUM_INGOT.get(), List.copyOf(transcendium));
        }));
    }

    public AutoEnchantingTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ItemStack getDisplayInputItem() {
        return this.items.getFirst();
    }

    public ItemStack getDisplayOutputItem() {
        return this.items.get(1);
    }

    public void bookAnimationTick(
        final Level level,
        final BlockPos worldPosition,
        final BlockState state
    ) {
        this.oldOpen = this.open;
        this.oldRot = this.rot;
        this.targetRot += 0.02F;

        if (!this.getItem(0).isEmpty()) {
            this.open += 0.1f;
            if (this.open < 0.5f || random.nextInt(40) == 0) {
                float old = this.flipT;

                do {
                    this.flipT = this.flipT + (random.nextInt(4) - random.nextInt(4));
                } while (old == this.flipT);
            }
        } else {
            this.open -= 0.1f;
        }

        while (this.rot >= (float) Math.PI) {
            this.rot -= (float) (Math.PI * 2);
        }

        while (this.rot < (float) -Math.PI) {
            this.rot += (float) (Math.PI * 2);
        }

        while (this.targetRot >= (float) Math.PI) {
            this.targetRot -= (float) (Math.PI * 2);
        }

        while (this.targetRot < (float) -Math.PI) {
            this.targetRot += (float) (Math.PI * 2);
        }

        float rotDir = this.targetRot - this.rot;

        while (rotDir >= (float) Math.PI) {
            rotDir -= (float) (Math.PI * 2);
        }

        while (rotDir < (float) -Math.PI) {
            rotDir += (float) (Math.PI * 2);
        }

        this.rot += rotDir * 0.4F;
        this.open = Mth.clamp(this.open, 0, 1f);
        this.time++;
        this.oldFlip = this.flip;
        float diff = (this.flipT - this.flip) * 0.4f;
        diff = Mth.clamp(diff, -0.2f, 0.2f);
        this.flipA = this.flipA + (diff - this.flipA) * 0.9f;
        this.flip = this.flip + this.flipA;
    }

    public void serverTick(
        final Level level,
        final BlockPos pos,
        final BlockState state
    ) {
        if (this.cooldown > 0) {
            this.cooldown--;
        } else if (this.cooldown == 0) {
            this.cooldown = 80;
            if (this.grid != null && !this.grid.isWorking()) {
                return;
            }
            if (!this.getItem(1).isEmpty() || this.getItem(0).isEmpty()) {
                return;
            }
            ItemStack enchantItem = this.getItem(0).copyWithCount(1);
            if (!enchantItem.isEnchantable()) {
                this.setItem(0, ItemStack.EMPTY);
                this.setItem(1, enchantItem);
                return;
            }
            if (this.getItem(2).isEmpty()) {
                // 无引物模式
                int exp = 0;
                for (BlockPos offset : AutoEnchantingTableBlock.BOOKSHELF_OFFSETS) {
                    if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                        exp += 400;
                        if (exp >= 6000) {
                            exp = 6000;
                            break;
                        }
                    }
                }
                if (this.fluidHandler.getAmountAsInt(0) < exp) {
                    return;
                }
                int[][] costAndEnchant = this.getCostAndEnchant(enchantItem);
                if (costAndEnchant.length == 0) {
                    return;
                }
                int index = level.getRandom().nextInt(0, 3);
                int[] enchant = costAndEnchant[index];
                List<EnchantmentInstance> enchantmentList = this.getEnchantmentList(level.registryAccess(), enchantItem, index, enchant[0]);
                if (enchantItem.is(Items.BOOK)) {
                    enchantItem.transmuteCopy(Items.ENCHANTED_BOOK);
                }
                if (!enchantmentList.isEmpty()) {
                    for (EnchantmentInstance enchantmentInstance : enchantmentList) {
                        enchantItem.enchant(enchantmentInstance.enchantment(), enchantmentInstance.level());
                    }
                }
                this.setItem(0, ItemStack.EMPTY);
                this.setItem(1, enchantItem);
                try (Transaction ts = Transaction.openRoot()) {
                    this.fluidHandler.extract(FluidResource.of(ModFluids.EXP_FLUID), exp, ts);
                    ts.commit();
                    this.onChange();
                }
                this.enchantmentSeed = level.getRandom().nextInt();
                level.playSound(null, this.getBlockPos(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
            } else {
                // TODO: 引物模式
            }
        }
    }

    private int[][] getCostAndEnchant(ItemStack itemStack) {
        int[] costs = new int[3];
        int[] enchantClue = new int[]{-1, -1, -1};
        int[] levelClue = new int[]{-1, -1, -1};
        if (itemStack.isEmpty() || !itemStack.isEnchantable()) {
            return new int[][]{};
        }
        if (this.level != null) {
            IdMap<Holder<Enchantment>> idMap = this.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
            int bookcases = 0;
            for (BlockPos offset : AutoEnchantingTableBlock.BOOKSHELF_OFFSETS) {
                if (EnchantingTableBlock.isValidBookShelf(this.level, this.getBlockPos(), offset)) {
                    bookcases++;
                }
            }
            if (this.enchantmentSeed == 0) {
                this.enchantmentSeed = level.getRandom().nextInt();
            }
            random.setSeed(this.enchantmentSeed);
            for (int ixx = 0; ixx < 3; ixx++) {
                costs[ixx] = EnchantmentHelper.getEnchantmentCost(random, ixx, bookcases, itemStack);
                enchantClue[ixx] = -1;
                levelClue[ixx] = -1;
                if (costs[ixx] < ixx + 1) {
                    costs[ixx] = 0;
                }
            }

            for (int ix = 0; ix < 3; ix++) {
                if (costs[ix] > 0) {
                    List<EnchantmentInstance> list = this.getEnchantmentList(level.registryAccess(), itemStack, ix, costs[ix]);
                    if (!list.isEmpty()) {
                        EnchantmentInstance enchant = list.get(random.nextInt(list.size()));
                        enchantClue[ix] = idMap.getId(enchant.enchantment());
                        levelClue[ix] = enchant.level();
                    }
                }
            }
            return new int[][]{
                new int[] { costs[0], enchantClue[0], levelClue[0] },
                new int[] { costs[1], enchantClue[1], levelClue[1] },
                new int[] { costs[2], enchantClue[2], levelClue[2] }
            };
        }
        return new int[][]{};
    }

    private List<EnchantmentInstance> getEnchantmentList(final RegistryAccess access, final ItemStack itemStack, final int slot, final int enchantmentCost) {
        random.setSeed(this.enchantmentSeed + slot);
        Optional<HolderSet.Named<Enchantment>> tag = access.lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE);
        if (tag.isEmpty()) {
            return List.of();
        }

        List<EnchantmentInstance> list = EnchantmentHelper.selectEnchantment(random, itemStack, enchantmentCost, tag.get().stream());
        if (itemStack.is(Items.BOOK) && list.size() > 1) {
            list.remove(random.nextInt(list.size()));
        }

        return list;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        this.fluidHandler.serialize(output);
        output.putInt("cooldown", this.cooldown);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, this.items);
        this.fluidHandler.deserialize(input);
        input.getInt("cooldown").ifPresent((cooldown) -> this.cooldown = cooldown);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(new ProblemReporter.Collector(this.problemPath()), registries);
        this.saveAdditional(output);
        return output.buildResult();
    }

    public void onChange() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 2);
        }
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.anvilcraft.auto_enchanting_table");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        this.items.set(slot, itemStack.copyWithCount(1));
        this.onChange();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack itemStack) {
        return 1;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new AutoEnchantingTableMenu(ModMenuTypes.AUTO_ENCHANTING_TABLE.get(), containerId, inventory, this);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return new int[] { 0, 1 };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, @Nullable Direction direction) {
        return slot == 0;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        return slot == 1;
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public int getInputPower() {
        return this.items.get(2).isEmpty() ? 16 : 64;
    }
}
