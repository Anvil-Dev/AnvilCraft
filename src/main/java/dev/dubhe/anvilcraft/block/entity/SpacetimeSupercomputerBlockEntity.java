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
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class SpacetimeSupercomputerBlockEntity extends BlockEntity implements IPowerConsumer {
    private static final int TICK_SPRINT_COUNTDOWN_TICKS = 5 * 20;

    @Getter
    @Setter
    private @Nullable PowerGrid grid;

    @Getter
    private String command = "";

    @Getter
    private EvictingQueue<String> historyCommands = EvictingQueue.create(16);

    @Getter
    private float chargingProgress = 0;

    private @Nullable String pendingTickSprintCommand;
    private @Nullable UUID pendingTickSprintPlayer;
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
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
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
        this.tickSprintCountdownTicks = TICK_SPRINT_COUNTDOWN_TICKS;
        this.broadcastTickSprintCountdown(server, 5);
        this.setChanged();
    }

    private void broadcastTickSprintCountdown(MinecraftServer server, int seconds) {
        Component message = Component.translatable(
            "block.anvilcraft.spacetime_supercomputer.tick_sprint_countdown",
            seconds
        ).withStyle(ChatFormatting.YELLOW);
        for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
            serverPlayer.displayClientMessage(message, true);
        }
    }

    private void tickTickSprintCountdown() {
        if (this.pendingTickSprintCommand == null || this.level == null) {
            return;
        }
        MinecraftServer server = this.level.getServer();
        if (server == null) {
            return;
        }

        this.tickSprintCountdownTicks--;
        if (this.tickSprintCountdownTicks > 0) {
            if (this.tickSprintCountdownTicks % 20 == 0) {
                this.broadcastTickSprintCountdown(server, this.tickSprintCountdownTicks / 20);
            }
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
        this.pendingTickSprintCommand = null;
        this.pendingTickSprintPlayer = null;
        this.tickSprintCountdownTicks = 0;
        this.onChange();
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
}
