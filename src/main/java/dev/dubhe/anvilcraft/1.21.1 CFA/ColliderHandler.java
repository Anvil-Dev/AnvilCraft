package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.collision.AnvilCollisionCraftRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

public class ColliderHandler extends BaseMegastructureHandler {
    private static final int COOLDOWN_TICKS = 10;
    private static final int MAX_COLLISIONS = 16;

    private int cooldown = 0;
    private int cycleRemaining = 0;
    private ItemStack reservedAnvil = ItemStack.EMPTY;
    private ItemStack reservedHitBlock = ItemStack.EMPTY;
    private int activeSpeed = 0;
    private final List<ItemStack> targetItems = new ArrayList<>();
    private int logisticsRoundRobin = 0;

    @Override
    public String name() {
        return "stellar_ring_collider";
    }

    @Override
    public int getInputPower(CelestialForgingAnvilBlockEntity be) {
        return 4000;
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        CelestialRefactorOption option = be.getActiveMegastructureOption();
        if (option == null || !name().equals(option.megastructure())) return;
        if (be.getPlanetaryResourceSet() == null) return;
        if (!(be.getCelestialBodyData() instanceof StarData star)) return;
        if (star.size() >= 48) return;

        boolean starMissing = !be.isAmplifierPresent();
        boolean isProcessing = cycleRemaining > 0 && !be.isPowerInsufficient();

        if (be.getLevel().getGameTime() % 20 == 0) {
            refreshColliderTargetItems(be);
        }

        if (starMissing) {
            if (cycleRemaining > 0 || !reservedAnvil.isEmpty() || !reservedHitBlock.isEmpty()) {
                outputColliderReservedItems(be);
                resetColliderState(be);
            }
            broadcastColliderState(be, false, true);
            return;
        }

        broadcastColliderState(be, isProcessing, false);

        if (be.isPowerInsufficient()) {
            outputColliderReservedItems(be);
            resetColliderState(be);
            return;
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        if (cycleRemaining > 0) {
            cycleRemaining--;
            if (cycleRemaining == 0) {
                completeColliderCycle(be);
                cooldown = COOLDOWN_TICKS;
            }
            return;
        }

        tryStartColliderCycle(be);
    }

    private void broadcastColliderState(CelestialForgingAnvilBlockEntity be, boolean processing, boolean starMissing) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        scanAdjacentBlocks(
            (checkPos) -> {
                var blockEntity = be.getLevel().getBlockEntity(checkPos);
                if (blockEntity instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logiBe) {
                    logiBe.setColliderProcessing(processing);
                    logiBe.setColliderStarMissing(starMissing);
                    logiBe.setChanged();
                }
            }, be
        );
    }

    private void broadcastColliderTargets(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        scanAdjacentBlocks(
            (checkPos) -> {
                var blockEntity = be.getLevel().getBlockEntity(checkPos);
                if (blockEntity instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logiBe) {
                    logiBe.setColliderTargetItems(new ArrayList<>(targetItems));
                    logiBe.setChanged();
                }
            }, be
        );
    }

