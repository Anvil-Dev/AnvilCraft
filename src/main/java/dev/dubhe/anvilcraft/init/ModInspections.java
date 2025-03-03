package dev.dubhe.anvilcraft.init;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.network.InspectionStateChangedPacket;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.commands.Commands.literal;

@Slf4j
public class ModInspections {
    public static final ModInspections INSTANCE = new ModInspections();
    private final List<ResourceLocation> inspectionOptions = new ArrayList<>();

    public static void initialize() {
        INSTANCE.registerActionServer(AnvilCraft.of("silencer"));
    }

    /**
     * 注册检查项
     * <p>
     * 检查项需同时在{@link ModInspections}和{@link dev.dubhe.anvilcraft.client.ModInspectionClient}中注册
     * </p>
     * <p>
     * 对于{@link dev.dubhe.anvilcraft.client.ModInspectionClient}
     * 使用{@link dev.dubhe.anvilcraft.client.ModInspectionClient#registerActionClient} 注册检查项
     * </p>
     *
     * @see dev.dubhe.anvilcraft.client.ModInspectionClient
     */
    public void registerActionServer(ResourceLocation id) {
        INSTANCE.inspectionOptions.add(id);
    }

    private int changeStateServer(ServerPlayer player, ResourceLocation id, boolean state) {
        PacketDistributor.sendToPlayer(player, new InspectionStateChangedPacket(id, state));
        return 0;
    }

    public void registerCommand(LiteralArgumentBuilder<CommandSourceStack> parent) {
        LiteralArgumentBuilder<CommandSourceStack> commandRoot = literal("inspection");
        for (ResourceLocation option : inspectionOptions) {
            commandRoot.then(
                literal(option.toString())
                    .then(literal("enable")
                        .executes(ctx -> ctx.getSource().isPlayer()
                            ? changeStateServer(ctx.getSource().getPlayer(), option, true)
                            : 0
                        )
                    )
                    .then(literal("disable")
                        .executes(ctx -> ctx.getSource().isPlayer()
                            ? changeStateServer(ctx.getSource().getPlayer(), option, false)
                            : 0
                        )
                    )
            );
        }
        parent.then(commandRoot);
    }
}
