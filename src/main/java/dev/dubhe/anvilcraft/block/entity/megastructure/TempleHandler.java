package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyRecipe;
import dev.dubhe.anvilcraft.block.entity.celestial.TempleDemandRecipe;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.List;

public class TempleHandler extends BaseMegastructureHandler {

    @Getter
    private int cycleDay = 0;
    private long lastDay = -1;
    @Getter
    private ItemStack demandItem = ItemStack.EMPTY;
    @Getter
    private int demandCount = 0;
    @Getter
    private int demandProgress = 0;
    @Getter
    private boolean demandSatisfied = false;

    @Override
    public String name() {
        return "temple";
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        CelestialRefactorOption option = be.getActiveMegastructureOption();
        if (option == null || !this.name().equals(option.megastructure())) return;
        if (be.getPlanetaryResourceSet() == null || !be.getPlanetaryResourceSet().hasCivilization()) return;

        long currentDay = be.getLevel().getGameTime() / 24000;
        if (this.lastDay != currentDay || this.demandItem.isEmpty()) {
            this.lastDay = currentDay;
            this.cycleDay = (this.cycleDay + 1) % 3;
            this.demandSatisfied = false;
            this.demandProgress = 0;
            TempleDemandRecipe.Category cat = this.cycleDay == 2
                                              ? TempleDemandRecipe.Category.PUNISHMENT
                                              : TempleDemandRecipe.Category.BLESSING;
            var demand = this.pickTempleDemand(be, cat);
            this.demandItem = demand.item();
            this.demandCount = demand.count();
            this.pushTempleDemandToLogistics(be);
            be.setChanged();
            be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }

        if (!this.demandSatisfied && !this.demandItem.isEmpty()) {
            if (this.trySatisfyDemand(be)) {
                this.demandSatisfied = true;
                this.pushTempleDemandToLogistics(be);
                be.setChanged();
                be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
            }
        }

        if (this.demandSatisfied) {
            this.produceTempleOfferings(be);
        }

        if (be.getLevel().getGameTime() % 20 == 0) {
            this.pushTempleDemandToLogistics(be);
        }
    }

    public void pushTempleDemandToLogistics(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        scanAdjacentBlocks(
            (checkPos) -> {
                var blockEntity = be.getLevel().getBlockEntity(checkPos);
                if (blockEntity instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logiBe) {
                    logiBe.setTempleDemandItem(this.demandSatisfied ? ItemStack.EMPTY : this.demandItem);
                    logiBe.setTempleDemandCount(this.demandSatisfied ? 0 : this.demandCount);
                    logiBe.setTempleDemandProgress(this.demandSatisfied ? 0 : this.demandProgress);
                    logiBe.setTempleDemandSatisfied(this.demandSatisfied);
                }
            }, be
        );
    }

    private record TempleDemandResult(ItemStack item, int count) {
        static final TempleDemandResult EMPTY = new TempleDemandResult(ItemStack.EMPTY, 0);
    }

    private TempleDemandResult pickTempleDemand(CelestialForgingAnvilBlockEntity be, TempleDemandRecipe.Category category) {
        if (be.getLevel() == null) return TempleDemandResult.EMPTY;

        List<TempleDemandRecipe.Entry> candidates = new ArrayList<>();

        var globalRecipes = RecipesRecord.getRecipes(be.getLevel())
            .byType(ModRecipeTypes.TEMPLE_DEMAND.get())
            .stream()
            .map(RecipeHolder::value)
            .toList();
        for (var recipe : globalRecipes) {
            if (recipe.category() == category) {
                candidates.addAll(recipe.entries());
            }
        }

        if (be.getCelestialBodyData() instanceof SpecialCelestialBodyData s && !s.isErrorPlanet()) {
            Identifier recipeId = Identifier.parse(s.recipeId());
            ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, recipeId);
            var holder = RecipesRecord.getRecipes(be.getLevel()).byKey(key);
            if (holder != null && holder.value() instanceof SpecialCelestialBodyRecipe specialRecipe) {
                List<SpecialCelestialBodyRecipe.DemandEntry> demands = category == TempleDemandRecipe.Category.BLESSING
                                                                       ? specialRecipe.templeBlessings()
                                                                       : specialRecipe.templePunishments();
                for (var d : demands) {
                    candidates.add(new TempleDemandRecipe.Entry(d.id(), d.count()));
                }
            }
        }

        if (candidates.isEmpty()) return TempleDemandResult.EMPTY;