    private void refreshColliderTargetItems(CelestialForgingAnvilBlockEntity be) {
        targetItems.clear();
        if (be.getLevel() == null) return;
        List<RecipeHolder<AnvilCollisionCraftRecipe>> recipes = be.getLevel()
            .getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.ANVIL_COLLISION_CRAFT.get());
        for (var holder : recipes) {
            AnvilCollisionCraftRecipe recipe = holder.value();
            if (recipe.outputItems().isEmpty()) continue;
            var hitPred = recipe.hitBlock();
            if (hitPred.getStatesCache().isEmpty()) continue;
            for (var state : hitPred.getStatesCache()) {
                ItemStack item = new ItemStack(state.getBlock().asItem(), 1);
                boolean has = false;
                for (ItemStack existing : targetItems) {
                    if (ItemStack.isSameItemSameComponents(existing, item)) {
                        has = true;
                        break;
                    }
                }
                if (!has) targetItems.add(item);
            }
        }
        broadcastColliderTargets(be);
    }

    private record CLogisticsRef(IItemHandler handler, BlockPos pos) {
    }

    private record CLocatedStack(int li, int slot, ItemStack stack, Block block) {
    }

    private void tryStartColliderCycle(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || !(be.getCelestialBodyData() instanceof StarData star)) return;
        int mass = be.getStellarMass();
        int mag = star.magneticFieldStrength();
        int denominator = mass * mag + 10;
        if (denominator <= 0) return;

        List<RecipeHolder<AnvilCollisionCraftRecipe>> recipes = be.getLevel()
            .getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.ANVIL_COLLISION_CRAFT.get());

        List<CLogisticsRef> logistics = new ArrayList<>();
        scanAdjacentBlocks(
            (checkPos) -> {
                var blockEntity = be.getLevel().getBlockEntity(checkPos);
                if (blockEntity instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logiBe) {
                    logistics.add(new CLogisticsRef(logiBe.getItemHandler(), checkPos.immutable()));
                }
            }, be
        );
        if (logistics.isEmpty()) return;

        List<CLocatedStack> anvilStacks = new ArrayList<>();
        List<CLocatedStack> hitStacks = new ArrayList<>();

        for (int li = 0; li < logistics.size(); li++) {
            IItemHandler handler = logistics.get(li).handler;
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                Block block = Block.byItem(stack.getItem());
                if (block == Blocks.AIR) continue;
                for (var holder : recipes) {
                    AnvilCollisionCraftRecipe recipe = holder.value();
                    if (recipe.outputItems().isEmpty()) continue;
                    if (recipe.anvil().test(be.getLevel(), block.defaultBlockState(), null)) {
                        anvilStacks.add(new CLocatedStack(li, slot, stack, block));
                        break;
                    }
                }
                for (var holder : recipes) {
                    AnvilCollisionCraftRecipe recipe = holder.value();
                    if (recipe.outputItems().isEmpty()) continue;
                    if (recipe.hitBlock().test(be.getLevel(), block.defaultBlockState(), null)) {
                        hitStacks.add(new CLocatedStack(li, slot, stack, block));
                        break;
                    }
                }
            }
        }

        AnvilCollisionCraftRecipe bestRecipe = null;
        int bestSpeed = Integer.MIN_VALUE;
        CLocatedStack bestAnvil = null;
        CLocatedStack bestHit = null;

        for (CLocatedStack anvil : anvilStacks) {
            for (CLocatedStack hit : hitStacks) {
                if (anvil.li == hit.li && anvil.slot == hit.slot) continue;
                for (var holder : recipes) {
                    AnvilCollisionCraftRecipe recipe = holder.value();
                    if (recipe.outputItems().isEmpty()) continue;
                    if (recipe.speed() <= bestSpeed) continue;
                    if (recipe.anvil().test(be.getLevel(), anvil.block.defaultBlockState(), null) && recipe.hitBlock()
                        .test(be.getLevel(), hit.block.defaultBlockState(), null)) {
                        bestSpeed = recipe.speed();
                        bestRecipe = recipe;
                        bestAnvil = anvil;
                        bestHit = hit;
                    }
                }
            }
        }

        if (bestRecipe == null || bestAnvil == null || bestHit == null) return;

        int t = (1000 * bestRecipe.speed() + denominator - 1) / denominator;
        if (t <= 0) t = 1;

        int anvilToTake = Math.min(bestAnvil.stack.getCount(), MAX_COLLISIONS);
        int hitToTake = Math.min(bestHit.stack.getCount(), MAX_COLLISIONS);

        CLogisticsRef anvilSrc = logistics.get(bestAnvil.li);
        CLogisticsRef hitSrc = logistics.get(bestHit.li);

        reservedAnvil = anvilSrc.handler.extractItem(bestAnvil.slot, anvilToTake, false);
        reservedHitBlock = hitSrc.handler.extractItem(bestHit.slot, hitToTake, false);

        activeSpeed = bestRecipe.speed();
        cycleRemaining = t;
        cooldown = 0;

        markLogisticsProcessing(be, anvilSrc.pos, true);
        if (!hitSrc.pos.equals(anvilSrc.pos)) {
            markLogisticsProcessing(be, hitSrc.pos, true);
        }
        broadcastColliderTargets(be);
        broadcastColliderState(be, true, false);

        be.setChanged();
        be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
    }

    private void markLogisticsProcessing(CelestialForgingAnvilBlockEntity be, BlockPos pos, boolean processing) {
        if (be.getLevel() == null || pos == null) return;
        var blockEntity = be.getLevel().getBlockEntity(pos);
        if (blockEntity instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logiBe) {
            logiBe.setColliderTargetItems(new ArrayList<>(targetItems));
            logiBe.setColliderProcessing(processing);
            logiBe.setChanged();
        }
    }

    private void completeColliderCycle(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null) return;
        List<IItemHandler> logistics = findLogisticsInterfaces(be);

        List<RecipeHolder<AnvilCollisionCraftRecipe>> recipes = be.getLevel()
            .getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.ANVIL_COLLISION_CRAFT.get());

        AnvilCollisionCraftRecipe activeRecipe = null;
        for (var holder : recipes) {
            AnvilCollisionCraftRecipe recipe = holder.value();
            if (recipe.outputItems().isEmpty()) continue;
            if (recipe.speed() != activeSpeed) continue;
            Block anvilBlock = Block.byItem(reservedAnvil.getItem());
            Block hitBlock = Block.byItem(reservedHitBlock.getItem());
            if (anvilBlock != Blocks.AIR && hitBlock != Blocks.AIR && recipe.anvil()
                .test(be.getLevel(), anvilBlock.defaultBlockState(), null) && recipe.hitBlock()
                    .test(be.getLevel(), hitBlock.defaultBlockState(), null)) {
                activeRecipe = recipe;
                break;
            }
        }

        int anvilReserved = reservedAnvil.getCount();
        int hitReserved = reservedHitBlock.getCount();
        boolean consumeAnvil = activeRecipe != null && activeRecipe.consume();
        int collisionCount = consumeAnvil ? Math.min(anvilReserved, hitReserved) : hitReserved;

        // Return unused hit blocks
        int hitRemaining = hitReserved - collisionCount;
        if (hitRemaining > 0) {
            ItemStack hitReturn = reservedHitBlock.copyWithCount(hitRemaining);
            if (!logistics.isEmpty()) {
                int startIdx = logisticsRoundRobin % logistics.size();
                for (int attempt = 0; attempt < logistics.size(); attempt++) {
                    int idx = (startIdx + attempt) % logistics.size();
                    ItemStack remainder = insertIntoHandler(logistics.get(idx), hitReturn);
                    if (remainder.getCount() < hitReturn.getCount()) {
                        logisticsRoundRobin = (idx + 1) % logistics.size();
                        if (remainder.isEmpty()) {
                            hitReturn = ItemStack.EMPTY;
                            break;
                        }
                        hitReturn = remainder;
                    }
                }
            }
            if (!hitReturn.isEmpty() && logistics.isEmpty()) {
                dropItemOnGround(hitReturn, be.getLevel(), be.getBlockPos());
            }
        }

        // Return unused anvils
        if (consumeAnvil) {
            int anvilRemaining = anvilReserved - collisionCount;
            if (anvilRemaining > 0) {
                ItemStack anvilReturn = reservedAnvil.copyWithCount(anvilRemaining);
                if (!logistics.isEmpty()) {
                    int startIdx = logisticsRoundRobin % logistics.size();
                    for (int attempt = 0; attempt < logistics.size(); attempt++) {
                        int idx = (startIdx + attempt) % logistics.size();
                        ItemStack remainder = insertIntoHandler(logistics.get(idx), anvilReturn);
                        if (remainder.getCount() < anvilReturn.getCount()) {
                            logisticsRoundRobin = (idx + 1) % logistics.size();
                            if (remainder.isEmpty()) {
                                anvilReturn = ItemStack.EMPTY;
                                break;
                            }
                            anvilReturn = remainder;
                        }
                    }
                }
                if (!anvilReturn.isEmpty() && logistics.isEmpty()) {
                    dropItemOnGround(anvilReturn, be.getLevel(), be.getBlockPos());
                }
            }
        } else {
            if (!reservedAnvil.isEmpty() && !logistics.isEmpty()) {
                ItemStack anvilReturn = reservedAnvil.copy();
                int startIdx = logisticsRoundRobin % logistics.size();
                for (int attempt = 0; attempt < logistics.size(); attempt++) {
                    int idx = (startIdx + attempt) % logistics.size();
                    ItemStack remainder = insertIntoHandler(logistics.get(idx), anvilReturn);
                    if (remainder.getCount() < anvilReturn.getCount()) {
                        logisticsRoundRobin = (idx + 1) % logistics.size();
                        if (remainder.isEmpty()) {
                            anvilReturn = ItemStack.EMPTY;
                            break;
                        }
                        anvilReturn = remainder;
                    }
                }
                if (!anvilReturn.isEmpty()) dropItemOnGround(anvilReturn, be.getLevel(), be.getBlockPos());
            } else if (!reservedAnvil.isEmpty()) {
                dropItemOnGround(reservedAnvil.copy(), be.getLevel(), be.getBlockPos());
            }
        }

        // Output products
        if (activeRecipe != null && be.getLevel() instanceof ServerLevel serverLevel && collisionCount > 0) {
            for (ChanceItemStack chanceStack : activeRecipe.outputItems()) {
                for (int c = 0; c < collisionCount; c++) {
                    ItemStack output = chanceStack.getResult(serverLevel);
                    if (output.isEmpty()) continue;
                    int startIdx = logisticsRoundRobin % logistics.size();
                    for (int attempt = 0; attempt < logistics.size(); attempt++) {
                        int idx = (startIdx + attempt) % logistics.size();
                        ItemStack remainder = insertIntoHandler(logistics.get(idx), output);
                        if (remainder.getCount() < output.getCount()) {
                            logisticsRoundRobin = (idx + 1) % logistics.size();
                            if (!remainder.isEmpty()) {
                                output = remainder;
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
        }

        broadcastColliderState(be, false, false);
        resetColliderState(be);
    }

    private void outputColliderReservedItems(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null) return;
        List<IItemHandler> logistics = findLogisticsInterfaces(be);

        if (!reservedAnvil.isEmpty()) {
            ItemStack remaining = reservedAnvil.copy();
            if (!logistics.isEmpty()) {
                int startIdx = logisticsRoundRobin % logistics.size();
                for (int attempt = 0; attempt < logistics.size() && !remaining.isEmpty(); attempt++) {
                    int idx = (startIdx + attempt) % logistics.size();
                    remaining = insertIntoHandler(logistics.get(idx), remaining);
                }
            }
            if (!remaining.isEmpty()) dropItemOnGround(remaining, be.getLevel(), be.getBlockPos());
        }

        if (!reservedHitBlock.isEmpty()) {
            ItemStack remaining = reservedHitBlock.copy();
            if (!logistics.isEmpty()) {
                int startIdx = logisticsRoundRobin % logistics.size();
                for (int attempt = 0; attempt < logistics.size() && !remaining.isEmpty(); attempt++) {
                    int idx = (startIdx + attempt) % logistics.size();
                    remaining = insertIntoHandler(logistics.get(idx), remaining);
                }
            }
            if (!remaining.isEmpty()) dropItemOnGround(remaining, be.getLevel(), be.getBlockPos());
        }

        resetColliderState(be);
    }

    private void resetColliderState(CelestialForgingAnvilBlockEntity be) {
        cooldown = 0;
        cycleRemaining = 0;
        reservedAnvil = ItemStack.EMPTY;
        reservedHitBlock = ItemStack.EMPTY;
        activeSpeed = 0;
        broadcastColliderState(be, false, false);
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        outputColliderReservedItems(be);
        resetColliderState(be);
    }
}
