package dev.dubhe.anvilcraft.block.entity;

import com.google.common.collect.EvictingQueue;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.model.CommandInfo;
import dev.dubhe.anvilcraft.recipe.multiblock.Multiblock4DRecipe;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;

public class SpacetimeSupercomputerBlockEntity extends BlockEntity implements IPowerConsumer {
    private static final int TICK_SPRINT_COUNTDOWN_SECONDS = 30;
    private static final int TICK_SPRINT_COUNTDOWN_TICKS = TICK_SPRINT_COUNTDOWN_SECONDS * 20;
    private static final String TICK_SPRINT_VOTE_SEPARATOR = "————————————";
    private static final Set<SpacetimeSupercomputerBlockEntity> PENDING_TICK_SPRINTS = new HashSet<>();

    @Getter
    @Setter
    private @Nullable PowerGrid grid;

    @Getter
    private String command = "";

    @Getter
    private final EvictingQueue<String> historyCommands = EvictingQueue.create(16);

    @Getter
    private float chargingProgress = 0;

    private @Nullable String pendingTickSprintCommand;
    private @Nullable UUID pendingTickSprintPlayer;
    private @Nullable UUID pendingTickSprintVoteId;
    private final Set<UUID> pendingTickSprintVoters = new HashSet<>();
    private final Set<UUID> confirmedTickSprintVoters = new HashSet<>();
    private int tickSprintCountdownTicks;

    @Getter
    private final List<CommandInfo> availableCommands = net.minecraft.Util.make(
        new ObjectArrayList<>(), (list) -> {
            list.add(new CommandInfo("/locate biome", AnvilCraft.CONFIG.spacetimeSupercomputerCommand.allowLocateBiomeCommand));
            list.add(new CommandInfo("/locate structure", AnvilCraft.CONFIG.spacetimeSupercomputerCommand.allowLocateStructureCommand));
            list.add(new CommandInfo("/locate poi", AnvilCraft.CONFIG.spacetimeSupercomputerCommand.allowLocatePoiCommand));
            list.add(new CommandInfo("/time add", AnvilCraft.CONFIG.spacetimeSupercomputerCommand.allowTimeAddCommand));
            list.add(new CommandInfo("/tick sprint", AnvilCraft.CONFIG.spacetimeSupercomputerCommand.allowTickSprintCommand));
        }
    );

    @Getter
    private @Nullable RecipeHolder<Multiblock4DRecipe> processingRecipe = null;
    @Getter
    private int processingStep = -1;
    @Getter
    private int processingSize = -1;
    private int processingTotal = -1;
    private final List<ItemStack> pendingDrops = new ArrayList<>();
    private @Nullable String pendingRecipeId;

    public SpacetimeSupercomputerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void setCommand(String command) {
        this.command = command;
        this.onChange();
    }

    public void addHistoryCommand(String command) {
        this.historyCommands.add(command);
        this.onChange();
    }

