package dev.dubhe.anvilcraft.api.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.power.consumer.HeaterBlock;
import dev.dubhe.anvilcraft.block.workstation.BurningHeaterBlock;
import dev.dubhe.anvilcraft.block.workstation.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * 炼药锅加工类别与有效加工方块的注册入口，供配方匹配和大锅选取加工位置共用。
 * 附属模组可向已有类别追加方块，也可注册自有类别并通过配方条件引用。
 */
public final class CauldronProcessing {
    public static final Identifier FAST_COOKING = AnvilCraft.of("fast_cooking");
    public static final Identifier SUPER_HEATING = AnvilCraft.of("super_heating");
    public static final Identifier TIME_WARP = AnvilCraft.of("time_warp");
    public static final Identifier NEUTRON_IRRADIATION = AnvilCraft.of("neutron_irradiation");

    private static final Map<Identifier, List<Predicate<BlockState>>> PROCESSORS = new ConcurrentHashMap<>();

    static {
        register(FAST_COOKING, state -> state.is(Blocks.CAMPFIRE) && CampfireBlock.isLitCampfire(state)
            || state.is(ModBlocks.BURNING_HEATER) && state.getValue(BurningHeaterBlock.LEVEL) == 1);
        register(SUPER_HEATING, state -> state.is(ModBlocks.HEATER) && ((HeaterBlock) state.getBlock()).isActive(state)
            || state.is(ModBlocks.BURNING_HEATER) && state.getValue(BurningHeaterBlock.LEVEL) == 2);
        register(TIME_WARP, state -> state.is(ModBlocks.CORRUPTED_BEACON) && state.getValue(CorruptedBeaconBlock.LIT));
        register(NEUTRON_IRRADIATION, state -> state.is(ModBlocks.NEUTRON_IRRADIATOR));
    }

    private CauldronProcessing() {
    }

    /**
     * 在公共初始化阶段为加工类别追加方块条件；客户端与服务端必须注册相同规则。
     * 条件仅负责判断方块是否可工作，原料、流体和产物仍由配方处理。
     */
    public static void register(Identifier category, Predicate<BlockState> active) {
        Objects.requireNonNull(category);
        Objects.requireNonNull(active);
        PROCESSORS.computeIfAbsent(category, key -> new CopyOnWriteArrayList<>()).add(active);
    }

    /** 判断方块是否能执行指定类别的配方，未注册的类别不匹配。 */
    public static boolean matches(Identifier category, BlockState state) {
        return PROCESSORS.getOrDefault(category, List.of()).stream().anyMatch(active -> active.test(state));
    }

    /** 判断方块能否作为大锅的加工位置。 */
    public static boolean isActive(BlockState state) {
        return PROCESSORS.values().stream().flatMap(List::stream).anyMatch(active -> active.test(state));
    }
}
