package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.resources.model.ModelResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/// 将天体映射到其可能的天体约束环重构选项的注册表。
/// 每种天体类型的最内层环：
/// - 小型岩石行星（size < 26）：最内环 = R1
/// - 小型气态行星（size < 26）：最内环 = R2
/// - 小型恒星（size < 26）：最内环 = R4
/// - 大型恒星（size >= 26）：最内环 = R5
/// 共有11种独特巨构建筑，分布在不同环上。
/// 变体模型（如excavator_off、coil_fix）在世界中单独渲染；
/// UI中仅显示主要模型。
public final class CelestialRefactorRegistry {

    private CelestialRefactorRegistry() {
    }

    /// 获取给定天体的最内环编号。
    /// 启用增幅时，最小环始终为4（恒星级别）。
    public static int getInnermostRing(CelestialBodyData body, boolean amplified) {
        boolean isLarge = body.size() >= 48;
        int ring = switch (body) {
            case StarData ignored -> isLarge ? 5 : 4;
            case GiantPlanetData ignored -> 2;
            case RockyPlanetData ignored -> 1;
            case SpecialCelestialBodyData s -> s.isErrorPlanet() ? 0 : 1;
        };
        if (amplified) {
            ring = Math.max(ring, 4);
        }
        return ring;
    }

    /// 获取已锁定天体的可用重构选项。
    /// 非增幅锻星砧有环1-3 → 显示环1+2的巨构建筑。
    /// 增幅锻星砧有环3-5 → 显示环4+5的巨构建筑。
    /// resources - 行星资源集，用于根据资源可用性过滤选项；
    ///             可以为null（最宽松模式，显示所有符合环条件的选项）
    public static List<CelestialRefactorOption> getOptions(CelestialBodyData body, boolean amplified,
                                                           @Nullable PlanetaryResourceSet resources) {
        if (body == null) return Collections.emptyList();
        /// 错误行星无法建造巨构
        if (body instanceof SpecialCelestialBodyData s && s.isErrorPlanet()) {
            return Collections.emptyList();
        }
        int innermostRing = getInnermostRing(body, amplified);
        int maxRing = amplified ? 5 : 2;
        List<CelestialRefactorOption> options = getOptionsForRing(innermostRing, maxRing);

        /// 过滤行星开采器：岩石/特殊行星必须有液体
        if (!hasLiquid(body)) {
            options.removeIf(opt -> "planet_exctractor".equals(opt.megastructure()));
        }

        /// 过滤巨行星抽取器：仅对巨行星可用
        if (!(body instanceof GiantPlanetData)) {
            options.removeIf(opt -> "giant_planet_exctractor".equals(opt.megastructure()));
        }

        /// 过滤星环对撞机：仅对小型恒星可用（size < 48），黑洞和中子星不可用
        if (!(body instanceof StarData star && star.size() < 48
              && star.bodyClass() != CelestialBodyClass.NEUTRON_STAR
              && star.bodyClass() != CelestialBodyClass.BLACK_HOLE)) {
            options.removeIf(opt -> "stellar_ring_collider".equals(opt.megastructure()));
        }

        /// 过滤恒星演化加速器：恒星残骸不可用
        if (body instanceof StarData star
            && (star.bodyClass() == CelestialBodyClass.WHITE_DWARF
                || star.bodyClass() == CelestialBodyClass.NEUTRON_STAR
                || star.bodyClass() == CelestialBodyClass.BLACK_HOLE)) {
            options.removeIf(opt -> "stellar_evolution_accelerator".equals(opt.megastructure()));
        }

        /// 过滤磁星线圈：仅对中子星可用
        if (!(body instanceof StarData star && star.bodyClass() == CelestialBodyClass.NEUTRON_STAR)) {
            options.removeIf(opt -> "magnetar_coil".equals(opt.megastructure()));
        }

        /// 按恒星大小过滤恒星演化加速器的环变体：
        /// 小型恒星用环5模型，大型恒星用环6模型。
        if (body instanceof StarData star) {
            boolean isLarge = star.size() >= 48;
            options.removeIf(opt -> "stellar_evolution_accelerator".equals(opt.megastructure())
                && ((isLarge && opt.ring() == 5) || (!isLarge && opt.ring() == 6)));
        }

        /// 过滤戴森球：仅恒星可用（黑洞和中子星不可用），小型给小型恒星，大型给大型恒星
        if (!(body instanceof StarData star
              && star.bodyClass() != CelestialBodyClass.NEUTRON_STAR
              && star.bodyClass() != CelestialBodyClass.BLACK_HOLE)) {
            options.removeIf(opt -> "dyson_sphere_small".equals(opt.megastructure())
                || "dyson_sphere_large".equals(opt.megastructure()));
        } else {
            boolean isLarge = star.size() >= 48;
            options.removeIf(opt -> "dyson_sphere_small".equals(opt.megastructure()) && isLarge);
            options.removeIf(opt -> "dyson_sphere_large".equals(opt.megastructure()) && !isLarge);
        }

        /// 过滤彭罗斯球：仅对黑洞可用
        if (!(body instanceof StarData star && star.bodyClass() == CelestialBodyClass.BLACK_HOLE)) {
            options.removeIf(opt -> "penrose_sphere".equals(opt.megastructure()));
        }

        /// 过滤虫洞稳定器：仅对增幅模式下的黑洞可用
        if (!(body instanceof StarData star && star.bodyClass() == CelestialBodyClass.BLACK_HOLE && amplified)) {
            options.removeIf(opt -> "wormhole_stabilizer".equals(opt.megastructure()));
        }

        /// 过滤物质解压器：仅对中子星或黑洞可用
        if (!(body instanceof StarData star
            && (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR
                || star.bodyClass() == CelestialBodyClass.BLACK_HOLE))) {
            options.removeIf(opt -> "matter_decompressor".equals(opt.megastructure()));
        }

        /// 过滤生态站：需要生物资源且没有低级文明
        if (resources != null) {
            options.removeIf(opt -> "eco_station".equals(opt.megastructure())
                && !isEcoStationEligible(resources));
            /// 过滤神庙：需要低级文明
            options.removeIf(opt -> "temple".equals(opt.megastructure())
                && !resources.hasCivilization());
        }

        return options;
    }

