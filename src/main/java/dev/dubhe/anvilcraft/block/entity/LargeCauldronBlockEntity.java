package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.cache.IItemHandlerCache;
import dev.anvilcraft.lib.v2.recipe.event.InWorldRecipeEvent;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.predicate.block.HasBlockBase;
import dev.anvilcraft.lib.v2.recipe.predicate.item.HasItemIngredient;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeData;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeManager;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.fluid.FluidHandlerWrapper;
import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.api.itemhandler.LargeCauldronInputHandler;
import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.NeutronIrradiatorBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluidTags;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTriggers;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.DamageAnvil;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasAnvil;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.util.FireReforgingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LargeCauldronBlockEntity extends BlockEntity
    implements IItemHandlerHolder, IItemHandlerCache, IFluidHandlerHolder {
    public static final int OUTPUT_SLOTS = 32;
    public static final int MAX_PROCESS_EFFICIENCY = 9;
    private static final int[][] INPUT_SLOT_OFFSETS = {
        {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };
    private static final int[][] FOOTPRINT_OFFSETS = {
        {0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };
    private static final double CONTENT_HEIGHT = 2.25;
    private static final IItemHandler EMPTY_HANDLER = new EmptyItemHandler();
    private static final InWorldRecipeData<Map<Long, FluidRecipeState>> FLUID_RECIPE_STATES =
        InWorldRecipeData.of(AnvilCraft.of("large_cauldron_fluid_states"), (context, key) -> new HashMap<>());
    private static final ResourceLocation FLUID_RECIPE_ACCEPTOR = AnvilCraft.of("large_cauldron_fluid_acceptor");

    private final LargeCauldronInputHandler input = new LargeCauldronInputHandler(this::contentsChanged);
    private final ItemStackHandler output = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            LargeCauldronBlockEntity.this.contentsChanged();
        }
    };
    private final LargeCauldronFluidHandler fluids = new LargeCauldronFluidHandler(this::contentsChanged);
    private final IItemHandler selectedInput = new SelectedInputHandler();
    private int processingSlot = -1;
    private boolean processingOutput;
    private int lastImpactEntityId = Integer.MIN_VALUE;
    private long lastImpactGameTime = Long.MIN_VALUE;
    private long recipePreviewGameTime = Long.MIN_VALUE;
    private List<RecipePreview> recipePreviewCache = List.of();
    private boolean droppedContents;
    private boolean ignited;

    public LargeCauldronBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static int inputSlotForPart(Cube3x3PartHalf part) {
        for (int i = 0; i < INPUT_SLOT_OFFSETS.length; i++) {
            if (INPUT_SLOT_OFFSETS[i][0] == part.getOffsetX()
                && INPUT_SLOT_OFFSETS[i][1] == part.getOffsetZ()) return i;
        }
        return -1;
    }

    public static @Nullable LargeCauldronBlockEntity getMain(
        Level level,
        BlockPos pos,
        BlockState state
    ) {
        if (!(state.getBlock() instanceof LargeCauldronBlock block)) return null;
        BlockEntity entity = level.getBlockEntity(block.getMainPartPos(pos, state));
        return entity instanceof LargeCauldronBlockEntity cauldron ? cauldron : null;
    }

    public LargeCauldronBlockEntity getMainPart() {
        if (!(this.getBlockState().getBlock() instanceof LargeCauldronBlock block) || this.level == null) return this;
        BlockPos mainPos = block.getMainPartPos(this.worldPosition, this.getBlockState());
        BlockEntity entity = this.level.getBlockEntity(mainPos);
        return entity instanceof LargeCauldronBlockEntity cauldron ? cauldron : this;
    }

    public boolean isMainPart() {
        return this.getBlockState().getBlock() instanceof LargeCauldronBlock block
               && block.isMainPart(this.getBlockState());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            FluidNetworkManager.INSTANCE.addContainer(this.level, this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null && !this.level.isClientSide()) {
            FluidNetworkManager.INSTANCE.removeContainer(this.level, this.worldPosition);
        }
        super.setRemoved();
    }

    public static void serverTick(
        Level level,
        BlockPos pos,
        BlockState state,
        LargeCauldronBlockEntity entity
    ) {
        if (!entity.isMainPart()) return;
        entity.refreshIgnited();
        entity.applyFluidEffects((ServerLevel) level);
        entity.hurtEntitiesInside((ServerLevel) level);
        entity.reforgeItemsInLava(level);
    }

    @Override
    public IItemHandler getItemHandler() {
        return this.getMainPart().output;
    }

    @Override
    public IItemHandler getInput() {
        LargeCauldronBlockEntity main = this.getMainPart();
        return main.processingSlot < 0 ? EMPTY_HANDLER : main.selectedInput;
    }

    @Override
    public IItemHandler getOutput() {
        return this.getMainPart().output;
    }

    @Override
    public IFluidHandler getFluidHandler() {
        return this.getMainPart().fluids;
    }

    public LargeCauldronInputHandler getInputHandler() {
        return this.getMainPart().input;
    }

    public ItemStackHandler getOutputHandler() {
        return this.getMainPart().output;
    }

    public LargeCauldronFluidHandler getFluids() {
        return this.getMainPart().fluids;
    }

    public FluidStack getTopFluid() {
        FluidStack fluid = this.getMainPart().topFluid();
        return fluid.isEmpty() ? FluidStack.EMPTY : fluid.copy();
    }

    public boolean isIgnited() {
        return this.getMainPart().ignited;
    }

    public void setIgnited(boolean ignited) {
        LargeCauldronBlockEntity main = this.getMainPart();
        boolean next = ignited && main.canIgniteTopFluid();
        if (main.ignited == next) return;
        main.ignited = next;
        main.contentsChanged();
    }

    public IItemHandler getAutomationItemHandler(@Nullable Direction side) {
        LargeCauldronBlockEntity main = this.getMainPart();
        Cube3x3PartHalf part = this.getBlockState().getValue(LargeCauldronBlock.HALF);
        boolean outputSide = side == Direction.DOWN
                             || (side != Direction.UP && part.getOffsetY() == 0);
        if (outputSide) return new OutputOnlyHandler(main.output);
        boolean extractPreferred = side != null
                                   && side.getAxis().isHorizontal()
                                   && part.getOffsetY() > 0;
        return new PreferredInputHandler(main.input, inputSlotForPart(part), extractPreferred);
    }

    public IFluidHandler getAutomationFluidHandler(@Nullable Direction side) {
        LargeCauldronFluidHandler handler = this.getMainPart().fluids;
        if (side == Direction.DOWN) return handler.bottomAccess();
        return handler.topAccess();
    }

    public boolean interactWithFluid(Player player, InteractionHand hand, BlockHitResult hit) {
        if (this.level == null) return false;
        LargeCauldronBlockEntity main = this.getMainPart();
        double minY = main.worldPosition.getY() - 0.5;
        double fraction = Math.max(0.0, Math.min(1.0, (hit.getLocation().y - minY) / 2.25));
        int accessible = Math.max(1, (int) Math.ceil(fraction * LargeCauldronFluidHandler.TOTAL_CAPACITY));
        IFluidHandler handler = main.fluids.sideAccess(accessible);
        if (FluidHandlerWrapper.tryInteractWithBottle(player, hand, handler, this.level, this.worldPosition)) return true;
        if (FluidUtil.interactWithFluidHandler(player, hand, handler)) return true;
        return FluidUtil.getFluidHandler(player.getItemInHand(hand))
            .map(itemHandler -> !itemHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE).isEmpty())
            .orElse(false);
    }

    public boolean clearFluids() {
        LargeCauldronBlockEntity main = this.getMainPart();
        if (main.fluids.getTotalAmount() == 0) return false;
        if (main.level != null && !main.level.isClientSide()) {
            main.fluids.setFluids(List.of());
            main.setIgnited(false);
        }
        return true;
    }

    public boolean insertFromHand(ItemStack held, int preferredSlot) {
        ItemStack remainder = ItemHandlerUtil.insertItem(new PreferredInputHandler(this.input, preferredSlot), held.copy(), false);
        int inserted = held.getCount() - remainder.getCount();
        if (inserted <= 0) return false;
        held.shrink(inserted);
        return true;
    }

    public boolean extractItemsToHand(Player player, InteractionHand hand, int inputSlot) {
        LargeCauldronBlockEntity main = this.getMainPart();
        boolean extractOutputs = inputSlot < 0;
        if (extractOutputs ? main.outputIsEmpty() : main.input.getStackInSlot(inputSlot).isEmpty()) return false;
        if (player.level().isClientSide()) return true;

        List<ItemStack> extracted = new ArrayList<>();
        if (extractOutputs) {
            for (int slot = 0; slot < main.output.getSlots(); slot++) {
                ItemStack stack;
                while (!(stack = main.output.extractItem(slot, Integer.MAX_VALUE, false)).isEmpty()) {
                    extracted.add(stack);
                }
            }
        } else {
            ItemStack stack;
            while (!(stack = main.input.extractItem(inputSlot, Integer.MAX_VALUE, false)).isEmpty()) {
                extracted.add(stack);
            }
        }
        if (extracted.isEmpty()) return false;
        player.setItemInHand(hand, extracted.getFirst());
        for (int i = 1; i < extracted.size(); i++) {
            player.getInventory().placeItemBackInInventory(extracted.get(i));
        }
        return true;
    }

    private boolean outputIsEmpty() {
        for (int slot = 0; slot < this.output.getSlots(); slot++) {
            if (!this.output.getStackInSlot(slot).isEmpty()) return false;
        }
        return true;
    }

    public void absorbItem(ItemEntity entity, int preferredSlot) {
        if (!entity.anvilcraft$isAdsorbable() || entity.isRemoved()) return;
        ItemStack stack = entity.getItem();
        ItemStack remainder = ItemHandlerUtil.insertItem(
            new PreferredInputHandler(this.getMainPart().input, preferredSlot),
            stack.copy(),
            false
        );
        if (remainder.getCount() == stack.getCount()) return;
        if (remainder.isEmpty()) {
            entity.discard();
        } else {
            entity.setItem(remainder);
        }
    }

    public ItemStack insertRecipeOutput(ItemStack stack) {
        return ItemHandlerUtil.insertItem(this.getMainPart().output, stack, false);
    }

    public boolean hasInputMatching(java.util.function.Predicate<ItemStack> predicate) {
        LargeCauldronInputHandler handler = this.getMainPart().input;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty() && predicate.test(stack)) return true;
        }
        return false;
    }

    public void dropContents() {
        LargeCauldronBlockEntity main = this.getMainPart();
        if (main.droppedContents || main.level == null) return;
        main.droppedContents = true;
        for (int slot = 0; slot < main.input.getSlots(); slot++) {
            ItemStack stack;
            while (!(stack = main.input.extractItem(slot, Integer.MAX_VALUE, false)).isEmpty()) {
                Block.popResource(main.level, main.worldPosition, stack);
            }
        }
        for (int slot = 0; slot < main.output.getSlots(); slot++) {
            ItemStack stack = main.output.extractItem(slot, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) Block.popResource(main.level, main.worldPosition, stack);
        }
    }

    public int getLightLevel() {
        LargeCauldronBlockEntity main = this.getMainPart();
        if (main.ignited) return 15;
        LargeCauldronFluidHandler handler = main.fluids;
        int maxLight = 0;
        for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack fluid = handler.getFluidInTank(i);
            if (!fluid.isEmpty()) maxLight = Math.max(maxLight, fluid.getFluidType().getLightLevel(fluid));
        }
        float fill = (float) handler.getTotalAmount()
                     / (LargeCauldronFluidHandler.TANK_COUNT * LargeCauldronFluidHandler.TANK_CAPACITY);
        return Math.round(maxLight * fill);
    }

    private void contentsChanged() {
        if (!this.isMainPart()) return;
        this.recipePreviewGameTime = Long.MIN_VALUE;
        this.setChanged();
        if (this.level == null) return;
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.isMainPart()) return;
        tag.put("Inputs", this.input.serializeNBT(registries));
        tag.put("Outputs", this.output.serializeNBT(registries));
        tag.put("Fluids", this.fluids.serializeNBT(registries));
        tag.putBoolean("Ignited", this.ignited);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (!this.isMainPart()) return;
        this.input.deserializeNBT(registries, tag.getCompound("Inputs"));
        this.output.deserializeNBT(registries, tag.getCompound("Outputs"));
        this.fluids.deserializeNBT(registries, tag.getCompound("Fluids"));
        this.ignited = tag.getBoolean("Ignited") && this.canIgniteTopFluid();
        this.recipePreviewGameTime = Long.MIN_VALUE;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (this.isMainPart()) {
            tag.put("Inputs", this.input.serializeNBT(registries));
            tag.put("Outputs", this.output.serializeNBT(registries));
            tag.put("Fluids", this.fluids.serializeNBT(registries));
            tag.putBoolean("Ignited", this.ignited);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public boolean handleGiantAnvilImpact(AnvilEvent.OnLand event) {
        LargeCauldronBlockEntity main = this.getMainPart();
        Level level = main.level;
        if (!(level instanceof ServerLevel serverLevel)) return false;
        BlockState landedAnvilState = level.getBlockState(event.getPos());
        if (!(landedAnvilState.getBlock() instanceof GiantAnvilBlock giantAnvil)
            || !giantAnvil.getMainPartPos(event.getPos(), landedAnvilState).equals(main.worldPosition.above(3))) {
            return true;
        }
        int entityId = event.getEntity().getId();
        long gameTime = level.getGameTime();
        if (main.lastImpactEntityId == entityId && main.lastImpactGameTime == gameTime) return true;
        main.lastImpactEntityId = entityId;
        main.lastImpactGameTime = gameTime;

        BlockPos base = main.worldPosition.below();
        List<BlockPos> helpers = main.findActiveHelpers(base);
        boolean allowItemCompression = helpers.isEmpty();
        List<FluidStack> initialFluids = main.fluids.copyFluids();
        List<Integer> initialOutputSlots = new ArrayList<>();
        for (int slot = 0; slot < main.output.getSlots(); slot++) {
            if (!main.output.getStackInSlot(slot).isEmpty()) initialOutputSlots.add(slot);
        }
        Set<BlockPos> fuelCharged = new HashSet<>();
        int processed = 0;
        boolean madeProgress;
        do {
            madeProgress = false;
            for (int slot = 0; slot < INPUT_SLOT_OFFSETS.length && processed < MAX_PROCESS_EFFICIENCY; slot++) {
                if (main.input.getStackInSlot(slot).isEmpty()) continue;
                BlockPos slotPos = positionForInputSlot(base, slot);
                List<BlockPos> candidates = helpers.isEmpty()
                    ? List.of(slotPos.below())
                    : orderedHelpers(slotPos, helpers, base);
                RecipeAttempt attempt = main.tryProcessItemGroup(
                    serverLevel,
                    base,
                    candidates,
                    event.getEntity(),
                    slot,
                    false,
                    fuelCharged,
                    allowItemCompression
                );
                if (!attempt.execution().executed()) continue;
                if (level.getBlockState(attempt.helper()).is(ModBlocks.BURNING_HEATER)) {
                    fuelCharged.add(attempt.helper());
                }
                if (attempt.execution().damageAnvil()) event.setAnvilDamage(true);
                processed++;
                madeProgress = true;
            }
        } while (madeProgress && processed < MAX_PROCESS_EFFICIENCY);

        List<BlockPos> centerCandidates = helpers.isEmpty()
            ? List.of(base.below())
            : orderedHelpers(base, helpers, base);
        for (int slot : initialOutputSlots) {
            if (processed >= MAX_PROCESS_EFFICIENCY) break;
            if (main.output.getStackInSlot(slot).isEmpty()) continue;
            RecipeAttempt attempt = main.tryProcessItemGroup(
                serverLevel,
                base,
                centerCandidates,
                event.getEntity(),
                slot,
                true,
                fuelCharged,
                allowItemCompression
            );
            if (!attempt.execution().executed()) continue;
            if (level.getBlockState(attempt.helper()).is(ModBlocks.BURNING_HEATER)) {
                fuelCharged.add(attempt.helper());
            }
            if (attempt.execution().damageAnvil()) event.setAnvilDamage(true);
            processed++;
        }

        if (sameFluids(initialFluids, main.fluids.copyFluids())) {
            while (processed < MAX_PROCESS_EFFICIENCY) {
                RecipeAttempt attempt = main.tryProcessFluidRecipe(
                    serverLevel,
                    base,
                    centerCandidates,
                    event.getEntity(),
                    fuelCharged,
                    allowItemCompression
                );
                if (!attempt.execution().executed()) break;
                if (level.getBlockState(attempt.helper()).is(ModBlocks.BURNING_HEATER)) {
                    fuelCharged.add(attempt.helper());
                }
                if (attempt.execution().damageAnvil()) event.setAnvilDamage(true);
                processed++;
            }
        }
        return true;
    }

    private RecipeAttempt tryProcessItemGroup(
        ServerLevel level,
        BlockPos base,
        List<BlockPos> candidates,
        Entity anvil,
        int slot,
        boolean outputSource,
        Set<BlockPos> fuelCharged,
        boolean allowItemCompression
    ) {
        for (BlockPos helper : candidates) {
            Vec3 contextPos = new Vec3(helper.getX() + 0.5, base.getY() + 1.0, helper.getZ() + 0.5);
            this.processingSlot = slot;
            this.processingOutput = outputSource;
            RecipeExecution execution;
            try {
                execution = this.triggerRecipeGroup(
                    level,
                    contextPos,
                    anvil,
                    slot,
                    outputSource,
                    fuelCharged.contains(helper),
                    allowItemCompression
                );
            } finally {
                this.processingSlot = -1;
                this.processingOutput = false;
            }
            if (execution.executed()) return new RecipeAttempt(execution, helper);
        }
        return RecipeAttempt.EMPTY;
    }

    private RecipeAttempt tryProcessFluidRecipe(
        ServerLevel level,
        BlockPos base,
        List<BlockPos> candidates,
        Entity anvil,
        Set<BlockPos> fuelCharged,
        boolean allowItemCompression
    ) {
        for (BlockPos helper : candidates) {
            Vec3 contextPos = new Vec3(helper.getX() + 0.5, base.getY() + 1.0, helper.getZ() + 0.5);
            InWorldRecipeContext context = new InWorldRecipeContext(level, contextPos, anvil);
            if (fuelCharged.contains(helper)) {
                context.put(SuperHeatingRecipe.ConsumeFuel.FUEL_CONSUMED, true);
            }
            RecipeExecution execution = triggerOneRecipe(level, context, ItemStack.EMPTY, allowItemCompression);
            if (execution.executed()) return new RecipeAttempt(execution, helper);
        }
        return RecipeAttempt.EMPTY;
    }

    private RecipeExecution triggerRecipeGroup(
        ServerLevel level,
        Vec3 contextPos,
        Entity anvil,
        int slot,
        boolean outputSource,
        boolean fuelConsumed,
        boolean allowItemCompression
    ) {
        IItemHandler source = outputSource ? this.output : this.input;
        ItemStack starting = source.getStackInSlot(slot);
        if (starting.isEmpty()) return new RecipeExecution(false, false);
        int itemBudget = starting.getMaxStackSize();
        int consumed = 0;
        boolean executed = false;
        boolean damageAnvil = false;
        while (consumed < itemBudget) {
            ItemStack processingInput = source.getStackInSlot(slot);
            if (processingInput.isEmpty()) break;
            InWorldRecipeContext context = new InWorldRecipeContext(level, contextPos, anvil);
            if (fuelConsumed || executed) {
                context.put(SuperHeatingRecipe.ConsumeFuel.FUEL_CONSUMED, true);
            }
            RecipeExecution current = triggerOneRecipe(
                level,
                context,
                processingInput,
                allowItemCompression
            );
            if (!current.executed()) break;
            executed = true;
            damageAnvil |= current.damageAnvil();
            int consumedNow = processingInput.getCount() - source.getStackInSlot(slot).getCount();
            if (consumedNow <= 0) break;
            consumed += consumedNow;
        }
        return new RecipeExecution(executed, damageAnvil);
    }

    private static RecipeExecution triggerOneRecipe(
        ServerLevel level,
        InWorldRecipeContext context,
        ItemStack processingInput,
        boolean allowItemCompression
    ) {
        InWorldRecipeManager manager = level.getRecipeManager().anvillib$getInWorldRecipeManager();
        for (RecipeHolder<InWorldRecipe> holder : manager.recipeHolders.get(ModRecipeTriggers.ON_ANVIL_FALL_ON.get())) {
            InWorldRecipe recipe = holder.value();
            if (!allowItemCompression && recipe instanceof ItemCompressRecipe) continue;
            if (processingInput.isEmpty()) {
                if (!isFluidOnlyRecipe(recipe)) continue;
            } else if (!recipeAnchoredByInput(recipe, processingInput)) {
                continue;
            }
            if (!recipe.matches(context, level)) continue;
            recipe.assemble(context, level.registryAccess());
            NeoForge.EVENT_BUS.post(new InWorldRecipeEvent(recipe.getType(), holder.id(), recipe, context));
            boolean damageAnvil = context.get(DamageAnvil.DAMAGE_ANVIL);
            GiantAnvilBlock.SUPPRESS_DROPS.set(true);
            try {
                context.accept();
            } finally {
                GiantAnvilBlock.SUPPRESS_DROPS.set(false);
            }
            return new RecipeExecution(true, damageAnvil);
        }
        return new RecipeExecution(false, false);
    }

    private static boolean isFluidOnlyRecipe(InWorldRecipe recipe) {
        boolean hasFluid = false;
        for (IRecipePredicate<?> predicate : recipe.nonConflicting()) {
            if (predicate instanceof HasItemIngredient) return false;
            hasFluid |= predicate instanceof HasCauldron;
        }
        for (IRecipePredicate<?> predicate : recipe.conflicting()) {
            if (predicate instanceof HasItemIngredient) return false;
            hasFluid |= predicate instanceof HasCauldron;
        }
        return hasFluid;
    }

    private static boolean sameFluids(List<FluidStack> first, List<FluidStack> second) {
        if (first.size() != second.size()) return false;
        for (int i = 0; i < first.size(); i++) {
            FluidStack left = first.get(i);
            FluidStack right = second.get(i);
            if (!FluidStack.matches(left, right) || left.getAmount() != right.getAmount()) return false;
        }
        return true;
    }

    private static boolean recipeUsesInput(InWorldRecipe recipe, ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (IRecipePredicate<?> predicate : recipe.nonConflicting()) {
            if (predicate instanceof HasItemIngredient ingredient && ingredient.getItem().test(stack)) return true;
        }
        for (IRecipePredicate<?> predicate : recipe.conflicting()) {
            if (predicate instanceof HasItemIngredient ingredient && ingredient.getItem().test(stack)) return true;
        }
        return false;
    }

    private static boolean recipeAnchoredByInput(InWorldRecipe recipe, ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (IRecipePredicate<?> predicate : recipe.nonConflicting()) {
            if (predicate instanceof HasItemIngredient ingredient) return ingredient.getItem().test(stack);
        }
        for (IRecipePredicate<?> predicate : recipe.conflicting()) {
            if (predicate instanceof HasItemIngredient ingredient) return ingredient.getItem().test(stack);
        }
        return false;
    }

    public List<RecipePreview> getRecipePreviews() {
        LargeCauldronBlockEntity main = this.getMainPart();
        if (main != this) return main.getRecipePreviews();
        if (this.level == null) return List.of();
        long gameTime = this.level.getGameTime();
        if (this.recipePreviewGameTime != Long.MIN_VALUE
            && gameTime >= this.recipePreviewGameTime
            && gameTime - this.recipePreviewGameTime < 10) {
            return this.recipePreviewCache;
        }

        List<RecipeHolder<InWorldRecipe>> recipes = this.getPreviewRecipes();
        if (recipes.isEmpty()) {
            this.recipePreviewGameTime = gameTime;
            this.recipePreviewCache = List.of();
            return this.recipePreviewCache;
        }

        BlockPos base = this.worldPosition.below();
        List<BlockPos> helpers = this.findActiveHelpers(base);
        boolean allowItemCompression = helpers.isEmpty();
        List<RecipePreview> previews = new ArrayList<>();
        for (int slot = 0; slot < INPUT_SLOT_OFFSETS.length; slot++) {
            ItemStack stack = this.input.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            BlockPos slotPos = positionForInputSlot(base, slot);
            List<BlockPos> candidates = helpers.isEmpty()
                ? List.of(slotPos.below())
                : orderedHelpers(slotPos, helpers, base);
            for (BlockPos helper : candidates) {
                RecipePreview preview = this.previewFirstRecipe(
                    recipes,
                    slot,
                    helper,
                    base,
                    allowItemCompression
                );
                if (preview == null) continue;
                previews.add(preview);
                break;
            }
        }
        List<BlockPos> fluidCandidates = helpers.isEmpty()
            ? List.of(base.below())
            : orderedHelpers(base, helpers, base);
        for (BlockPos helper : fluidCandidates) {
            RecipePreview preview = this.previewFirstFluidRecipe(
                recipes,
                helper,
                base,
                allowItemCompression
            );
            if (preview == null) continue;
            previews.add(preview);
            break;
        }
        this.recipePreviewGameTime = gameTime;
        this.recipePreviewCache = List.copyOf(previews);
        return this.recipePreviewCache;
    }

    @SuppressWarnings("unchecked")
    private List<RecipeHolder<InWorldRecipe>> getPreviewRecipes() {
        List<RecipeHolder<InWorldRecipe>> recipes = new ArrayList<>();
        for (RecipeHolder<?> holder : this.level.getRecipeManager().getRecipes()) {
            if (!(holder.value() instanceof InWorldRecipe recipe)) continue;
            if (!recipe.trigger().equals(ModRecipeTriggers.ON_ANVIL_FALL_ON.get())) continue;
            recipes.add((RecipeHolder<InWorldRecipe>) (RecipeHolder<?>) holder);
        }
        recipes.sort(
            Comparator.comparingInt((RecipeHolder<InWorldRecipe> holder) -> holder.value().priority())
                .reversed()
                .thenComparing(holder -> holder.id().toString())
        );
        return recipes;
    }

    private @Nullable RecipePreview previewFirstRecipe(
        Collection<RecipeHolder<InWorldRecipe>> recipes,
        int slot,
        BlockPos helper,
        BlockPos base,
        boolean allowItemCompression
    ) {
        Vec3 contextPos = new Vec3(helper.getX() + 0.5, base.getY() + 1.0, helper.getZ() + 0.5);
        for (RecipeHolder<InWorldRecipe> holder : recipes) {
            InWorldRecipe recipe = holder.value();
            if (!allowItemCompression && recipe instanceof ItemCompressRecipe) continue;
            if (!recipeUsesInput(recipe, this.input.getStackInSlot(slot))) continue;
            PreviewState state = new PreviewState(
                this.copyInputStacks(),
                slot,
                new HashSet<>(),
                this.fluids.copyFluids(),
                new ArrayList<>()
            );
            if (!this.previewCompatible(recipe.nonConflicting(), state, contextPos)) continue;
            boolean matches = recipe.compatible()
                ? this.previewCompatible(recipe.conflicting(), state, contextPos)
                : this.previewIncompatible(recipe.conflicting(), state, contextPos);
            if (!matches || !state.usedItemSlots.contains(slot)) continue;
            return new RecipePreview(
                slot,
                holder.id(),
                categoryPath(holder),
                List.copyOf(state.fluidPredicates)
            );
        }
        return null;
    }

    private @Nullable RecipePreview previewFirstFluidRecipe(
        Collection<RecipeHolder<InWorldRecipe>> recipes,
        BlockPos helper,
        BlockPos base,
        boolean allowItemCompression
    ) {
        Vec3 contextPos = new Vec3(helper.getX() + 0.5, base.getY() + 1.0, helper.getZ() + 0.5);
        for (RecipeHolder<InWorldRecipe> holder : recipes) {
            InWorldRecipe recipe = holder.value();
            if (!allowItemCompression && recipe instanceof ItemCompressRecipe) continue;
            if (!isFluidOnlyRecipe(recipe)) continue;
            PreviewState state = new PreviewState(
                this.copyInputStacks(),
                -1,
                new HashSet<>(),
                this.fluids.copyFluids(),
                new ArrayList<>()
            );
            if (!this.previewCompatible(recipe.nonConflicting(), state, contextPos)) continue;
            boolean matches = recipe.compatible()
                ? this.previewCompatible(recipe.conflicting(), state, contextPos)
                : this.previewIncompatible(recipe.conflicting(), state, contextPos);
            if (!matches) continue;
            return new RecipePreview(
                -1,
                holder.id(),
                categoryPath(holder),
                List.copyOf(state.fluidPredicates)
            );
        }
        return null;
    }

    private List<ItemStack> copyInputStacks() {
        List<ItemStack> result = new ArrayList<>(this.input.getSlots());
        for (int slot = 0; slot < this.input.getSlots(); slot++) {
            result.add(this.input.getStackInSlot(slot).copy());
        }
        return result;
    }

    private boolean previewCompatible(
        List<IRecipePredicate<?>> predicates,
        PreviewState state,
        Vec3 contextPos
    ) {
        for (IRecipePredicate<?> predicate : predicates) {
            if (!this.applyPreviewPredicate(predicate, state, contextPos)) return false;
        }
        return true;
    }

    private boolean previewIncompatible(
        List<IRecipePredicate<?>> predicates,
        PreviewState state,
        Vec3 contextPos
    ) {
        return this.previewIncompatible(
            predicates,
            new boolean[predicates.size()],
            predicates.size(),
            state,
            contextPos
        );
    }

    private boolean previewIncompatible(
        List<IRecipePredicate<?>> predicates,
        boolean[] used,
        int remaining,
        PreviewState state,
        Vec3 contextPos
    ) {
        if (remaining == 0) return true;
        for (int i = 0; i < predicates.size(); i++) {
            if (used[i]) continue;
            PreviewState branch = state.copy();
            if (!this.applyPreviewPredicate(predicates.get(i), branch, contextPos)) continue;
            used[i] = true;
            if (this.previewIncompatible(predicates, used, remaining - 1, branch, contextPos)) {
                state.copyFrom(branch);
                return true;
            }
            used[i] = false;
        }
        return false;
    }

    private boolean applyPreviewPredicate(
        IRecipePredicate<?> predicate,
        PreviewState state,
        Vec3 contextPos
    ) {
        switch (predicate) {
            case HasItemIngredient itemIngredient -> {
                int slot = state.findItem(itemIngredient);
                if (slot < 0) return false;
                state.items.get(slot).shrink(itemIngredient.getItem().count());
                state.usedItemSlots.add(slot);
                return true;
            }
            case HasCauldron cauldron -> {
                if ((cauldron.ignited() && !this.ignited) || !this.targetsThisCauldron(cauldron, contextPos)) {
                    return false;
                }
                if (!applyFluidPredicate(state.fluids, cauldron)) return false;
                state.fluidPredicates.add(cauldron);
                return true;
            }
            case HasBlockBase<?> block -> {
                BlockPos pos = BlockPos.containing(contextPos.add(block.getOffset()));
                return block.getPredicate().test(this.level, this.level.getBlockState(pos), this.level.getBlockEntity(pos));
            }
            case HasAnvil anvil -> {
                boolean matches = anvil.anvil().testWithoutEntity(ModBlocks.GIANT_ANVIL.getDefaultState());
                return matches != anvil.inverted();
            }
            default -> {
            }
        }
        return false;
    }

    private boolean targetsThisCauldron(HasCauldron predicate, Vec3 contextPos) {
        BlockPos pos = BlockPos.containing(contextPos.add(predicate.offset()));
        BlockState state = this.level.getBlockState(pos);
        return state.getBlock() instanceof LargeCauldronBlock block
               && block.getMainPartPos(pos, state).equals(this.worldPosition);
    }

    private static String categoryPath(RecipeHolder<InWorldRecipe> holder) {
        ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
        if (typeId != null && !typeId.getPath().equals("in_world_recipe")) return typeId.getPath();
        String path = holder.id().getPath();
        int separator = path.indexOf('/');
        return separator < 0 ? path : path.substring(0, separator);
    }

    private List<BlockPos> findActiveHelpers(BlockPos base) {
        List<BlockPos> result = new ArrayList<>();
        for (int slot = 0; slot < FOOTPRINT_OFFSETS.length; slot++) {
            BlockPos helper = positionForFootprint(base, slot).below();
            if (isActiveRecipeHelper(this.level.getBlockState(helper))) result.add(helper);
        }
        return result;
    }

    private static boolean isActiveRecipeHelper(BlockState state) {
        if (CampfireBlock.isLitCampfire(state) && state.is(Blocks.CAMPFIRE)) return true;
        if (state.is(ModBlocks.HEATER)) return !state.getValue(HeaterBlock.OVERLOAD);
        if (state.is(ModBlocks.BURNING_HEATER)) return state.getValue(BurningHeaterBlock.LEVEL) == 2;
        if (state.is(ModBlocks.CORRUPTED_BEACON)) return state.getValue(CorruptedBeaconBlock.LIT);
        return state.getBlock() instanceof NeutronIrradiatorBlock;
    }

    private static List<BlockPos> orderedHelpers(BlockPos slotPos, List<BlockPos> helpers, BlockPos base) {
        return helpers.stream().sorted(Comparator
            .comparingInt((BlockPos pos) -> horizontalDistanceSquared(pos, slotPos))
            .thenComparingInt(pos -> horizontalDistanceSquared(pos, base))
            .thenComparingInt(pos -> slotForPosition(base, pos.above())))
            .toList();
    }

    private static int horizontalDistanceSquared(BlockPos first, BlockPos second) {
        int dx = first.getX() - second.getX();
        int dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private static int slotForPosition(BlockPos base, BlockPos pos) {
        int dx = pos.getX() - base.getX();
        int dz = pos.getZ() - base.getZ();
        for (int slot = 0; slot < FOOTPRINT_OFFSETS.length; slot++) {
            if (FOOTPRINT_OFFSETS[slot][0] == dx && FOOTPRINT_OFFSETS[slot][1] == dz) return slot;
        }
        return FOOTPRINT_OFFSETS.length;
    }

    private static BlockPos positionForInputSlot(BlockPos base, int slot) {
        return base.offset(INPUT_SLOT_OFFSETS[slot][0], 0, INPUT_SLOT_OFFSETS[slot][1]);
    }

    private static BlockPos positionForFootprint(BlockPos base, int slot) {
        return base.offset(FOOTPRINT_OFFSETS[slot][0], 0, FOOTPRINT_OFFSETS[slot][1]);
    }

    private void applyFluidEffects(ServerLevel level) {
        List<FluidStack> layers = this.fluids.copyFluids();
        int totalAmount = this.fluids.getTotalAmount();
        if (totalAmount <= 0) return;

        AABB contentArea = this.contentArea();
        double fluidTop = contentArea.minY
                          + CONTENT_HEIGHT * totalAmount / LargeCauldronFluidHandler.TOTAL_CAPACITY;
        AABB fluidArea = new AABB(
            contentArea.minX,
            contentArea.minY,
            contentArea.minZ,
            contentArea.maxX,
            fluidTop,
            contentArea.maxZ
        );
        for (Entity entity : level.getEntitiesOfClass(Entity.class, fluidArea)) {
            if (this.ignited) {
                if (!entity.fireImmune()) {
                    entity.setRemainingFireTicks(entity.getRemainingFireTicks() + 1);
                    if (entity.getRemainingFireTicks() == 0) entity.igniteForSeconds(8.0F);
                }
                entity.hurt(level.damageSources().inFire(), 4.0F);
                continue;
            }
            boolean touchesLava = false;
            FluidStack extinguishingFluid = FluidStack.EMPTY;
            double layerMinY = contentArea.minY;
            for (FluidStack fluid : layers) {
                if (fluid.isEmpty()) continue;
                double layerMaxY = layerMinY
                                   + CONTENT_HEIGHT * fluid.getAmount() / LargeCauldronFluidHandler.TOTAL_CAPACITY;
                AABB layerArea = new AABB(
                    contentArea.minX,
                    layerMinY,
                    contentArea.minZ,
                    contentArea.maxX,
                    layerMaxY,
                    contentArea.maxZ
                );
                if (layerArea.intersects(entity.getBoundingBox())) {
                    touchesLava |= fluid.is(Fluids.LAVA);
                    if (extinguishingFluid.isEmpty() && entity.canFluidExtinguish(fluid.getFluidType())) {
                        extinguishingFluid = fluid;
                    }
                }
                layerMinY = layerMaxY;
            }

            if (touchesLava) entity.lavaHurt();
            if (!extinguishingFluid.isEmpty() && entity.isOnFire()) {
                entity.clearFire();
                boolean creativePlayer = entity instanceof Player player && player.isCreative();
                if (!creativePlayer && entity.mayInteract(level, this.worldPosition)) {
                    this.fluids.drainStoredFluid(
                        extinguishingFluid.copyWithAmount(250),
                        IFluidHandler.FluidAction.EXECUTE
                    );
                }
            }
        }
    }

    private void reforgeItemsInLava(Level level) {
        boolean hasLava = false;
        for (int tank = 0; tank < this.fluids.getTanks(); tank++) {
            if (this.fluids.getFluidInTank(tank).is(Fluids.LAVA)) {
                hasLava = true;
                break;
            }
        }
        if (!hasLava) return;

        for (int slot = 0; slot < this.input.getSlots(); slot++) {
            this.input.mutateStackInSlot(
                slot,
                stack -> FireReforgingUtil.repair(
                    stack,
                    FireReforgingUtil.LAVA_REPAIR_PER_TICK,
                    level,
                    this.worldPosition
                )
            );
        }
    }

    private FluidStack topFluid() {
        for (int tank = this.fluids.getTanks() - 1; tank >= 0; tank--) {
            FluidStack fluid = this.fluids.getFluidInTank(tank);
            if (!fluid.isEmpty()) return fluid;
        }
        return FluidStack.EMPTY;
    }

    private boolean canIgniteTopFluid() {
        return this.topFluid().is(ModFluidTags.IGNITABLE);
    }

    private void refreshIgnited() {
        if (!this.canIgniteTopFluid()) {
            this.setIgnited(false);
            return;
        }
        if (this.ignited) return;
        for (int slot = 0; slot < this.input.getSlots(); slot++) {
            ItemStack stack = this.input.getStackInSlot(slot);
            if (stack.is(ModItemTags.FIRE_STARTER)) {
                this.input.extractItem(slot, 1, false);
                this.setIgnited(true);
                return;
            }
            if (stack.is(ModItemTags.UNBROKEN_FIRE_STARTER)) {
                this.setIgnited(true);
                return;
            }
        }
    }

    private AABB contentArea() {
        return new AABB(
            this.worldPosition.getX() - 0.75,
            this.worldPosition.getY() - 0.5,
            this.worldPosition.getZ() - 0.75,
            this.worldPosition.getX() + 1.75,
            this.worldPosition.getY() + 1.75,
            this.worldPosition.getZ() + 1.75
        );
    }

    private void hurtEntitiesInside(ServerLevel level) {
        BlockPos base = this.worldPosition.below();
        boolean normalCampfire = false;
        boolean soulCampfire = false;
        boolean heater = false;
        for (int slot = 0; slot < FOOTPRINT_OFFSETS.length; slot++) {
            BlockState state = level.getBlockState(positionForFootprint(base, slot).below());
            if (CampfireBlock.isLitCampfire(state)) {
                soulCampfire |= state.is(Blocks.SOUL_CAMPFIRE);
                normalCampfire |= state.is(Blocks.CAMPFIRE);
            }
            heater |= state.is(ModBlocks.HEATER) && !state.getValue(HeaterBlock.OVERLOAD);
        }
        if (!normalCampfire && !soulCampfire && !heater) return;

        AABB inside = this.contentArea();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, inside)) {
            if (living.fireImmune() || living.isSteppingCarefully()) continue;
            if (heater) {
                living.hurt(ModDamageTypes.heaterBurn(level), 4.0F);
            } else {
                living.hurt(level.damageSources().inFire(), soulCampfire ? 2.0F : 1.0F);
            }
        }
    }

    public boolean testFluidRecipe(InWorldRecipeContext context, HasCauldron predicate) {
        LargeCauldronBlockEntity main = this.getMainPart();
        if (predicate.ignited() && !main.ignited) return false;
        FluidRecipeState state = fluidState(context, main);
        List<FluidStack> simulated = copyFluids(state.fluids);
        return applyFluidPredicate(simulated, predicate);
    }

    public void snapshotFluidRecipe(InWorldRecipeContext context, HasCauldron predicate) {
        LargeCauldronBlockEntity main = this.getMainPart();
        FluidRecipeState state = fluidState(context, main);
        state.rollback.push(copyFluids(state.fluids));
        if (context.getLevel().getRandom().nextFloat() <= predicate.chance()) {
            applyFluidPredicate(state.fluids, predicate);
        }
    }

    public void rollbackFluidRecipe(InWorldRecipeContext context) {
        FluidRecipeState state = fluidState(context, this.getMainPart());
        if (!state.rollback.isEmpty()) state.fluids = state.rollback.pop();
    }

    public void clearFluidRecipeStack(InWorldRecipeContext context) {
        fluidState(context, this.getMainPart()).rollback.clear();
    }

    public void acceptFluidRecipe(InWorldRecipeContext context) {
        context.putAcceptor(FLUID_RECIPE_ACCEPTOR, LargeCauldronBlockEntity::commitFluidRecipes);
    }

    private static FluidRecipeState fluidState(InWorldRecipeContext context, LargeCauldronBlockEntity cauldron) {
        Map<Long, FluidRecipeState> states = context.computeIfAbsent(FLUID_RECIPE_STATES);
        return states.computeIfAbsent(cauldron.worldPosition.asLong(), ignored ->
            new FluidRecipeState(cauldron.fluids.copyFluids()));
    }

    private static void commitFluidRecipes(InWorldRecipeContext context) {
        Map<Long, FluidRecipeState> states = context.computeIfAbsent(FLUID_RECIPE_STATES);
        for (Map.Entry<Long, FluidRecipeState> entry : states.entrySet()) {
            BlockEntity entity = context.getLevel().getBlockEntity(BlockPos.of(entry.getKey()));
            if (entity instanceof LargeCauldronBlockEntity cauldron) {
                cauldron.getMainPart().fluids.setFluids(entry.getValue().fluids);
            }
        }
    }

    private static boolean applyFluidPredicate(List<FluidStack> fluids, HasCauldron predicate) {
        int source = findSourceTank(fluids, predicate);
        if (predicate.fluid().equals(HasCauldron.EMPTY) && source < 0) return false;
        if (predicate.hasCheck() && !predicate.fluid().equals(HasCauldron.EMPTY) && source < 0) return false;
        int sourceAmount = source < 0 ? 0 : fluids.get(source).getAmount();
        if (predicate.consume() > sourceAmount) return false;

        ResourceLocation sourceId = source < 0
            ? null
            : BuiltInRegistries.FLUID.getKey(fluids.get(source).getFluid());
        ResourceLocation targetId = HasCauldron.isNotEmpty(predicate.transform())
            ? predicate.transform()
            : sourceId != null ? sourceId : HasCauldron.isNotEmpty(predicate.fluid()) ? predicate.fluid() : null;

        if (predicate.consume() == 0 && predicate.produce() == 0) {
            if (source < 0 || targetId == null || targetId.equals(sourceId)) return true;
            int target = findTank(fluids, targetId);
            int targetAmount = target < 0 ? 0 : fluids.get(target).getAmount();
            if (targetAmount + sourceAmount > LargeCauldronFluidHandler.TANK_CAPACITY) return false;
            if (target < 0) target = findEmptyTankAfterRemoving(fluids, source);
            if (target < 0) return false;
            FluidStack transformed = fluids.get(target).isEmpty()
                ? new FluidStack(BuiltInRegistries.FLUID.get(targetId), targetAmount + sourceAmount)
                : fluids.get(target).copyWithAmount(targetAmount + sourceAmount);
            fluids.set(source, FluidStack.EMPTY);
            fluids.set(target, transformed);
            return true;
        }

        if (source >= 0 && predicate.consume() > 0) {
            int remaining = sourceAmount - predicate.consume();
            fluids.set(source, remaining == 0 ? FluidStack.EMPTY : fluids.get(source).copyWithAmount(remaining));
        }
        if (predicate.produce() == 0) return true;
        if (targetId == null) return false;

        int target = findTank(fluids, targetId);
        int targetAmount = target < 0 ? 0 : fluids.get(target).getAmount();
        if (targetAmount + predicate.produce() > LargeCauldronFluidHandler.TANK_CAPACITY) return false;
        if (target < 0) target = findEmptyTank(fluids);
        if (target < 0) return false;
        FluidStack produced = fluids.get(target).isEmpty()
            ? new FluidStack(BuiltInRegistries.FLUID.get(targetId), targetAmount + predicate.produce())
            : fluids.get(target).copyWithAmount(targetAmount + predicate.produce());
        fluids.set(target, produced);
        return true;
    }

    private static int findSourceTank(List<FluidStack> fluids, HasCauldron predicate) {
        if (predicate.fluid().equals(HasCauldron.EMPTY)) {
            for (FluidStack fluid : fluids) {
                if (!fluid.isEmpty()) return -1;
            }
            return 0;
        }
        if (!predicate.hasCheck()) return -1;
        for (int i = 0; i < fluids.size(); i++) {
            FluidStack fluid = fluids.get(i);
            if (fluid.isEmpty()) continue;
            ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
            if (predicate.matchesFluid(id)) return i;
        }
        return -1;
    }

    private static int findTank(List<FluidStack> fluids, ResourceLocation id) {
        for (int i = 0; i < fluids.size(); i++) {
            FluidStack fluid = fluids.get(i);
            if (!fluid.isEmpty() && BuiltInRegistries.FLUID.getKey(fluid.getFluid()).equals(id)) return i;
        }
        return -1;
    }

    private static int findEmptyTank(List<FluidStack> fluids) {
        for (int i = 0; i < fluids.size(); i++) {
            if (fluids.get(i).isEmpty()) return i;
        }
        return -1;
    }

    private static int findEmptyTankAfterRemoving(List<FluidStack> fluids, int source) {
        int empty = findEmptyTank(fluids);
        return empty >= 0 ? empty : source;
    }

    private static List<FluidStack> copyFluids(List<FluidStack> fluids) {
        List<FluidStack> result = new ArrayList<>(fluids.size());
        for (FluidStack fluid : fluids) result.add(fluid.copy());
        return result;
    }

    private record RecipeExecution(boolean executed, boolean damageAnvil) {
    }

    private record RecipeAttempt(RecipeExecution execution, @Nullable BlockPos helper) {
        private static final RecipeAttempt EMPTY = new RecipeAttempt(new RecipeExecution(false, false), null);
    }

    public record RecipePreview(
        int inputSlot,
        ResourceLocation recipeId,
        String categoryPath,
        List<HasCauldron> fluidPredicates
    ) {
    }

    private static class PreviewState {
        private List<ItemStack> items;
        private final int primarySlot;
        private Set<Integer> usedItemSlots;
        private List<FluidStack> fluids;
        private List<HasCauldron> fluidPredicates;

        private PreviewState(
            List<ItemStack> items,
            int primarySlot,
            Set<Integer> usedItemSlots,
            List<FluidStack> fluids,
            List<HasCauldron> fluidPredicates
        ) {
            this.items = items;
            this.primarySlot = primarySlot;
            this.usedItemSlots = usedItemSlots;
            this.fluids = fluids;
            this.fluidPredicates = fluidPredicates;
        }

        private PreviewState copy() {
            List<ItemStack> copiedItems = new ArrayList<>(this.items.size());
            for (ItemStack item : this.items) copiedItems.add(item.copy());
            return new PreviewState(
                copiedItems,
                this.primarySlot,
                new HashSet<>(this.usedItemSlots),
                copyFluids(this.fluids),
                new ArrayList<>(this.fluidPredicates)
            );
        }

        private void copyFrom(PreviewState other) {
            this.items = other.items;
            this.usedItemSlots = other.usedItemSlots;
            this.fluids = other.fluids;
            this.fluidPredicates = other.fluidPredicates;
        }

        private int findItem(HasItemIngredient ingredient) {
            if (ingredient.getItem().test(this.items.get(this.primarySlot))) return this.primarySlot;
            for (int slot = 0; slot < this.items.size(); slot++) {
                if (slot != this.primarySlot && ingredient.getItem().test(this.items.get(slot))) return slot;
            }
            return -1;
        }
    }

    private static class FluidRecipeState {
        private List<FluidStack> fluids;
        private final Deque<List<FluidStack>> rollback = new ArrayDeque<>();

        private FluidRecipeState(List<FluidStack> fluids) {
            this.fluids = copyFluids(fluids);
        }
    }

    private class SelectedInputHandler implements IItemHandlerModifiable {
        private IItemHandlerModifiable delegate() {
            return LargeCauldronBlockEntity.this.processingOutput
                ? LargeCauldronBlockEntity.this.output
                : LargeCauldronBlockEntity.this.input;
        }

        @Override
        public int getSlots() {
            return this.delegate().getSlots();
        }

        private int slot(int viewSlot) {
            int selected = LargeCauldronBlockEntity.this.processingSlot;
            if (selected < 0 || selected >= this.getSlots()) return viewSlot;
            if (viewSlot == 0) return selected;
            int unselected = viewSlot - 1;
            return unselected >= selected ? unselected + 1 : unselected;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return this.delegate().getStackInSlot(this.slot(slot));
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            this.delegate().setStackInSlot(this.slot(slot), stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return this.delegate().insertItem(this.slot(slot), stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return this.delegate().extractItem(this.slot(slot), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return this.delegate().getSlotLimit(this.slot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return this.delegate().isItemValid(this.slot(slot), stack);
        }
    }

    private static class PreferredInputHandler implements IItemHandler {
        private final LargeCauldronInputHandler delegate;
        private final int[] order;
        private final int extractSlot;

        private PreferredInputHandler(LargeCauldronInputHandler delegate, int preferred) {
            this(delegate, preferred, false);
        }

        private PreferredInputHandler(LargeCauldronInputHandler delegate, int preferred, boolean extractPreferred) {
            this.delegate = delegate;
            this.extractSlot = extractPreferred ? preferred : -1;
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < delegate.getSlots(); i++) slots.add(i);
            slots.sort(Comparator
                .comparingInt((Integer slot) -> distanceFromPreferred(slot, preferred))
                .thenComparingInt(Integer::intValue));
            this.order = slots.stream().mapToInt(Integer::intValue).toArray();
        }

        private static int distanceFromPreferred(int slot, int preferred) {
            int preferredX = preferred < 0 ? 0 : INPUT_SLOT_OFFSETS[preferred][0];
            int preferredZ = preferred < 0 ? 0 : INPUT_SLOT_OFFSETS[preferred][1];
            int dx = INPUT_SLOT_OFFSETS[slot][0] - preferredX;
            int dz = INPUT_SLOT_OFFSETS[slot][1] - preferredZ;
            return dx * dx + dz * dz;
        }

        @Override
        public int getSlots() {
            return this.order.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return this.delegate.getStackInSlot(this.order[slot]);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return this.delegate.insertItem(this.order[slot], stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            int delegateSlot = this.order[slot];
            return delegateSlot == this.extractSlot
                ? this.delegate.extractItem(delegateSlot, amount, simulate)
                : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return this.delegate.getSlotLimit(this.order[slot]);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return this.delegate.isItemValid(this.order[slot], stack);
        }
    }

    private record OutputOnlyHandler(IItemHandler delegate) implements IItemHandler {
        @Override
        public int getSlots() {
            return this.delegate.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return this.delegate.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return this.delegate.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return this.delegate.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }

    private static class EmptyItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 0;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }
}
