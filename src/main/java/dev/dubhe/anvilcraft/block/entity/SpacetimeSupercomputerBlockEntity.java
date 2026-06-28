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
import java.util.function.Supplier;

public class SpacetimeSupercomputerBlockEntity extends BlockEntity implements IPowerConsumer {
    @Getter
    @Setter
    private @Nullable PowerGrid grid;

    @Getter
    private String command = "";

    @Getter
    private EvictingQueue<String> historyCommands = EvictingQueue.create(16);

    @Getter
    private float chargingProgress = 0;

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
        CommandSourceStack commandSourceStack;
        if (player == null) {
            MinecraftServer server = this.level.getServer();
            ServerLevel serverLevel = server.overworld();
            commandSourceStack = new CommandSourceStack(
                server,
                serverLevel == null ? Vec3.ZERO : Vec3.atLowerCornerOf(serverLevel.getSharedSpawnPos()),
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
                    boolean flag = this.source.acceptsSuccess() && !this.isSilent();
                    boolean flag1 = allowLogging && this.source.shouldInformAdmins() && !this.isSilent();
                    if (flag || flag1) {
                        Component component = messageSupplier.get();
                        if (flag) {
                            if (this.source instanceof MinecraftServer server1) {
                                server1.getPlayerList().broadcastSystemMessage(component, false);
                            }
                        }
                    }
                }

                @Override
                public void sendFailure(Component message) {
                    if (this.source.acceptsFailure() && !this.isSilent()) {
                        if (this.source instanceof MinecraftServer server1) {
                            server1.getPlayerList()
                                .broadcastSystemMessage(
                                    Component.empty().append(message).withStyle(ChatFormatting.RED),
                                    false
                                );
                        }
                        this.source.sendSystemMessage(Component.empty().append(message).withStyle(ChatFormatting.RED));
                    }
                }
            };
        } else {
            commandSourceStack = player.createCommandSourceStack().withPermission(4);
        }
        String cmd = this.command;
        if (this.command.equalsIgnoreCase("Searge")) {
            if (player == null) {
                Objects.requireNonNull(this.level.getServer())
                    .getPlayerList()
                    .broadcastSystemMessage(
                        Component.literal("#itzlipofutzli"),
                        false
                    );
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
                        Objects.requireNonNull(
                            this.level.getServer()).getCommands().performPrefixedCommand(
                            commandSourceStack,
                            this.command
                        );
                        this.chargingProgress -= 20f + timeAddConsumeProcess;
                        this.addHistoryCommand(cmd);
                    } else {
                        if (player == null) {
                            Objects.requireNonNull(this.level.getServer())
                                .getPlayerList()
                                .broadcastSystemMessage(
                                    Component.translatable("block.anvilcraft.spacetime_supercomputer.insufficient_energy")
                                        .withStyle(ChatFormatting.RED),
                                    false
                                );
                        } else {
                            player.sendSystemMessage(
                                Component.translatable("block.anvilcraft.spacetime_supercomputer.insufficient_energy")
                                    .withStyle(ChatFormatting.RED)
                            );
                        }
                    }
                } else if (cmd.startsWith("tick sprint")) {
                    if (player == null) {
                        Objects.requireNonNull(this.level.getServer()).sendSystemMessage(Component.literal("此命令必须由玩家触发"));
                        return;
                    }
                    int tickSprintConsumeProcess = getTickSprintConsumeProcess(cmd);
                    if (this.chargingProgress >= 20f + tickSprintConsumeProcess) {
                        Objects.requireNonNull(
                            this.level.getServer()).getCommands().performPrefixedCommand(
                            commandSourceStack,
                            this.command
                        );
                        this.chargingProgress -= 20f + tickSprintConsumeProcess;
                        this.addHistoryCommand(cmd);
                    } else {
                        player.sendSystemMessage(
                            Component.translatable("block.anvilcraft.spacetime_supercomputer.insufficient_energy")
                                .withStyle(ChatFormatting.RED)
                        );
                    }
                } else if (cmd.startsWith("locate")) {
                    Objects.requireNonNull(
                        this.level.getServer()).getCommands().performPrefixedCommand(
                        commandSourceStack,
                        this.command
                    );
                    this.chargingProgress -= 20f;
                    this.addHistoryCommand(cmd);
                }
            } else {
                if (player == null) {
                    Objects.requireNonNull(this.level.getServer())
                        .getPlayerList()
                        .broadcastSystemMessage(
                            Component.translatable("block.anvilcraft.spacetime_supercomputer.insufficient_energy")
                                .withStyle(ChatFormatting.RED),
                            false
                        );
                } else {
                    player.sendSystemMessage(
                        Component.translatable("block.anvilcraft.spacetime_supercomputer.insufficient_energy")
                            .withStyle(ChatFormatting.RED)
                    );
                }
            }
        } else {
            if (player == null) {
                Objects.requireNonNull(this.level.getServer())
                    .getPlayerList()
                    .broadcastSystemMessage(
                        Component.translatable("block.anvilcraft.spacetime_supercomputer.no_supported_command")
                            .withStyle(ChatFormatting.RED),
                        false
                    );
            } else {
                player.sendSystemMessage(
                    Component.translatable("block.anvilcraft.spacetime_supercomputer.no_supported_command")
                        .withStyle(ChatFormatting.RED)
                );
            }
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
        if (this.grid != null && this.grid.isWorking()) {
            if (this.chargingProgress < 100f) {
                this.chargingProgress += Math.clamp(0.01667f, 0f, 100.0f);
            }
        }
    }
}
