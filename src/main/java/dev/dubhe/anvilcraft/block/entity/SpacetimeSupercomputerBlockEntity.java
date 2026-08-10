package dev.dubhe.anvilcraft.block.entity;

import com.google.common.collect.EvictingQueue;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.model.CommandInfo;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class SpacetimeSupercomputerBlockEntity extends BlockEntity implements IPowerConsumer {
    private static final int TICK_SPRINT_COUNTDOWN_SECONDS = 30;
    private static final int TICK_SPRINT_COUNTDOWN_TICKS = SpacetimeSupercomputerBlockEntity.TICK_SPRINT_COUNTDOWN_SECONDS * 20;
    private static final String TICK_SPRINT_VOTE_SEPARATOR = "------------";
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
    private final List<CommandInfo> availableCommands = Util.make(
        new ObjectArrayList<>(), (list) -> {
            list.add(new CommandInfo("/locate biome", AnvilCraft.CONFIG.spacetimeSupercomputerCommand.allowLocateBiomeCommand));
            list.add(new CommandInfo("/locate structure", AnvilCraft.CONFIG.spacetimeSupercomputerCommand.allowLocateStructureCommand));
            list.add(new CommandInfo("/locate poi", AnvilCraft.CONFIG.spacetimeSupercomputerCommand.allowLocatePoiCommand));
            list.add(new CommandInfo("/time add", AnvilCraft.CONFIG.spacetimeSupercomputerCommand.allowTimeAddCommand));
            list.add(new CommandInfo("/tick sprint", AnvilCraft.CONFIG.spacetimeSupercomputerCommand.allowTickSprintCommand));
        }
    );

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
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.command.isBlank()) {
            output.putString("command", this.command);
        }
        if (this.chargingProgress > 0) {
            output.putFloat("chargingProgress", this.chargingProgress);
        }
        if (!this.historyCommands.isEmpty()) {
            ValueOutput.ValueOutputList historyCommands1 = output.childrenList("historyCommands");
            for (String historyCommand : this.historyCommands) {
                ValueOutput valueOutput = historyCommands1.addChild();
                valueOutput.putString("command", historyCommand);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getString("command").ifPresent((command) -> this.command = command);
        this.chargingProgress = input.getFloatOr("chargingProgress", 0);
        for (ValueInput command : input.childrenListOrEmpty("historyCommands")) {
            command.getString("command").ifPresent(this.historyCommands::add);
        }
    }

    public void runCommand(@Nullable Player player) {
        if (this.level == null) {
            return;
        }
        CommandSourceStack commandSourceStack = this.createCommandSource(player);
        String cmd = this.command;
        if (cmd.equalsIgnoreCase("Searge")) {
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
                    int timeAddConsumeProcess = SpacetimeSupercomputerBlockEntity.getTimeAddConsumeProcess(cmd);
                    if (this.chargingProgress >= 20f + timeAddConsumeProcess) {
                        Objects.requireNonNull(this.level.getServer())
                            .getCommands()
                            .performPrefixedCommand(commandSourceStack, this.command);
                        this.chargingProgress -= 20f + timeAddConsumeProcess;
                        this.addHistoryCommand(cmd);
                    } else {
                        this.sendCommandFailure(
                            player,
                            Component.translatable("block.anvilcraft.spacetime_supercomputer.insufficient_energy")
                        );
                    }
                } else if (cmd.startsWith("tick sprint")) {
                    int tickSprintConsumeProcess = SpacetimeSupercomputerBlockEntity.getTickSprintConsumeProcess(cmd);
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
                    Objects.requireNonNull(this.level.getServer())
                        .getCommands()
                        .performPrefixedCommand(commandSourceStack, this.command);
                    this.chargingProgress -= 20f;
                    this.addHistoryCommand(cmd);
                }
            } else {
                this.sendCommandFailure(
                    player,
                    Component.translatable("block.anvilcraft.spacetime_supercomputer.insufficient_energy")
                );
            }
        } else {
            this.sendCommandFailure(
                player,
                Component.translatable("block.anvilcraft.spacetime_supercomputer.no_supported_command")
            );
        }
    }

    private CommandSourceStack createCommandSource(@Nullable Player player) {
        if (player != null) {
            return player.createCommandSourceStackForNameResolution((ServerLevel) Objects.requireNonNull(this.level))
                .withMaximumPermission(LevelBasedPermissionSet.OWNER);
        }
        MinecraftServer server = Objects.requireNonNull(Objects.requireNonNull(this.level).getServer());
        ServerLevel serverLevel = (ServerLevel) this.level;
        return new CommandSourceStack(
            server,
            Vec3.atCenterOf(this.getBlockPos()),
            Vec2.ZERO,
            serverLevel,
            LevelBasedPermissionSet.OWNER,
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
                        minecraftServer.getPlayerList().broadcastSystemMessage(
                            Component.empty().append(message).withStyle(ChatFormatting.RED),
                            false
                        );
                    }
                    this.source.sendSystemMessage(Component.empty().append(message).withStyle(ChatFormatting.RED));
                }
            }
        };
    }

    private void startTickSprintCountdown(@Nullable Player player, String command) {
        MinecraftServer server = Objects.requireNonNull(this.level).getServer();
        if (server == null) return;
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
        this.tickSprintCountdownTicks = SpacetimeSupercomputerBlockEntity.TICK_SPRINT_COUNTDOWN_TICKS;
        SpacetimeSupercomputerBlockEntity.PENDING_TICK_SPRINTS.add(this);
        this.initializeTickSprintVoters(server);
        this.setChanged();
    }

    public static void cancelPendingTickSprints(MinecraftServer server) {
        for (SpacetimeSupercomputerBlockEntity supercomputer : List.copyOf(SpacetimeSupercomputerBlockEntity.PENDING_TICK_SPRINTS)) {
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

    private boolean updateTickSprintVoters(MinecraftServer server) {
        Set<UUID> onlinePlayers = new HashSet<>();
        for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
            UUID playerId = serverPlayer.getUUID();
            if (!this.pendingTickSprintVoters.contains(playerId)) return false;
            onlinePlayers.add(playerId);
        }
        this.pendingTickSprintVoters.retainAll(onlinePlayers);
        this.confirmedTickSprintVoters.retainAll(onlinePlayers);
        return true;
    }

    private void sendTickSprintVoteMessage(ServerPlayer player) {
        MutableComponent message = Component.empty()
            .append(Component.literal(SpacetimeSupercomputerBlockEntity.TICK_SPRINT_VOTE_SEPARATOR).withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal("\n"))
            .append(Component.translatable("block.anvilcraft.spacetime_supercomputer.tick_sprint_confirmation"))
            .append(Component.literal("\n"))
            .append(this.createTickSprintVoteOption(true))
            .append(Component.literal("    "))
            .append(this.createTickSprintVoteOption(false))
            .append(Component.literal("\n"))
            .append(Component.literal(SpacetimeSupercomputerBlockEntity.TICK_SPRINT_VOTE_SEPARATOR).withStyle(ChatFormatting.DARK_GRAY));
        player.sendSystemMessage(message);
    }

    private Component createTickSprintVoteOption(boolean accepted) {
        UUID voteId = Objects.requireNonNull(this.pendingTickSprintVoteId);
        ServerLevel serverLevel = (ServerLevel) Objects.requireNonNull(this.level);
        BlockPos pos = this.getBlockPos();
        String command = "/anvilcraft tick_sprint_vote %s %s %s %s %s %s".formatted(
            serverLevel.dimension().identifier(),
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
            .withClickEvent(new ClickEvent.RunCommand(command)));
    }

    public boolean submitTickSprintVote(ServerPlayer player, UUID voteId, boolean accepted) {
        if (!voteId.equals(this.pendingTickSprintVoteId)
            || !this.pendingTickSprintVoters.contains(player.getUUID())) {
            return false;
        }
        MinecraftServer server = Objects.requireNonNull(this.level).getServer();
        if (server == null) return false;
        if (!this.updateTickSprintVoters(server)) {
            this.cancelTickSprintCountdown();
            return false;
        }
        if (accepted && !this.confirmedTickSprintVoters.add(player.getUUID())) return false;
        String feedbackKey = accepted
            ? "block.anvilcraft.spacetime_supercomputer.tick_sprint_allowed"
            : "block.anvilcraft.spacetime_supercomputer.tick_sprint_rejected";
        ChatFormatting feedbackColor = accepted ? ChatFormatting.GREEN : ChatFormatting.RED;
        player.sendSystemMessage(Component.translatable(feedbackKey).withStyle(feedbackColor));
        if (!accepted) {
            this.cancelTickSprintCountdown();
            return true;
        }
        if (this.allTickSprintVotersConfirmed()) this.executePendingTickSprint();
        return true;
    }

    private boolean allTickSprintVotersConfirmed() {
        return !this.pendingTickSprintVoters.isEmpty()
            && this.confirmedTickSprintVoters.containsAll(this.pendingTickSprintVoters);
    }

    private void tickTickSprintCountdown() {
        if (this.pendingTickSprintCommand == null || this.level == null) return;
        MinecraftServer server = this.level.getServer();
        if (server == null) return;
        if (!this.updateTickSprintVoters(server)) {
            this.cancelTickSprintCountdown();
            return;
        }
        if (this.allTickSprintVotersConfirmed()) {
            this.executePendingTickSprint();
            return;
        }
        this.tickSprintCountdownTicks--;
        if (this.tickSprintCountdownTicks <= 0) this.cancelTickSprintCountdown();
    }

    private void executePendingTickSprint() {
        if (this.pendingTickSprintCommand == null || this.level == null) return;
        MinecraftServer server = this.level.getServer();
        if (server == null) return;
        String command = this.pendingTickSprintCommand;
        String normalizedCommand = command.startsWith("/") ? command.substring(1) : command;
        ServerPlayer player = this.pendingTickSprintPlayer == null
            ? null
            : server.getPlayerList().getPlayer(this.pendingTickSprintPlayer);
        int energyCost = 20 + SpacetimeSupercomputerBlockEntity.getTickSprintConsumeProcess(normalizedCommand);
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
        SpacetimeSupercomputerBlockEntity.PENDING_TICK_SPRINTS.remove(this);
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
        SpacetimeSupercomputerBlockEntity.PENDING_TICK_SPRINTS.remove(this);
        super.setRemoved();
    }

    private void sendCommandFailure(@Nullable Player player, Component message) {
        Component formattedMessage = Component.empty().append(message).withStyle(ChatFormatting.RED);
        MinecraftServer server = Objects.requireNonNull(this.level).getServer();
        if (player == null) {
            if (server != null) server.getPlayerList().broadcastSystemMessage(formattedMessage, false);
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
}