        TempleDemandRecipe.Entry entry = candidates.get(be.getLevel().getRandom().nextInt(candidates.size()));
        var item = BuiltInRegistries.ITEM.get(entry.itemResource())
            .map(h -> (net.minecraft.world.level.ItemLike) h.value())
            .orElse(Items.AIR);
        if (item == Items.AIR) return TempleDemandResult.EMPTY;
        return new TempleDemandResult(new ItemStack(item, 1), entry.count());
    }

    private boolean trySatisfyDemand(CelestialForgingAnvilBlockEntity be) {
        if (this.demandItem.isEmpty() || this.demandCount <= 0) return false;
        if (this.demandProgress >= this.demandCount) return true;
        List<ResourceHandler<ItemResource>> logistics = findLogisticsInterfaces(be);
        if (logistics.isEmpty()) return false;

        int needed = this.demandCount - this.demandProgress;
        for (ResourceHandler<ItemResource> handler : logistics) {
            for (int slot = 0; slot < handler.size() && needed > 0; slot++) {
                ItemStack contained = getStackFromHandler(handler, slot);
                if (ItemStack.isSameItemSameComponents(contained, this.demandItem)) {
                    ItemStack extracted = extractFromHandler(handler, slot, needed);
                    int taken = extracted.getCount();
                    this.demandProgress += taken;
                    needed -= taken;
                }
            }
            if (needed <= 0) {
                be.setChanged();
                return true;
            }
        }
        return false;
    }

    private void produceTempleOfferings(CelestialForgingAnvilBlockEntity be) {
        List<PlanetaryResourceSet.WeightedItemStack> offerings = null;
        if (be.getPlanetaryResourceSet() != null) {
            offerings = be.getPlanetaryResourceSet().getOfferings();
        }
        if (offerings == null || offerings.isEmpty()) return;

        int totalWeight = offerings.stream().mapToInt(PlanetaryResourceSet.WeightedItemStack::weight).sum();
        if (totalWeight <= 0) return;

        int roll = be.getLevel().getRandom().nextInt(totalWeight);
        int cumulative = 0;
        Identifier chosenItem = null;
        for (PlanetaryResourceSet.WeightedItemStack offering : offerings) {
            cumulative += offering.weight();
            if (roll < cumulative) {
                chosenItem = offering.itemId();
                break;
            }
        }
        if (chosenItem == null) chosenItem = offerings.getFirst().itemId();

        var item = BuiltInRegistries.ITEM.get(chosenItem).map(h -> (net.minecraft.world.level.ItemLike) h.value()).orElse(Items.AIR);
        if (item.asItem() == Items.AIR) return;
        ItemStack output = new ItemStack(item, 1);

        List<ResourceHandler<ItemResource>> logistics = findLogisticsInterfaces(be);
        if (logistics.isEmpty()) return;

        for (ResourceHandler<ItemResource> handler : logistics) {
            ItemStack remainder = insertIntoHandler(handler, output);
            if (remainder.getCount() < output.getCount()) return;
        }
    }

    // === 持久化 ===

    @Override
    public void saveAdditional(ValueOutput output) {
        output.putInt("templeCycleDay", this.cycleDay);
        output.putLong("templeLastDay", this.lastDay);
        if (!this.demandItem.isEmpty()) {
            output.store("templeDemand", ItemStack.OPTIONAL_CODEC, this.demandItem);
        }
        output.putInt("templeDemandCount", this.demandCount);
        output.putInt("templeDemandProgress", this.demandProgress);
        output.putBoolean("templeDemandSatisfied", this.demandSatisfied);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        this.cycleDay = input.getIntOr("templeCycleDay", 0);
        this.lastDay = input.getLongOr("templeLastDay", -1);
        this.demandItem = input.read("templeDemand", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.demandCount = input.getIntOr("templeDemandCount", 0);
        this.demandProgress = input.getIntOr("templeDemandProgress", 0);
        this.demandSatisfied = input.getBooleanOr("templeDemandSatisfied", false);
    }

    @Override
    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("templeCycleDay", this.cycleDay);
        tag.putLong("templeLastDay", this.lastDay);
        if (!this.demandItem.isEmpty()) {
            tag.put("templeDemand", ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, this.demandItem).getOrThrow());
        }
        tag.putInt("templeDemandCount", this.demandCount);
        tag.putInt("templeDemandProgress", this.demandProgress);
        tag.putBoolean("templeDemandSatisfied", this.demandSatisfied);
    }

    @Override
    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.cycleDay = tag.getIntOr("templeCycleDay", 0);
        this.lastDay = tag.getLongOr("templeLastDay", -1);
        if (tag.contains("templeDemand")) {
            this.demandItem = ItemStack.CODEC.parse(NbtOps.INSTANCE, tag.get("templeDemand")).result().orElse(ItemStack.EMPTY);
        } else {
            this.demandItem = ItemStack.EMPTY;
        }
        this.demandCount = tag.getIntOr("templeDemandCount", 0);
        this.demandProgress = tag.getIntOr("templeDemandProgress", 0);
        this.demandSatisfied = tag.getBooleanOr("templeDemandSatisfied", false);
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        this.cycleDay = 0;
        this.lastDay = -1;
        this.demandItem = ItemStack.EMPTY;
        this.demandCount = 0;
        this.demandProgress = 0;
        this.demandSatisfied = false;
        this.pushTempleDemandToLogistics(be);
    }
}