    public void onChange() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            if (this.level instanceof ServerLevel serverLevel) {
                Packet<?> packet = this.getUpdatePacket();
                if (packet != null) {
                    for (ServerPlayer serverPlayer : serverLevel.getChunkSource().chunkMap.getPlayers(
                        serverLevel.getChunkAt(this.getBlockPos()).getPos(), false
                    )) {
                        serverPlayer.connection.send(packet);
                    }
                }
            }
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.command.isBlank()) {
            tag.putString("command", this.command);
        }
        if (!this.historyCommands.isEmpty()) {
            ListTag historyTag = new ListTag();
            for (String historyCommand : this.historyCommands) {
                historyTag.add(StringTag.valueOf(historyCommand));
            }
            tag.put("historyCommands", historyTag);
        }
        if (this.chargingProgress > 0) {
            tag.putFloat("chargingProgress", this.chargingProgress);
        }
        if (this.processingRecipe != null) {
            CompoundTag processing = new CompoundTag();
            processing.putString("recipe", Objects.requireNonNull(this.processingRecipe.id()).toString());
            processing.putInt("step", this.processingStep);
            processing.putInt("size", this.processingSize);
            processing.putInt("total", this.processingTotal);
            tag.put("processing", processing);
        }
        if (!this.pendingDrops.isEmpty()) {
            ListTag pendingTag = new ListTag();
            for (ItemStack stack : this.pendingDrops) {
                if (!stack.isEmpty()) {
                    pendingTag.add(stack.save(registries));
                }
            }
            tag.put("pendingDrops", pendingTag);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("command")) {
            this.command = tag.getString("command");
        }
        if (tag.contains("historyCommands", Tag.TAG_LIST)) {
            ListTag tags = tag.getList("historyCommands", Tag.TAG_STRING);
            this.historyCommands.clear();
            for (Tag tag1 : tags) {
                if (tag1 instanceof StringTag stringTag) {
                    this.historyCommands.add(stringTag.getAsString());
                }
            }
        }
        if (tag.contains("chargingProgress")) {
            this.chargingProgress = tag.getFloat("chargingProgress");
        }
        if (tag.contains("processing")) {
            CompoundTag processing = tag.getCompound("processing");
            this.pendingRecipeId = processing.getString("recipe");
            this.processingStep = processing.getInt("step");
            this.processingSize = processing.getInt("size");
            this.processingTotal = processing.getInt("total");
        } else {
            // 服务端仅在处理中写入 processing：缺失即代表处理已结束，清除客户端残留状态。
            this.pendingRecipeId = null;
            this.processingRecipe = null;
            this.processingStep = -1;
            this.processingSize = -1;
            this.processingTotal = -1;
        }
        // 先加载 pendingDrops 再解析配方：解析失败时需要归还这些已消耗材料。
        if (tag.contains("pendingDrops", Tag.TAG_LIST)) {
            this.pendingDrops.clear();
            ListTag pendingTag = tag.getList("pendingDrops", Tag.TAG_COMPOUND);
            for (Tag tag1 : pendingTag) {
                if (tag1 instanceof CompoundTag compoundTag) {
                    ItemStack stack = ItemStack.parse(registries, compoundTag).orElse(ItemStack.EMPTY);
                    if (!stack.isEmpty()) {
                        this.pendingDrops.add(stack);
                    }
                }
            }
        }
        this.resolvePendingRecipe();
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        this.resolvePendingRecipe();
    }

    private void resolvePendingRecipe() {
        if (this.pendingRecipeId == null || this.level == null) {
            return;
        }
        String recipeId = this.pendingRecipeId;
        this.pendingRecipeId = null;
        this.processingRecipe = this.level.getRecipeManager().byKey(ResourceLocation.parse(recipeId))
            .filter(ref -> ref.value() instanceof Multiblock4DRecipe)
            .map(Util::<RecipeHolder<Multiblock4DRecipe>>cast)
            .orElse(null);
        if (this.processingRecipe == null) {
            // 配方已不存在（数据包被移除）：归还已消耗的材料。
            this.dropProcessingInputs();
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        // 与 saveAdditional 保持一致：仅在处理中写入 processing 子 tag，
        // 避免 recipe="" 让客户端 resolvePendingRecipe 执行 ResourceLocation.parse("") 抛异常。
        if (this.processingRecipe != null) {
            CompoundTag processing = new CompoundTag();
            processing.putString("recipe", Objects.requireNonNull(this.processingRecipe.id()).toString());
            processing.putInt("step", this.processingStep);
            processing.putInt("size", this.processingSize);
            processing.putInt("total", this.processingTotal);
            tag.put("processing", processing);
        }
        return tag;
    }

    @Override
    public void onDataPacket(
        Connection net,
        ClientboundBlockEntityDataPacket pkt,
        HolderLookup.Provider lookupProvider
    ) {
        super.onDataPacket(net, pkt, lookupProvider);
        // NeoForge 默认 onDataPacket 在空 tag 时会跳过 loadWithComponents，
        // 而合成完成时 getUpdateTag 不写入 processing 导致 tag 为空，
        // 客户端无法触发 loadAdditional 的清除分支，进度文本会停留在最后一步。
        if (!pkt.getTag().contains("processing")) {
            this.pendingRecipeId = null;
            this.processingRecipe = null;
            this.processingStep = -1;
            this.processingSize = -1;
            this.processingTotal = -1;
        }
    }

    public void runCommand(@Nullable Player player) {
        if (this.level == null) {
            return;
        }
        CommandSourceStack commandSourceStack = this.createCommandSource(player);
        String cmd = this.command;
        if (this.command.equalsIgnoreCase("Searge")) {
            if (player == null) {
                Objects.requireNonNull(this.level.getServer())
                    .getPlayerList()
                    .broadcastSystemMessage(Component.literal("#itzlipofutzli"), false);
            } else {
                player.sendSystemMessage(Component.literal("#itzlipofutzli"));
            }
            return;
        }
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        if (cmd.startsWith("locate") || cmd.startsWith("time add") || cmd.startsWith("tick sprint")) {
            if (this.chargingProgress >= 20f) {
                if (cmd.startsWith("time add")) {
                    int timeAddConsumeProcess = getTimeAddConsumeProcess(cmd);
                    if (this.chargingProgress >= 20f + timeAddConsumeProcess) {
                        Objects.requireNonNull(this.level.getServer())
                            .getCommands()
                            .performPrefixedCommand(commandSourceStack, this.command);
                        this.chargingProgress -= 20f + timeAddConsumeProcess;
                        this.addHistoryCommand(cmd);
                    } else {
                        if (player == null) {
                            Objects.requireNonNull(this.level.getServer()).getPlayerList().broadcastSystemMessage(
                                Component.translatable("block.anvilcraft.spacetime_supercomputer.insufficient_energy")
                                    .withStyle(ChatFormatting.RED), false
                            );
                        } else {
                            player.sendSystemMessage(Component.translatable("block.anvilcraft.spacetime_supercomputer.insufficient_energy")
                                .withStyle(ChatFormatting.RED));
                        }
                    }
                } else if (cmd.startsWith("tick sprint")) {
                    int tickSprintConsumeProcess = getTickSprintConsumeProcess(cmd);
                    if (this.chargingProgress >= 20f + tickSprintConsumeProcess) {
                        if (cmd.equals("tick sprint stop")) {
                            Objects.requireNonNull(this.level.getServer())
                                .getCommands()
                                .performPrefixedCommand(commandSourceStack, this.command);
                            this.chargingProgress -= 20f + tickSprintConsumeProcess;
                            this.addHistoryCommand(cmd);
                        } else {
                            this.startTickSprintCountdown(player, this.command);
                        }
                    } else {
                        this.sendCommandFailure(
                            player,
                            Component.translatable("block.anvilcraft.spacetime_supercomputer.insufficient_energy")
                        );
                    }
                } else if (cmd.startsWith("locate")) {
                    Objects.requireNonNull(this.level.getServer()).getCommands().performPrefixedCommand(commandSourceStack, this.command);
                    this.chargingProgress -= 20f;
                    this.addHistoryCommand(cmd);
                }
            } else {
                if (player == null) {
                    Objects.requireNonNull(this.level.getServer()).getPlayerList().broadcastSystemMessage(
                        Component.translatable("block.anvilcraft.spacetime_supercomputer.insufficient_energy")
                            .withStyle(ChatFormatting.RED), false
                    );
                } else {
                    player.sendSystemMessage(Component.translatable("block.anvilcraft.spacetime_supercomputer.insufficient_energy")
                        .withStyle(ChatFormatting.RED));
                }
            }
        } else {
            if (player == null) {
                Objects.requireNonNull(this.level.getServer()).getPlayerList().broadcastSystemMessage(
                    Component.translatable("block.anvilcraft.spacetime_supercomputer.no_supported_command").withStyle(ChatFormatting.RED),
                    false
                );
            } else {
                player.sendSystemMessage(Component.translatable("block.anvilcraft.spacetime_supercomputer.no_supported_command")
                    .withStyle(ChatFormatting.RED));
            }
        }
    }

    private CommandSourceStack createCommandSource(@Nullable Player player) {
        if (player != null) {
            return player.createCommandSourceStack().withPermission(4);
        }
        MinecraftServer server = Objects.requireNonNull(Objects.requireNonNull(this.level).getServer());
        ServerLevel serverLevel = (ServerLevel) this.level;
        return new CommandSourceStack(
            server,
            Vec3.atCenterOf(this.getBlockPos()),
            Vec2.ZERO,
            serverLevel,
            4,
            "Server",
            Component.literal("Server"),
            server,
            null
        ) {
            @Override
            public void sendSuccess(Supplier<Component> messageSupplier, boolean allowLogging) {
                super.sendSuccess(messageSupplier, allowLogging);
                boolean acceptsSuccess = this.source.acceptsSuccess() && !this.isSilent();
                boolean informsAdmins = allowLogging && this.source.shouldInformAdmins() && !this.isSilent();
                if (acceptsSuccess || informsAdmins) {
                    Component component = messageSupplier.get();
                    if (acceptsSuccess && this.source instanceof MinecraftServer minecraftServer) {
                        minecraftServer.getPlayerList().broadcastSystemMessage(component, false);
                    }
                }
            }

            @Override
            public void sendFailure(Component message) {
                if (this.source.acceptsFailure() && !this.isSilent()) {
                    if (this.source instanceof MinecraftServer minecraftServer) {
                        minecraftServer.getPlayerList()
                            .broadcastSystemMessage(Component.empty().append(message).withStyle(ChatFormatting.RED), false);
                    }
                    this.source.sendSystemMessage(Component.empty().append(message).withStyle(ChatFormatting.RED));
                }
            }
        };
    }

    private void startTickSprintCountdown(@Nullable Player player, String command) {
        MinecraftServer server = Objects.requireNonNull(this.level).getServer();
        if (server == null) {
            return;
        }
        if (this.pendingTickSprintCommand != null) {
            this.sendCommandFailure(
                player,
                Component.translatable("block.anvilcraft.spacetime_supercomputer.tick_sprint_countdown_in_progress")
            );
            return;
        }

        this.pendingTickSprintCommand = command;
        this.pendingTickSprintPlayer = player == null ? null : player.getUUID();
        this.pendingTickSprintVoteId = UUID.randomUUID();
        this.pendingTickSprintVoters.clear();
        this.confirmedTickSprintVoters.clear();
        this.tickSprintCountdownTicks = TICK_SPRINT_COUNTDOWN_TICKS;
        PENDING_TICK_SPRINTS.add(this);
        this.initializeTickSprintVoters(server);
        this.setChanged();
    }

    public static void cancelPendingTickSprints(MinecraftServer server) {
        for (SpacetimeSupercomputerBlockEntity supercomputer : List.copyOf(PENDING_TICK_SPRINTS)) {
            if (supercomputer.pendingTickSprintCommand != null
                && supercomputer.level != null
                && supercomputer.level.getServer() == server) {
                supercomputer.cancelTickSprintCountdown();
            }
        }
    }

    private void initializeTickSprintVoters(MinecraftServer server) {
        for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
            this.pendingTickSprintVoters.add(serverPlayer.getUUID());
            this.sendTickSprintVoteMessage(serverPlayer);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean updateTickSprintVoters(MinecraftServer server) {
        Set<UUID> onlinePlayers = new HashSet<>();
        for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
            UUID playerId = serverPlayer.getUUID();
            if (!this.pendingTickSprintVoters.contains(playerId)) {
                return false;
            }
            onlinePlayers.add(playerId);
        }
        this.pendingTickSprintVoters.retainAll(onlinePlayers);
        this.confirmedTickSprintVoters.retainAll(onlinePlayers);
        return true;
    }

    private void sendTickSprintVoteMessage(ServerPlayer player) {
        MutableComponent message = Component.empty()
            .append(Component.literal(TICK_SPRINT_VOTE_SEPARATOR).withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal("\n"))
            .append(Component.translatable("block.anvilcraft.spacetime_supercomputer.tick_sprint_confirmation"))
            .append(Component.literal("\n"))
            .append(this.createTickSprintVoteOption(true))
            .append(Component.literal("    "))
            .append(this.createTickSprintVoteOption(false))
            .append(Component.literal("\n"))
            .append(Component.literal(TICK_SPRINT_VOTE_SEPARATOR).withStyle(ChatFormatting.DARK_GRAY));
        player.sendSystemMessage(message);
    }

    private Component createTickSprintVoteOption(boolean accepted) {
        UUID voteId = Objects.requireNonNull(this.pendingTickSprintVoteId);
        ServerLevel serverLevel = (ServerLevel) Objects.requireNonNull(this.level);
        BlockPos pos = this.getBlockPos();
        String command = "/anvilcraft tick_sprint_vote %s %s %s %s %s %s".formatted(
            serverLevel.dimension().location(),
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            voteId,
            accepted ? "accept" : "reject"
        );
        String translationKey = accepted
            ? "block.anvilcraft.spacetime_supercomputer.tick_sprint_allow"
            : "block.anvilcraft.spacetime_supercomputer.tick_sprint_reject";
        ChatFormatting color = accepted ? ChatFormatting.GREEN : ChatFormatting.RED;
        return Component.translatable(translationKey).withStyle(style -> style
            .withColor(color)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    public boolean submitTickSprintVote(ServerPlayer player, UUID voteId, boolean accepted) {
        if (!voteId.equals(this.pendingTickSprintVoteId)
            || !this.pendingTickSprintVoters.contains(player.getUUID())) {
            return false;
        }
        MinecraftServer server = Objects.requireNonNull(this.level).getServer();
        if (server == null) {
            return false;
        }
        if (!this.updateTickSprintVoters(server)) {
            this.cancelTickSprintCountdown();
            return false;
        }
        if (accepted && !this.confirmedTickSprintVoters.add(player.getUUID())) {
            return false;
        }
        String feedbackKey = accepted
            ? "block.anvilcraft.spacetime_supercomputer.tick_sprint_allowed"
            : "block.anvilcraft.spacetime_supercomputer.tick_sprint_rejected";
        ChatFormatting feedbackColor = accepted ? ChatFormatting.GREEN : ChatFormatting.RED;
        player.sendSystemMessage(Component.translatable(feedbackKey).withStyle(feedbackColor));
        if (!accepted) {
            this.cancelTickSprintCountdown();
            return true;
        }
        if (this.allTickSprintVotersConfirmed()) {
            this.executePendingTickSprint();
        }
        return true;
    }

    private boolean allTickSprintVotersConfirmed() {
        return !this.pendingTickSprintVoters.isEmpty()
            && this.confirmedTickSprintVoters.containsAll(this.pendingTickSprintVoters);
    }

    private void tickTickSprintCountdown() {
        if (this.pendingTickSprintCommand == null || this.level == null) {
            return;
        }
        MinecraftServer server = this.level.getServer();
        if (server == null) {
            return;
        }

        if (!this.updateTickSprintVoters(server)) {
            this.cancelTickSprintCountdown();
            return;
        }
        if (this.allTickSprintVotersConfirmed()) {
            this.executePendingTickSprint();
            return;
        }
        this.tickSprintCountdownTicks--;
        if (this.tickSprintCountdownTicks > 0) {
            return;
        }

        this.cancelTickSprintCountdown();
    }

    private void executePendingTickSprint() {
        if (this.pendingTickSprintCommand == null || this.level == null) {
            return;
        }
        MinecraftServer server = this.level.getServer();
        if (server == null) {
            return;
        }
        String command = this.pendingTickSprintCommand;
        String normalizedCommand = command.startsWith("/") ? command.substring(1) : command;
        ServerPlayer player = this.pendingTickSprintPlayer == null
            ? null
            : server.getPlayerList().getPlayer(this.pendingTickSprintPlayer);
        int energyCost = 20 + getTickSprintConsumeProcess(normalizedCommand);
        if (this.chargingProgress >= energyCost) {
            server.getCommands().performPrefixedCommand(this.createCommandSource(player), command);
            this.chargingProgress -= energyCost;
            this.addHistoryCommand(normalizedCommand);
        } else {
            this.sendCommandFailure(
                player,
                Component.translatable("block.anvilcraft.spacetime_supercomputer.insufficient_energy")
            );
        }
        this.clearTickSprintCountdown();
    }

    private void cancelTickSprintCountdown() {
        MinecraftServer server = Objects.requireNonNull(this.level).getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(
                Component.translatable("block.anvilcraft.spacetime_supercomputer.tick_sprint_cancelled")
                    .withStyle(ChatFormatting.RED),
                false
            );
        }
        this.clearTickSprintCountdown();
    }

    private void clearTickSprintCountdown() {
        PENDING_TICK_SPRINTS.remove(this);
        this.pendingTickSprintCommand = null;
        this.pendingTickSprintPlayer = null;
        this.pendingTickSprintVoteId = null;
        this.pendingTickSprintVoters.clear();
        this.confirmedTickSprintVoters.clear();
        this.tickSprintCountdownTicks = 0;
        this.onChange();
    }

    @Override
    public void setRemoved() {
        PENDING_TICK_SPRINTS.remove(this);
        super.setRemoved();
    }

    private void sendCommandFailure(@Nullable Player player, Component message) {
        Component formattedMessage = Component.empty().append(message).withStyle(ChatFormatting.RED);
        MinecraftServer server = Objects.requireNonNull(this.level).getServer();
        if (player == null) {
            if (server != null) {
                server.getPlayerList().broadcastSystemMessage(formattedMessage, false);
            }
        } else {
            player.sendSystemMessage(formattedMessage);
        }
    }

    private static int getTickSprintConsumeProcess(String cmd) {
        int consumeProcess = 0;
        String timeAdd = cmd.replace("tick sprint ", "");
        if (timeAdd.matches("(\\d*.)?\\d+[tsd]")) {
            int tick = 0;
            float time = Float.parseFloat(timeAdd.substring(0, timeAdd.length() - 1));
            if (timeAdd.endsWith("t")) {
                tick += (int) time;
            } else if (timeAdd.endsWith("s")) {
                tick += (int) (time * 20);
            } else if (timeAdd.endsWith("d")) {
                tick += (int) (time * 24000);
            }
            int count = tick / 1000;
            consumeProcess += count;
        }
        return consumeProcess;
    }

    private static int getTimeAddConsumeProcess(String cmd) {
        int consumeEnergy = 0;
        String timeAdd = cmd.replace("time add ", "");
        if (timeAdd.matches("(\\d*.)?\\d+[tsd]")) {
            int tick = 0;
            float time = Float.parseFloat(timeAdd.substring(0, timeAdd.length() - 1));
            if (timeAdd.endsWith("t")) {
                tick += (int) time;
            } else if (timeAdd.endsWith("s")) {
                tick += (int) (time * 20);
            } else if (timeAdd.endsWith("d")) {
                tick += (int) (time * 24000);
            }
            int count = tick / 1000;
            consumeEnergy += count;
        }
        return consumeEnergy;
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
        return 512;
    }

    public void tick() {
        this.tickTickSprintCountdown();
        if (this.grid != null && this.grid.isWorking()) {
            if (this.chargingProgress < 100f) {
                this.chargingProgress += Math.clamp(0.01667f, 0f, 100.0f);
            }
        }
    }

    public void setProcessingRecipe(@Nullable RecipeHolder<Multiblock4DRecipe> processingRecipe) {
        this.processingRecipe = processingRecipe;
        this.processingTotal = processingRecipe == null ? -1 : processingRecipe.value().getDefinitions().size();
        this.onChange();
    }

    public void setProcessingStep(int processingStep) {
        this.processingStep = processingStep;
        this.onChange();
    }

    public void setProcessingSize(int processingSize) {
        this.processingSize = processingSize;
        this.onChange();
    }

    /**
     * 批量更新处理状态并只触发一次 {@link #onChange()}，避免连续多个 setter 各自
     * sendBlockUpdated + 逐玩家发包造成扇出。
     */
    public void setProcessingState(@Nullable RecipeHolder<Multiblock4DRecipe> recipe, int step, int size) {
        this.processingRecipe = recipe;
        this.processingStep = step;
        this.processingSize = size;
        this.processingTotal = recipe == null ? -1 : recipe.value().getDefinitions().size();
        this.onChange();
    }

    /**
     * 当前已成功合成的步数，尚未开始时为 0。
     */
    public int getProcessingProgress() {
        return this.processingRecipe == null ? 0 : Math.max(0, this.processingStep);
    }

    /**
     * 四维合成总步数。优先使用已同步的 total（客户端可能无法解析配方 holder），
     * 否则从配方定义推断。
     */
    public int getProcessingTotal() {
        if (this.processingTotal > 0) {
            return this.processingTotal;
        }
        if (this.processingRecipe != null) {
            return this.processingRecipe.value().getDefinitions().size();
        }
        return 0;
    }

    public void addPendingDrops(List<ItemStack> drops) {
        if (drops.isEmpty()) {
            return;
        }
        this.pendingDrops.addAll(drops);
        this.setChanged();
    }

    public void clearPendingDrops() {
        if (this.pendingDrops.isEmpty()) {
            return;
        }
        this.pendingDrops.clear();
        this.setChanged();
    }

    /**
     * 拆除时空超算时调用：将已消耗步骤的材料以掉落物形式释放。
     */
    public void dropProcessingInputs() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        List<ItemStack> drops = new ArrayList<>(this.pendingDrops);
        this.pendingDrops.clear();

        AnvilUtil.dropItems(drops, serverLevel, this.getBlockPos().below().getCenter());
        this.processingRecipe = null;
        this.processingStep = -1;
        this.processingSize = -1;
        this.processingTotal = -1;
        this.onChange();
    }
}
