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
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class SpacetimeSupercomputerBlockEntity extends BlockEntity implements IPowerConsumer {
    @Getter
    @Setter
    private PowerGrid grid;

    @Getter
    private String command = "";

    @Getter
    private final EvictingQueue<String> historyCommands = EvictingQueue.create(16);

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

    public void runCommand(Player player) {
        if (this.level == null) {
            return;
        }
        String cmd = this.command;
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
                            .performPrefixedCommand(player.createCommandSourceStackForNameResolution((ServerLevel) level), cmd);
                        this.chargingProgress -= 20f + timeAddConsumeProcess;
                        this.addHistoryCommand(cmd);
                    } else {
                        player.sendSystemMessage(Component.translatable("block.anvilcraft.spacetime_supercomputer.insufficient_energy")
                            .withStyle(ChatFormatting.RED));
                    }
                } else if (cmd.startsWith("tick sprint")) {
                    int tickSprintConsumeProcess = getTickSprintConsumeProcess(cmd);
                    if (this.chargingProgress >= 20f + tickSprintConsumeProcess) {
                        Objects.requireNonNull(this.level.getServer())
                            .getCommands()
                            .performPrefixedCommand(player.createCommandSourceStackForNameResolution((ServerLevel) level), cmd);
                        this.chargingProgress -= 20f + tickSprintConsumeProcess;
                        this.addHistoryCommand(cmd);
                    } else {
                        player.sendSystemMessage(Component.translatable("block.anvilcraft.spacetime_supercomputer.insufficient_energy")
                            .withStyle(ChatFormatting.RED));
                    }
                } else if (cmd.startsWith("locate")) {
                    Objects.requireNonNull(this.level.getServer())
                        .getCommands()
                        .performPrefixedCommand(player.createCommandSourceStackForNameResolution((ServerLevel) level), cmd);
                    this.chargingProgress -= 20f;
                    this.addHistoryCommand(cmd);
                }
            } else {
                player.sendSystemMessage(Component.translatable("block.anvilcraft.spacetime_supercomputer.insufficient_energy")
                    .withStyle(ChatFormatting.RED));
            }
        } else {
            player.sendSystemMessage(Component.translatable("block.anvilcraft.spacetime_supercomputer.no_supported_command")
                .withStyle(ChatFormatting.RED));
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
        if (this.grid.isWorking()) {
            if (this.chargingProgress < 100f) {
                this.chargingProgress += Math.clamp(0.01667f, 0f, 100.0f);
            }
        }
    }
}
