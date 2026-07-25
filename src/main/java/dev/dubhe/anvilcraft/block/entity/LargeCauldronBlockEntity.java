package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.cache.ItemResourceHandlerCache;
import dev.anvilcraft.lib.v2.recipe.event.InWorldRecipeEvent;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.predicate.block.HasBlockBase;
import dev.anvilcraft.lib.v2.recipe.predicate.item.HasItemIngredient;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeData;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeManager;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.LargeCauldronInputHandler;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.power.consumer.HeaterBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.workstation.BurningHeaterBlock;
import dev.dubhe.anvilcraft.block.workstation.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.block.workstation.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.workstation.NeutronIrradiatorBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluidTags;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTriggers;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.FluidMixingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.DamageAnvil;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasAnvil;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import dev.dubhe.anvilcraft.util.FireReforgingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
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
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

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
    implements IItemResourceHandlerHolder, ItemResourceHandlerCache, IFluidResourceHandlerHolder {
    public static final int OUTPUT_SLOTS = 32;
    public static final int MAX_PROCESS_EFFICIENCY = 9;
    private static final int[][] INPUT_SLOT_OFFSETS = {
        {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };
    private static final int[][] FOOTPRINT_OFFSETS = {
        {0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };
    private static final double CONTENT_HEIGHT = 2.25;
    private static final ResourceHandler<ItemResource> EMPTY_HANDLER = new EmptyItemHandler();
    private static final InWorldRecipeData<Map<Long, FluidRecipeState>> FLUID_RECIPE_STATES =
        InWorldRecipeData.of(AnvilCraft.of("large_cauldron_fluid_states"), (context, key) -> new HashMap<>());
    private static final Identifier FLUID_RECIPE_ACCEPTOR = AnvilCraft.of("large_cauldron_fluid_acceptor");

    private final LargeCauldronInputHandler input = new LargeCauldronInputHandler(this::contentsChanged);
    private final ItemStacksResourceHandler output = new ItemStacksResourceHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot, ItemStack previousContents) {
            LargeCauldronBlockEntity.this.contentsChanged();
        }
    };
    private final LargeCauldronFluidHandler fluids = new LargeCauldronFluidHandler(this::contentsChanged);
    private final ResourceHandler<ItemResource> selectedInput = new SelectedInputHandler();
    private final Set<Integer> processingOutputInputs = new HashSet<>();
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
        entity.absorbFluidSources(level);
        entity.refreshIgnited();
        entity.applyFluidEffects((ServerLevel) level);
        entity.hurtEntitiesInsideFromCampfire((ServerLevel) level);
        entity.reforgeItemsInLava(level);
    }

    private void absorbFluidSources(Level level) {
        BlockPos intakeCenter = this.worldPosition.above(2);
        for (int slot = 0; slot < FOOTPRINT_OFFSETS.length; slot++) {
            BlockPos sourcePos = positionForFootprint(intakeCenter, slot);
            FluidState fluidState = level.getFluidState(sourcePos);
            if (fluidState.isEmpty() || !fluidState.isSource()) continue;
            BlockState sourceState = level.getBlockState(sourcePos);
            if (!(sourceState.getBlock() instanceof BucketPickup bucketPickup)) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = this.fluids.insert(
                    FluidResource.of(fluidState.getType()),
                    FluidType.BUCKET_VOLUME,
                    transaction
                );
                if (inserted != FluidType.BUCKET_VOLUME) continue;
                if (bucketPickup.pickupBlock(null, level, sourcePos, sourceState).isEmpty()) continue;
                transaction.commit();
            }
        }
    }

    @Override
    public ResourceHandler<ItemResource> getItemHandler() {
        return this.getMainPart().output;
    }

    @Override
    public ResourceHandler<ItemResource> getInput() {
        LargeCauldronBlockEntity main = this.getMainPart();
        return main.processingSlot < 0 ? EMPTY_HANDLER : main.selectedInput;
    }

    @Override
    public ResourceHandler<ItemResource> getOutput() {
        LargeCauldronBlockEntity main = this.getMainPart();
        // Keep recipe input and output caches from snapshotting the same inventory during output reprocessing.
        return main.processingOutput ? EMPTY_HANDLER : main.output;
    }

    @Override
    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.getMainPart().fluids;
    }

    public LargeCauldronInputHandler getInputHandler() {
        return this.getMainPart().input;
    }

    public ItemStacksResourceHandler getOutputHandler() {
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

    public ResourceHandler<ItemResource> getAutomationItemHandler(@Nullable Direction side) {
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

    public ResourceHandler<FluidResource> getAutomationFluidHandler(@Nullable Direction side) {
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
        ResourceHandler<FluidResource> handler = main.fluids.sideAccess(accessible);
        try (Transaction transaction = Transaction.openRoot()) {
            boolean success = FluidUtil.interactWithFluidHandler(
                player,
                hand,
                this.worldPosition,
                handler,
                transaction
            );
            if (success) transaction.commit();
            return success;
        }
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
        ItemStack remainder = insertItem(new PreferredInputHandler(this.input, preferredSlot), held.copy());
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
            for (int slot = 0; slot < main.output.size(); slot++) {
                ItemStack stack;
                while (!(stack = extractItem(main.output, slot, Integer.MAX_VALUE)).isEmpty()) {
                    extracted.add(stack);
                }
            }
        } else {
            ItemStack stack;
            while (!(stack = extractItem(main.input, inputSlot, Integer.MAX_VALUE)).isEmpty()) {
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
        for (int slot = 0; slot < this.output.size(); slot++) {
            if (!getStack(this.output, slot).isEmpty()) return false;
        }
        return true;
    }

    public void absorbItem(ItemEntity entity, int preferredSlot) {
        if (!entity.anvilcraft$isAdsorbable() || entity.isRemoved()) return;
        ItemStack stack = entity.getItem();
        ItemStack remainder = insertItem(
            new PreferredInputHandler(this.getMainPart().input, preferredSlot),
            stack.copy()
        );
        if (remainder.getCount() == stack.getCount()) return;
        if (remainder.isEmpty()) {
            entity.discard();
        } else {
            entity.setItem(remainder);
        }
    }

    public ItemStack insertRecipeOutput(ItemStack stack) {
        LargeCauldronBlockEntity main = this.getMainPart();
        if (!main.processingOutput) return insertItem(main.output, stack);
        ItemStack remainder = stack;
        for (int slot = 0; slot < main.output.size() && !remainder.isEmpty(); slot++) {
            if (main.processingOutputInputs.contains(slot)) continue;
            remainder = insertItem(main.output, slot, remainder);
        }
        return remainder;
    }

    public boolean hasInputMatching(java.util.function.Predicate<ItemStack> predicate) {
        LargeCauldronInputHandler handler = this.getMainPart().input;
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty() && predicate.test(stack)) return true;
        }
        return false;
    }

    public void dropContents() {
        LargeCauldronBlockEntity main = this.getMainPart();
        if (main.droppedContents || main.level == null) return;
        main.droppedContents = true;
        for (int slot = 0; slot < main.input.size(); slot++) {
            ItemStack stack;
            while (!(stack = extractItem(main.input, slot, Integer.MAX_VALUE)).isEmpty()) {
                Block.popResource(main.level, main.worldPosition, stack);
            }
        }
        for (int slot = 0; slot < main.output.size(); slot++) {
            ItemStack stack = extractItem(main.output, slot, Integer.MAX_VALUE);
            if (!stack.isEmpty()) Block.popResource(main.level, main.worldPosition, stack);
        }
    }

    public int getLightLevel() {
        LargeCauldronBlockEntity main = this.getMainPart();
        if (main.ignited) return 15;
        LargeCauldronFluidHandler handler = main.fluids;
        int maxLight = 0;
        for (int i = 0; i < handler.size(); i++) {
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.isMainPart()) return;
        this.input.serialize(output.child("Inputs"));
        this.output.serialize(output.child("Outputs"));
        this.fluids.serialize(output.child("Fluids"));
        output.putBoolean("Ignited", this.ignited);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        if (!this.isMainPart()) return;
        this.input.deserialize(input.childOrEmpty("Inputs"));
        this.output.deserialize(input.childOrEmpty("Outputs"));
        this.fluids.deserialize(input.childOrEmpty("Fluids"));
        this.ignited = input.getBooleanOr("Ignited", false) && this.canIgniteTopFluid();
        this.recipePreviewGameTime = Long.MIN_VALUE;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (this.isMainPart()) {
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
            this.input.serialize(output.child("Inputs"));
            this.output.serialize(output.child("Outputs"));
            this.fluids.serialize(output.child("Fluids"));
            tag.merge(output.buildResult());
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
        List<RecipePass> itemRecipePasses = helpers.isEmpty()
            ? List.of(RecipePass.ALL)
            : List.of(RecipePass.NON_COMPRESSION, RecipePass.COMPRESSION_ONLY);
        List<FluidStack> initialFluids = main.fluids.copyFluids();
        List<Integer> initialOutputSlots = new ArrayList<>();
        for (int slot = 0; slot < main.output.size(); slot++) {
            if (!getStack(main.output, slot).isEmpty()) initialOutputSlots.add(slot);
        }
        int processed = 0;
        for (RecipePass recipePass : itemRecipePasses) {
            boolean madeProgress;
            do {
                madeProgress = false;
                // Slot order must not override recipe priority when ingredients occupy different cauldron cells.
                for (int slot : orderedInputSlots(serverLevel, main.input, recipePass)) {
                    if (processed >= MAX_PROCESS_EFFICIENCY) break;
                    BlockPos slotPos = positionForInputSlot(base, slot);
                    List<BlockPos> candidates = helpers.isEmpty()
                        ? List.of(slotPos.below())
                        : orderedHelpers(slotPos, helpers, base);
                    RecipeExecution execution = main.tryProcessItemGroup(
                        serverLevel,
                        base,
                        candidates,
                        event.getEntity(),
                        slot,
                        false,
                        recipePass
                    );
                    if (!execution.executed()) continue;
                    if (execution.damageAnvil()) event.setAnvilDamage(true);
                    processed++;
                    madeProgress = true;
                }
            } while (madeProgress && processed < MAX_PROCESS_EFFICIENCY);
            if (processed >= MAX_PROCESS_EFFICIENCY) break;
        }

        List<BlockPos> centerCandidates = helpers.isEmpty()
            ? List.of(base.below())
            : orderedHelpers(base, helpers, base);
        for (RecipePass recipePass : itemRecipePasses) {
            for (int slot : initialOutputSlots) {
                if (processed >= MAX_PROCESS_EFFICIENCY) break;
                if (getStack(main.output, slot).isEmpty()) continue;
                RecipeExecution execution = main.tryProcessItemGroup(
                    serverLevel,
                    base,
                    centerCandidates,
                    event.getEntity(),
                    slot,
                    true,
                    recipePass
                );
                if (!execution.executed()) continue;
                if (execution.damageAnvil()) event.setAnvilDamage(true);
                processed++;
            }
            if (processed >= MAX_PROCESS_EFFICIENCY) break;
        }

        if (sameFluids(initialFluids, main.fluids.copyFluids())) {
            while (processed < MAX_PROCESS_EFFICIENCY) {
                if (main.tryProcessFluidMixingRecipe(serverLevel)) {
                    processed++;
                    continue;
                }
                RecipeExecution execution = main.tryProcessFluidRecipe(
                    serverLevel,
                    base,
                    centerCandidates,
                    event.getEntity(),
                    RecipePass.NON_COMPRESSION
                );
                if (!execution.executed()) break;
                if (execution.damageAnvil()) event.setAnvilDamage(true);
                processed++;
            }
        }
        return true;
    }

    private boolean tryProcessFluidMixingRecipe(ServerLevel level) {
        List<FluidStack> storedFluids = this.fluids.copyFluids();
        for (RecipeHolder<FluidMixingRecipe> holder
            : RecipesRecord.getRecipes(level).byType(ModRecipeTypes.FLUID_MIXING.get())) {
            FluidMixingRecipe recipe = holder.value();
            var remainingFluids = recipe.consume(storedFluids);
            if (remainingFluids.isEmpty()) continue;

            LargeCauldronFluidHandler simulatedFluids = new LargeCauldronFluidHandler(() -> {});
            simulatedFluids.setFluids(remainingFluids.get());
            boolean fluidsFit = true;
            for (FluidStack result : recipe.getFluidResults()) {
                int filled = insertFluid(simulatedFluids, result);
                if (filled != result.getAmount()) {
                    fluidsFit = false;
                    break;
                }
            }
            if (!fluidsFit) continue;

            ItemStacksResourceHandler simulatedOutput = new ItemStacksResourceHandler(this.output.size());
            for (int slot = 0; slot < this.output.size(); slot++) {
                ItemStack stack = getStack(this.output, slot);
                simulatedOutput.set(slot, ItemResource.of(stack), stack.getCount());
            }
            boolean fits = true;
            for (ItemStack result : recipe.getItemResults()) {
                if (!insertItem(simulatedOutput, result.copy()).isEmpty()) {
                    fits = false;
                    break;
                }
            }
            if (!fits) continue;

            this.fluids.setFluids(simulatedFluids.copyFluids());
            for (ItemStack result : recipe.getItemResults()) {
                insertItem(this.output, result.copy());
            }
            return true;
        }
        return false;
    }

    private RecipeExecution tryProcessItemGroup(
        ServerLevel level,
        BlockPos base,
        List<BlockPos> candidates,
        Entity anvil,
        int slot,
        boolean outputSource,
        RecipePass recipePass
    ) {
        for (BlockPos helper : candidates) {
            final Vec3 contextPos = new Vec3(helper.getX() + 0.5, base.getY() + 1.0, helper.getZ() + 0.5);
            this.processingSlot = slot;
            this.processingOutput = outputSource;
            this.processingOutputInputs.clear();
            if (outputSource) {
                for (int outputSlot = 0; outputSlot < this.output.size(); outputSlot++) {
                    if (!getStack(this.output, outputSlot).isEmpty()) {
                        this.processingOutputInputs.add(outputSlot);
                    }
                }
            }
            RecipeExecution execution;
            try {
                execution = this.triggerRecipeGroup(
                    level,
                    contextPos,
                    anvil,
                    slot,
                    outputSource,
                    recipePass
                );
            } finally {
                this.processingSlot = -1;
                this.processingOutput = false;
                this.processingOutputInputs.clear();
            }
            if (execution.executed()) return execution;
        }
        return RecipeExecution.EMPTY;
    }

    private RecipeExecution tryProcessFluidRecipe(
        ServerLevel level,
        BlockPos base,
        List<BlockPos> candidates,
        Entity anvil,
        RecipePass recipePass
    ) {
        for (BlockPos helper : candidates) {
            Vec3 contextPos = new Vec3(helper.getX() + 0.5, base.getY() + 1.0, helper.getZ() + 0.5);
            InWorldRecipeContext context = new InWorldRecipeContext(level, contextPos, anvil);
            RecipeExecution execution = triggerOneRecipe(level, context, ItemStack.EMPTY, recipePass);
            if (execution.executed()) return execution;
        }
        return RecipeExecution.EMPTY;
    }

    private RecipeExecution triggerRecipeGroup(
        ServerLevel level,
        Vec3 contextPos,
        Entity anvil,
        int slot,
        boolean outputSource,
        RecipePass recipePass
    ) {
        ResourceHandler<ItemResource> source = outputSource ? this.output : this.input;
        ItemStack starting = getStack(source, slot);
        if (starting.isEmpty()) return new RecipeExecution(false, false);
        int itemBudget = starting.getMaxStackSize();
        int consumed = 0;
        boolean executed = false;
        boolean damageAnvil = false;
        while (consumed < itemBudget) {
            ItemStack processingInput = getStack(source, slot);
            if (processingInput.isEmpty()) break;
            InWorldRecipeContext context = new InWorldRecipeContext(level, contextPos, anvil);
            RecipeExecution current = triggerOneRecipe(
                level,
                context,
                processingInput,
                recipePass
            );
            if (!current.executed()) break;
            executed = true;
            damageAnvil |= current.damageAnvil();
            int consumedNow = processingInput.getCount() - getStack(source, slot).getCount();
            if (consumedNow <= 0) break;
            consumed += consumedNow;
        }
        return new RecipeExecution(executed, damageAnvil);
    }

    private static RecipeExecution triggerOneRecipe(
        ServerLevel level,
        InWorldRecipeContext context,
        ItemStack processingInput,
        RecipePass recipePass
    ) {
        InWorldRecipeManager manager = level.getServer().getRecipeManager().anvillib$getInWorldRecipeManager();
        for (RecipeHolder<InWorldRecipe> holder : manager.recipeHolders.get(ModRecipeTriggers.ON_ANVIL_FALL_ON.get())) {
            InWorldRecipe recipe = holder.value();
            if (!recipePass.accepts(recipe)) continue;
            if (processingInput.isEmpty()) {
                if (!isFluidOnlyRecipe(recipe)) continue;
            } else if (!recipeAnchoredByInput(recipe, processingInput)) {
                continue;
            }
            if (!recipe.matches(context, level)) continue;
            recipe.assemble(context);
            NeoForge.EVENT_BUS.post(
                new InWorldRecipeEvent(recipe.getType(), holder.id().identifier(), recipe, context)
            );
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

    /// 按可匹配配方的最高优先级排列非空输入槽，避免槽位顺序盖过配方优先级
    private static List<Integer> orderedInputSlots(
        ServerLevel level,
        ResourceHandler<ItemResource> input,
        RecipePass recipePass
    ) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < input.size(); slot++) {
            if (!getStack(input, slot).isEmpty()) slots.add(slot);
        }
        InWorldRecipeManager manager = level.recipeAccess().anvillib$getInWorldRecipeManager();
        slots.sort(
            Comparator.comparingInt((Integer slot) -> highestAnchoredRecipePriority(
                manager,
                getStack(input, slot),
                recipePass
            )).reversed().thenComparingInt(Integer::intValue)
        );
        return slots;
    }

    private static int highestAnchoredRecipePriority(
        InWorldRecipeManager manager,
        ItemStack stack,
        RecipePass recipePass
    ) {
        int priority = Integer.MIN_VALUE;
        for (RecipeHolder<InWorldRecipe> holder
            : manager.recipeHolders.get(ModRecipeTriggers.ON_ANVIL_FALL_ON.get())) {
            InWorldRecipe recipe = holder.value();
            if (!recipePass.accepts(recipe)) continue;
            if (recipeAnchoredByInput(recipe, stack)) priority = Math.max(priority, recipe.priority());
        }
        return priority;
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

        BlockPos base = this.worldPosition.below();
        List<BlockPos> helpers = this.findActiveHelpers(base);
        boolean itemCompressionLast = !helpers.isEmpty();
        List<RecipeHolder<InWorldRecipe>> recipes = this.getPreviewRecipes(itemCompressionLast);
        if (recipes.isEmpty()) {
            this.recipePreviewGameTime = gameTime;
            this.recipePreviewCache = List.of();
            return this.recipePreviewCache;
        }

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
                    this.input,
                    slot,
                    false,
                    helper,
                    base
                );
                if (preview == null) continue;
                previews.add(preview);
                break;
            }
        }
        List<BlockPos> centerCandidates = helpers.isEmpty()
            ? List.of(base.below())
            : orderedHelpers(base, helpers, base);
        for (int slot = 0; slot < this.output.size(); slot++) {
            if (getStack(this.output, slot).isEmpty()) continue;
            for (BlockPos helper : centerCandidates) {
                RecipePreview preview = this.previewFirstRecipe(
                    recipes,
                    this.output,
                    slot,
                    true,
                    helper,
                    base
                );
                if (preview == null) continue;
                previews.add(preview);
                break;
            }
        }
        for (BlockPos helper : centerCandidates) {
            RecipePreview preview = this.previewFirstFluidRecipe(
                recipes,
                helper,
                base
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
    private List<RecipeHolder<InWorldRecipe>> getPreviewRecipes(boolean itemCompressionLast) {
        List<RecipeHolder<InWorldRecipe>> recipes = new ArrayList<>();
        for (RecipeHolder<?> holder : RecipesRecord.getRecipes(this.level).values()) {
            if (!(holder.value() instanceof InWorldRecipe recipe)) continue;
            if (!recipe.trigger().equals(ModRecipeTriggers.ON_ANVIL_FALL_ON.get())) continue;
            recipes.add((RecipeHolder<InWorldRecipe>) (RecipeHolder<?>) holder);
        }
        recipes.sort(
            Comparator.<RecipeHolder<InWorldRecipe>>comparingInt(
                holder -> itemCompressionLast && holder.value() instanceof ItemCompressRecipe ? 1 : 0
            ).thenComparing(
                Comparator.comparingInt((RecipeHolder<InWorldRecipe> holder) -> holder.value().priority())
                    .reversed()
            )
                .thenComparing(holder -> holder.id().toString())
        );
        return recipes;
    }

    private @Nullable RecipePreview previewFirstRecipe(
        Collection<RecipeHolder<InWorldRecipe>> recipes,
        ResourceHandler<ItemResource> source,
        int slot,
        boolean outputSource,
        BlockPos helper,
        BlockPos base
    ) {
        Vec3 contextPos = new Vec3(helper.getX() + 0.5, base.getY() + 1.0, helper.getZ() + 0.5);
        for (RecipeHolder<InWorldRecipe> holder : recipes) {
            InWorldRecipe recipe = holder.value();
            if (!recipeUsesInput(recipe, getStack(source, slot))) continue;
            PreviewState state = new PreviewState(
                copyItemStacks(source),
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
                outputSource ? -1 : slot,
                outputSource ? slot : -1,
                holder.id().identifier(),
                categoryPath(holder),
                List.copyOf(state.fluidPredicates)
            );
        }
        return null;
    }

    private @Nullable RecipePreview previewFirstFluidRecipe(
        Collection<RecipeHolder<InWorldRecipe>> recipes,
        BlockPos helper,
        BlockPos base
    ) {
        Vec3 contextPos = new Vec3(helper.getX() + 0.5, base.getY() + 1.0, helper.getZ() + 0.5);
        for (RecipeHolder<InWorldRecipe> holder : recipes) {
            InWorldRecipe recipe = holder.value();
            if (!isFluidOnlyRecipe(recipe)) continue;
            PreviewState state = new PreviewState(
                copyItemStacks(this.input),
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
                -1,
                holder.id().identifier(),
                categoryPath(holder),
                List.copyOf(state.fluidPredicates)
            );
        }
        return null;
    }

    private static List<ItemStack> copyItemStacks(ResourceHandler<ItemResource> source) {
        List<ItemStack> result = new ArrayList<>(source.size());
        for (int slot = 0; slot < source.size(); slot++) {
            result.add(getStack(source, slot));
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
                BlockState giantAnvil = ModBlocks.GIANT_ANVIL.getDefaultState();
                boolean matches = anvil.anvil()
                    .map(anvilPredicate -> anvilPredicate.test(this.level, giantAnvil, null))
                    .orElse(giantAnvil.is(BlockTags.ANVIL));
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
        Identifier typeId = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
        if (typeId != null && !typeId.getPath().equals("in_world_recipe")) return typeId.getPath();
        String path = holder.id().identifier().getPath();
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
                    extractFluid(this.fluids, extinguishingFluid, 250);
                }
            }
        }
    }

    private void reforgeItemsInLava(Level level) {
        boolean hasLava = false;
        for (int tank = 0; tank < this.fluids.size(); tank++) {
            if (this.fluids.getFluidInTank(tank).is(Fluids.LAVA)) {
                hasLava = true;
                break;
            }
        }
        if (!hasLava) return;

        for (int slot = 0; slot < this.input.size(); slot++) {
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
        for (int tank = this.fluids.size() - 1; tank >= 0; tank--) {
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
        for (int slot = 0; slot < this.input.size(); slot++) {
            ItemStack stack = this.input.getStackInSlot(slot);
            if (stack.is(ModItemTags.FIRE_STARTER)) {
                extractItem(this.input, slot, 1);
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
        return CauldronUtil.getInnerArea(this.worldPosition, this.getBlockState());
    }

    private void hurtEntitiesInsideFromCampfire(ServerLevel level) {
        BlockPos base = this.worldPosition.below();
        boolean normalCampfire = false;
        boolean soulCampfire = false;
        for (int slot = 0; slot < FOOTPRINT_OFFSETS.length; slot++) {
            BlockState state = level.getBlockState(positionForFootprint(base, slot).below());
            if (CampfireBlock.isLitCampfire(state)) {
                soulCampfire |= state.is(Blocks.SOUL_CAMPFIRE);
                normalCampfire |= state.is(Blocks.CAMPFIRE);
            }
        }
        if (!normalCampfire && !soulCampfire) return;

        AABB inside = this.contentArea();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, inside)) {
            if (living.fireImmune() || living.isSteppingCarefully()) continue;
            living.hurt(level.damageSources().inFire(), soulCampfire ? 2.0F : 1.0F);
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

        Identifier sourceId = source < 0
            ? null
            : BuiltInRegistries.FLUID.getKey(fluids.get(source).getFluid());
        Identifier targetId = HasCauldron.isNotEmpty(predicate.transform())
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
                ? new FluidStack(BuiltInRegistries.FLUID.getValue(targetId), targetAmount + sourceAmount)
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
            ? new FluidStack(BuiltInRegistries.FLUID.getValue(targetId), targetAmount + predicate.produce())
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
            Identifier id = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
            if (predicate.matchesFluid(id)) return i;
        }
        return -1;
    }

    private static int findTank(List<FluidStack> fluids, Identifier id) {
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

    private enum RecipePass {
        ALL,
        NON_COMPRESSION,
        COMPRESSION_ONLY;

        private boolean accepts(InWorldRecipe recipe) {
            boolean compression = recipe instanceof ItemCompressRecipe;
            return switch (this) {
                case ALL -> true;
                case NON_COMPRESSION -> !compression;
                case COMPRESSION_ONLY -> compression;
            };
        }
    }

    private record RecipeExecution(boolean executed, boolean damageAnvil) {
        private static final RecipeExecution EMPTY = new RecipeExecution(false, false);
    }

    public record RecipePreview(
        int inputSlot,
        int outputSlot,
        Identifier recipeId,
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

    private static ItemStack getStack(ResourceHandler<ItemResource> handler, int slot) {
        ItemResource resource = handler.getResource(slot);
        return resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(handler.getAmountAsInt(slot));
    }

    private static ItemStack insertItem(ResourceHandler<ItemResource> handler, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = handler.insert(ItemResource.of(stack), stack.getCount(), transaction);
            if (inserted > 0) transaction.commit();
            return inserted == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
        }
    }

    private static ItemStack insertItem(ResourceHandler<ItemResource> handler, int slot, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = handler.insert(slot, ItemResource.of(stack), stack.getCount(), transaction);
            if (inserted > 0) transaction.commit();
            return inserted == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
        }
    }

    private static ItemStack extractItem(ResourceHandler<ItemResource> handler, int slot, int amount) {
        ItemResource resource = handler.getResource(slot);
        if (resource.isEmpty()) return ItemStack.EMPTY;
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = handler.extract(slot, resource, amount, transaction);
            if (extracted > 0) transaction.commit();
            return extracted == 0 ? ItemStack.EMPTY : resource.toStack(extracted);
        }
    }

    private static int insertFluid(ResourceHandler<FluidResource> handler, FluidStack stack) {
        if (stack.isEmpty()) return 0;
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = handler.insert(FluidResource.of(stack), stack.getAmount(), transaction);
            if (inserted > 0) transaction.commit();
            return inserted;
        }
    }

    private static int extractFluid(ResourceHandler<FluidResource> handler, FluidStack stack, int amount) {
        if (stack.isEmpty()) return 0;
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = handler.extract(FluidResource.of(stack), amount, transaction);
            if (extracted > 0) transaction.commit();
            return extracted;
        }
    }

    private class SelectedInputHandler implements ResourceHandler<ItemResource> {
        private ResourceHandler<ItemResource> delegate() {
            return LargeCauldronBlockEntity.this.processingOutput
                ? LargeCauldronBlockEntity.this.output
                : LargeCauldronBlockEntity.this.input;
        }

        @Override
        public int size() {
            return this.delegate().size();
        }

        private int slot(int viewSlot) {
            int selected = LargeCauldronBlockEntity.this.processingSlot;
            if (selected < 0 || selected >= this.size()) return viewSlot;
            if (viewSlot == 0) return selected;
            int unselected = viewSlot - 1;
            return unselected >= selected ? unselected + 1 : unselected;
        }

        @Override
        public ItemResource getResource(int index) {
            return this.delegate().getResource(this.slot(index));
        }

        @Override
        public long getAmountAsLong(int index) {
            return this.delegate().getAmountAsLong(this.slot(index));
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return this.delegate().getCapacityAsLong(this.slot(index), resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return this.delegate().isValid(this.slot(index), resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return this.delegate().insert(this.slot(index), resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return this.delegate().extract(this.slot(index), resource, amount, transaction);
        }
    }

    private static class PreferredInputHandler implements ResourceHandler<ItemResource> {
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
            for (int i = 0; i < delegate.size(); i++) slots.add(i);
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
        public int size() {
            return this.order.length;
        }

        @Override
        public ItemResource getResource(int index) {
            return this.delegate.getResource(this.order[index]);
        }

        @Override
        public long getAmountAsLong(int index) {
            return this.delegate.getAmountAsLong(this.order[index]);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return this.delegate.getCapacityAsLong(this.order[index], resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return this.delegate.isValid(this.order[index], resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return this.delegate.insert(this.order[index], resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            int delegateSlot = this.order[index];
            return delegateSlot == this.extractSlot
                ? this.delegate.extract(delegateSlot, resource, amount, transaction)
                : 0;
        }
    }

    private record OutputOnlyHandler(ResourceHandler<ItemResource> delegate) implements ResourceHandler<ItemResource> {
        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public ItemResource getResource(int index) {
            return this.delegate.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            return this.delegate.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return this.delegate.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return false;
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return this.delegate.extract(index, resource, amount, transaction);
        }
    }

    private static class EmptyItemHandler implements ResourceHandler<ItemResource> {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public ItemResource getResource(int index) {
            return ItemResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            return 0;
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return 0;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return false;
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            return 0;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            return 0;
        }
    }
}