    /// 生态站仅在行星拥有生物资源且没有低级文明时可用。
    private static boolean hasLiquid(CelestialBodyData body) {
        if (body instanceof RockyPlanetData rocky) return rocky.liquidCoverage() != LiquidCoverage.NONE;
        if (body instanceof SpecialCelestialBodyData s) {
            LiquidCoverage lc = s.liquidCoverage();
            return lc != null && lc != LiquidCoverage.NONE;
        }
        return false;
    }

    private static boolean isEcoStationEligible(PlanetaryResourceSet resources) {
        if (resources.hasCivilization()) return false;
        return !resources.getBiologicalItems().isEmpty()
            || !resources.getBiologicalFluids().isEmpty();
    }

    /// 获取给定环范围的可用巨构选项。
    /// 内层环可以建造外层环可以建造的任何巨构。
    public static List<CelestialRefactorOption> getOptionsForRing(int innermostRing, int maxRing) {
        List<CelestialRefactorOption> options = new ArrayList<>();
        String prefix = "screen.anvilcraft.cfa.megastructure.";

        if (innermostRing <= 1 && 1 <= maxRing) {
            /// 环1巨构（小型岩石行星的最内环）
            options.add(CelestialRefactorOption.withMaterial(1, "planet_excavator",
                ringModel(1, "excavator"), prefix + "planet_excavator",
                ModBlocks.RUBY_PRISM.asItem(), 16));
            options.add(CelestialRefactorOption.withMaterial(1, "planet_exctractor",
                ringModel(1, "exctractor"), prefix + "planet_exctractor",
                ModBlocks.PUMP.asItem(), 16));
            options.add(CelestialRefactorOption.withMaterial(1, "eco_station",
                ringModel(1, "eco_station"), prefix + "eco_station",
                ModBlocks.TEMPERING_GLASS.asItem(), 64));
            options.add(CelestialRefactorOption.withMaterial(1, "temple",
                ringModel(1, "temple"), prefix + "temple",
                net.minecraft.world.item.Items.GOLD_BLOCK, 64));
        }
        if (innermostRing <= 2 && 2 <= maxRing) {
            /// 环2巨构（小型气态行星的最内环）
            options.add(CelestialRefactorOption.withMaterial(2, "giant_planet_exctractor",
                ringModel(2, "exctractor"), prefix + "giant_planet_exctractor",
                ModBlocks.PUMP.asItem(), 32));
        }
        if (innermostRing <= 4 && 4 <= maxRing) {
            /// 环4巨构（小型恒星的最内环）
            options.add(CelestialRefactorOption.withMaterial(4, "stellar_ring_collider",
                ringModel(4, "collider"), prefix + "stellar_ring_collider",
                ModBlocks.ACCELERATION_RING.asItem(), 16));
            options.add(CelestialRefactorOption.withMaterial(4, "dyson_sphere_small",
                ringModel(4, "dyson_sphere"), prefix + "dyson_sphere_small",
                ModItems.DYSON_SPHERE_COMPONENT, 16));
            options.add(CelestialRefactorOption.withMaterial(4, "magnetar_coil",
                ringModel(4, "coil"), prefix + "magnetar_coil",
                ModBlocks.INFINITE_COLLECTOR.asItem(), 4));
            options.add(CelestialRefactorOption.withMaterial(4, "penrose_sphere",
                ringModel(4, "penrose_sphere"), prefix + "penrose_sphere",
                ModItems.PENROSE_SPHERE_COMPONENT, 8));
            options.add(CelestialRefactorOption.withMaterial(4, "matter_decompressor",
                ringModel(4, "matter_decompressor"), prefix + "matter_decompressor",
                ModBlocks.SINGULARITY_CRYSTAL.asItem(), 1));
            options.add(CelestialRefactorOption.withMaterial(4, "wormhole_stabilizer",
                ringModel(4, "wormhole_stabilizer"), prefix + "wormhole_stabilizer",
                ModBlocks.NEGATIVE_MATTER_BLOCK.asItem(), 16));
            options.add(CelestialRefactorOption.withMaterial(5, "stellar_evolution_accelerator",
                ringModel(5, "stellar_evolution_accelerator"), prefix + "stellar_evolution_accelerator",
                ModBlocks.CORRUPTED_BEACON.asItem(), 8));
        }
        if (innermostRing <= 5 && 5 <= maxRing) {
            /// 环5巨构（大型恒星的最内环）
            options.add(CelestialRefactorOption.withMaterial(5, "dyson_sphere_large",
                ringModel(5, "dyson_sphere"), prefix + "dyson_sphere_large",
                ModItems.DYSON_SPHERE_COMPONENT, 32));
            options.add(CelestialRefactorOption.withMaterial(6, "stellar_evolution_accelerator",
                ringModel(6, "stellar_evolution_accelerator"), prefix + "stellar_evolution_accelerator",
                ModBlocks.CORRUPTED_BEACON.asItem(), 8));
        }
        return options;
    }

    private static ModelResourceLocation ringModel(int ring, String megastructure) {
        return ModelResourceLocation.standalone(
            AnvilCraft.of("block/celestial_forging_anvil_ring_" + ring + "_" + megastructure)
        );
    }
}
