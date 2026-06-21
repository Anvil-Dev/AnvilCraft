package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyRecipe;
import dev.dubhe.anvilcraft.block.entity.celestial.TempleDemandRecipe;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.items.IItemHandler;

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
        if (option == null || !name().equals(option.megastructure())) return;
        if (be.getPlanetaryResourceSet() == null || !be.getPlanetaryResourceSet().hasCivilization()) return;

        long currentDay = be.getLevel().getDayTime() / 24000;
        if (lastDay != currentDay || demandItem.isEmpty()) {
            lastDay = currentDay;
            cycleDay = (cycleDay + 1) % 3;
            demandSatisfied = false;
            demandProgress = 0;
            TempleDemandRecipe.Category cat = cycleDay == 2 ? TempleDemandRecipe.Category.PUNISHMENT : TempleDemandRecipe.Category.BLESSING;
            var demand = pickTempleDemand(be, cat);
            demandItem = demand.item();
            demandCount = demand.count();
            pushTempleDemandToLogistics(be);
            be.setChanged();
            be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }

        if (!demandSatisfied && !demandItem.isEmpty()) {
            if (trySatisfyDemand(be)) {
                demandSatisfied = true;
                pushTempleDemandToLogistics(be);
                be.setChanged();
                be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
            }
        }

        if (demandSatisfied) {
            produceTempleOfferings(be);
        }

        if (be.getLevel().getGameTime() % 20 == 0) {
            pushTempleDemandToLogistics(be);
        }
    }

    public void pushTempleDemandToLogistics(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        scanAdjacentBlocks(
            (checkPos) -> {
                var blockEntity = be.getLevel().getBlockEntity(checkPos);
                if (blockEntity instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logiBe) {
                    logiBe.setTempleDemandItem(demandSatisfied ? ItemStack.EMPTY : demandItem);
                    logiBe.setTempleDemandCount(demandSatisfied ? 0 : demandCount);
                    logiBe.setTempleDemandProgress(demandSatisfied ? 0 : demandProgress);
                    logiBe.setTempleDemandSatisfied(demandSatisfied);
                    logiBe.setChanged();
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

        var globalRecipes = be.getLevel()
            .getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.TEMPLE_DEMAND_TYPE.get())
            .stream()
            .map(RecipeHolder::value)
            .toList();
        for (var recipe : globalRecipes) {
            if (recipe.category() == category) {
                candidates.addAll(recipe.entries());
            }
        }

        if (be.getCelestialBodyData() instanceof SpecialCelestialBodyData s && !s.isErrorPlanet()) {
            ResourceLocation recipeId = ResourceLocation.parse(s.recipeId());
            be.getLevel().getRecipeManager().byKey(recipeId).ifPresent(holder -> {
                if (holder.value() instanceof SpecialCelestialBodyRecipe specialRecipe) {
                    List<SpecialCelestialBodyRecipe.DemandEntry> demands = category == TempleDemandRecipe.Category.BLESSING
                                                                           ? specialRecipe.templeBlessings()
                                                                           : specialRecipe.templePunishments();
                    for (var d : demands) {
                        candidates.add(new TempleDemandRecipe.Entry(d.id(), d.count()));
                    }
                }
            });
        }
        if (candidates.isEmpty()) return TempleDemandResult.EMPTY;

        TempleDemandRecipe.Entry entry = candidates.get(be.getLevel().getRandom().nextInt(candidates.size()));
        var item = BuiltInRegistries.ITEM.get(entry.itemResource());
        if (item == Items.AIR) return TempleDemandResult.EMPTY;
        return new TempleDemandResult(new ItemStack(item, 1), entry.count());
    }

    private boolean trySatisfyDemand(CelestialForgingAnvilBlockEntity be) {
        if (demandItem.isEmpty() || demandCount <= 0) return false;
        if (demandProgress >= demandCount) return true;
        List<IItemHandler> logistics = findLogisticsInterfaces(be);
        if (logistics.isEmpty()) return false;

        int needed = demandCount - demandProgress;
        for (IItemHandler handler : logistics) {
            for (int slot = 0; slot < handler.getSlots() && needed > 0; slot++) {
                ItemStack contained = handler.getStackInSlot(slot);
                if (ItemStack.isSameItemSameComponents(contained, demandItem)) {
                    ItemStack extracted = handler.extractItem(slot, needed, false);
                    int taken = extracted.getCount();
                    demandProgress += taken;
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
        ResourceLocation chosenItem = null;
        for (PlanetaryResourceSet.WeightedItemStack offering : offerings) {
            cumulative += offering.weight();
            if (roll < cumulative) {
                chosenItem = offering.itemId();
                break;
            }
        }
        if (chosenItem == null) chosenItem = offerings.getFirst().itemId();

        var item = BuiltInRegistries.ITEM.get(chosenItem);
        if (item.asItem() == Items.AIR) return;
        ItemStack output = new ItemStack(item, 1);

        List<IItemHandler> logistics = findLogisticsInterfaces(be);
        if (logistics.isEmpty()) return;

        int startIdx = 0;
        for (int attempt = 0; attempt < logistics.size(); attempt++) {
            int idx = (startIdx + attempt) % logistics.size();
            IItemHandler handler = logistics.get(idx);
            ItemStack remainder = insertIntoHandler(handler, output);
            if (remainder.getCount() < output.getCount()) {
                return;
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("templeCycleDay", cycleDay);
        tag.putLong("templeLastDay", lastDay);
        if (!demandItem.isEmpty()) {
            tag.put("templeDemand", demandItem.save(registries));
        }
        tag.putInt("templeDemandCount", demandCount);
        tag.putInt("templeDemandProgress", demandProgress);
        tag.putBoolean("templeDemandSatisfied", demandSatisfied);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        this.cycleDay = tag.getInt("templeCycleDay");
        this.lastDay = tag.contains("templeLastDay") ? tag.getLong("templeLastDay") : -1;
        if (tag.contains("templeDemand")) {
            this.demandItem = ItemStack.parse(registries, tag.getCompound("templeDemand")).orElse(ItemStack.EMPTY);
        } else {
            this.demandItem = ItemStack.EMPTY;
        }
        this.demandCount = tag.getInt("templeDemandCount");
        this.demandProgress = tag.getInt("templeDemandProgress");
        this.demandSatisfied = tag.getBoolean("templeDemandSatisfied");
    }

    @Override
    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("templeCycleDay", cycleDay);
        tag.putLong("templeLastDay", lastDay);
        if (!demandItem.isEmpty()) {
            tag.put("templeDemand", demandItem.save(registries));
        }
        tag.putInt("templeDemandCount", demandCount);
        tag.putInt("templeDemandProgress", demandProgress);
        tag.putBoolean("templeDemandSatisfied", demandSatisfied);
    }

    @Override
    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.cycleDay = tag.getInt("templeCycleDay");
        this.lastDay = tag.contains("templeLastDay") ? tag.getLong("templeLastDay") : -1;
        if (tag.contains("templeDemand")) {
            this.demandItem = ItemStack.parse(registries, tag.getCompound("templeDemand")).orElse(ItemStack.EMPTY);
        } else {
            this.demandItem = ItemStack.EMPTY;
        }
        this.demandCount = tag.getInt("templeDemandCount");
        this.demandProgress = tag.getInt("templeDemandProgress");
        this.demandSatisfied = tag.getBoolean("templeDemandSatisfied");
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        this.cycleDay = 0;
        this.lastDay = -1;
        this.demandItem = ItemStack.EMPTY;
        this.demandCount = 0;
        this.demandProgress = 0;
        this.demandSatisfied = false;
        pushTempleDemandToLogistics(be);
    }
}
