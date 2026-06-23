package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.collision.AnvilCollisionCraftRecipe;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

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
        if (option == null || !this.name().equals(option.megastructure())) return;
        if (be.getPlanetaryResourceSet() == null) return;
        if (!(be.getCelestialBodyData() instanceof StarData star)) return;
        if (star.size() >= 48) return;

        boolean starMissing = !be.isAmplifierPresent();
        boolean isProcessing = this.cycleRemaining > 0 && !be.isPowerInsufficient();

        if (be.getLevel().getGameTime() % 20 == 0) {
            this.refreshColliderTargetItems(be);
        }

        if (starMissing) {
            if (this.cycleRemaining > 0 || !this.reservedAnvil.isEmpty() || !this.reservedHitBlock.isEmpty()) {
                this.outputColliderReservedItems(be);
                this.resetColliderState(be);
            }
            this.broadcastColliderState(be, false, true);
            return;
        }

        this.broadcastColliderState(be, isProcessing, false);

        if (be.isPowerInsufficient()) {
            this.outputColliderReservedItems(be);
            this.resetColliderState(be);
            return;
        }

        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }

        if (this.cycleRemaining > 0) {
            this.cycleRemaining--;
            if (this.cycleRemaining == 0) {
                this.completeColliderCycle(be);
                this.cooldown = COOLDOWN_TICKS;
            }
            return;
        }

        this.tryStartColliderCycle(be);
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
                    logiBe.setColliderTargetItems(new ArrayList<>(this.targetItems));
                    logiBe.setChanged();
                }
            }, be
        );
    }

    private void refreshColliderTargetItems(CelestialForgingAnvilBlockEntity be) {
        this.targetItems.clear();
        if (be.getLevel() == null) return;
        var recipes = RecipesRecord.getRecipes(be.getLevel()).byType(ModRecipeTypes.ANVIL_COLLISION_CRAFT.get());
        for (var holder : recipes) {
            AnvilCollisionCraftRecipe recipe = holder.value();
            if (recipe.outputItems().isEmpty()) continue;
            var hitPred = recipe.hitBlock();
            if (hitPred.getStatesCache().isEmpty()) continue;
            for (var state : hitPred.getStatesCache()) {
                ItemStack item = new ItemStack(state.getBlock().asItem(), 1);
                boolean has = false;
                for (ItemStack existing : this.targetItems) {
                    if (ItemStack.isSameItemSameComponents(existing, item)) {
                        has = true;
                        break;
                    }
                }
                if (!has) this.targetItems.add(item);
            }
        }
        this.broadcastColliderTargets(be);
    }

    private record CLogisticsRef(ResourceHandler<ItemResource> handler, BlockPos pos) {
    }

    private record CLocatedStack(int li, int slot, ItemStack stack, Block block) {
    }

    private void tryStartColliderCycle(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || !(be.getCelestialBodyData() instanceof StarData star)) return;
        int mass = be.getStellarMass();
        int mag = star.magneticFieldStrength();
        int denominator = mass * mag + 10;
        if (denominator <= 0) return;

        var recipes = RecipesRecord.getRecipes(be.getLevel()).byType(ModRecipeTypes.ANVIL_COLLISION_CRAFT.get());

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
            ResourceHandler<ItemResource> handler = logistics.get(li).handler;
            for (int slot = 0; slot < handler.size(); slot++) {
                ItemStack stack = getStackFromHandler(handler, slot);
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

        this.reservedAnvil = extractFromHandler(anvilSrc.handler, bestAnvil.slot, anvilToTake);
        this.reservedHitBlock = extractFromHandler(hitSrc.handler, bestHit.slot, hitToTake);

        this.activeSpeed = bestRecipe.speed();
        this.cycleRemaining = t;
        this.cooldown = 0;

        this.markLogisticsProcessing(be, anvilSrc.pos, true);
        if (!hitSrc.pos.equals(anvilSrc.pos)) {
            this.markLogisticsProcessing(be, hitSrc.pos, true);
        }
        this.broadcastColliderTargets(be);
        this.broadcastColliderState(be, true, false);

        be.setChanged();
        be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
    }

    private void markLogisticsProcessing(CelestialForgingAnvilBlockEntity be, BlockPos pos, boolean processing) {
        if (be.getLevel() == null || pos == null) return;
        var blockEntity = be.getLevel().getBlockEntity(pos);
        if (blockEntity instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logiBe) {
            logiBe.setColliderTargetItems(new ArrayList<>(this.targetItems));
            logiBe.setColliderProcessing(processing);
            logiBe.setChanged();
        }
    }

    private void completeColliderCycle(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null) return;
        List<ResourceHandler<ItemResource>> logistics = findLogisticsInterfaces(be);

        var recipes = RecipesRecord.getRecipes(be.getLevel()).byType(ModRecipeTypes.ANVIL_COLLISION_CRAFT.get());

        AnvilCollisionCraftRecipe activeRecipe = null;
        for (var holder : recipes) {
            AnvilCollisionCraftRecipe recipe = holder.value();
            if (recipe.outputItems().isEmpty()) continue;
            if (recipe.speed() != this.activeSpeed) continue;
            Block anvilBlock = Block.byItem(this.reservedAnvil.getItem());
            Block hitBlock = Block.byItem(this.reservedHitBlock.getItem());
            if (anvilBlock != Blocks.AIR && hitBlock != Blocks.AIR && recipe.anvil()
                .test(be.getLevel(), anvilBlock.defaultBlockState(), null) && recipe.hitBlock()
                    .test(be.getLevel(), hitBlock.defaultBlockState(), null)) {
                activeRecipe = recipe;
                break;
            }
        }

        int anvilReserved = this.reservedAnvil.getCount();
        int hitReserved = this.reservedHitBlock.getCount();
        boolean consumeAnvil = activeRecipe != null && activeRecipe.consume();
        int collisionCount = consumeAnvil ? Math.min(anvilReserved, hitReserved) : hitReserved;

        int hitRemaining = hitReserved - collisionCount;
        if (hitRemaining > 0) {
            ItemStack hitReturn = this.reservedHitBlock.copyWithCount(hitRemaining);
            this.returnToLogistics(logistics, hitReturn);
        }

        if (consumeAnvil) {
            int anvilRemaining = anvilReserved - collisionCount;
            if (anvilRemaining > 0) {
                this.returnToLogistics(logistics, this.reservedAnvil.copyWithCount(anvilRemaining));
            }
        } else {
            if (!this.reservedAnvil.isEmpty()) {
                this.returnToLogistics(logistics, this.reservedAnvil.copy());
            }
        }

        if (activeRecipe != null && be.getLevel() instanceof ServerLevel serverLevel && collisionCount > 0) {
            for (ChanceItemStack chanceStack : activeRecipe.outputItems()) {
                for (int c = 0; c < collisionCount; c++) {
                    ItemStackTemplate template = chanceStack.stack();
                    ItemStack output = new ItemStack(template.item(), template.count());
                    if (output.isEmpty()) continue;
                    for (ResourceHandler<ItemResource> handler : logistics) {
                        ItemStack remainder = insertIntoHandler(handler, output);
                        if (remainder.getCount() < output.getCount()) {
                            break;
                        }
                    }
                }
            }
        }

        this.broadcastColliderState(be, false, false);
        this.resetColliderState(be);
    }

    private void returnToLogistics(List<ResourceHandler<ItemResource>> logistics, ItemStack stack) {
        if (logistics.isEmpty()) {
            // Can't drop to ground without level reference; discard
            return;
        }
        int startIdx = this.logisticsRoundRobin % logistics.size();
        for (int attempt = 0; attempt < logistics.size(); attempt++) {
            int idx = (startIdx + attempt) % logistics.size();
            ItemStack remainder = insertIntoHandler(logistics.get(idx), stack);
            if (remainder.getCount() < stack.getCount()) {
                this.logisticsRoundRobin = (idx + 1) % logistics.size();
                if (remainder.isEmpty()) return;
                stack = remainder;
            }
        }
    }

    private void outputColliderReservedItems(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null) return;
        List<ResourceHandler<ItemResource>> logistics = findLogisticsInterfaces(be);

        if (!this.reservedAnvil.isEmpty() && !logistics.isEmpty()) {
            this.returnToLogistics(logistics, this.reservedAnvil.copy());
        }
        if (!this.reservedHitBlock.isEmpty() && !logistics.isEmpty()) {
            this.returnToLogistics(logistics, this.reservedHitBlock.copy());
        }

        this.resetColliderState(be);
    }

    private void resetColliderState(CelestialForgingAnvilBlockEntity be) {
        this.cooldown = 0;
        this.cycleRemaining = 0;
        this.reservedAnvil = ItemStack.EMPTY;
        this.reservedHitBlock = ItemStack.EMPTY;
        this.activeSpeed = 0;
        this.broadcastColliderState(be, false, false);
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        this.outputColliderReservedItems(be);
        this.resetColliderState(be);
    }
}
