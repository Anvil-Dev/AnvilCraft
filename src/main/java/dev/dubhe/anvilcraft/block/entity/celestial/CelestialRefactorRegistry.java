package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 根据天体数据生成束星环巨构重构选项。
 * 岩石行星、巨行星、小型恒星和大型恒星的最内环分别为 R1、R2、R4 和 R5；
 * 恒星大小以 48 为分界。巨构的工作/停机变体在世界中单独渲染，界面只显示主模型。
 */
public final class CelestialRefactorRegistry {

    private CelestialRefactorRegistry() {
    }

    /** 获取指定天体的最内环；增幅模式下最小为恒星尺度的 R4。 */
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

    /**
     * 获取锁定天体可用的重构选项。普通锻星砧提供 R1-R2 巨构，增幅锻星砧提供 R4-R5 巨构。
     *
     * @param resources 天体资源集合，用于过滤依赖资源的巨构；为空时只按天体和环过滤
     */
    public static List<CelestialRefactorOption> getOptions(
        @Nullable CelestialBodyData body,
        boolean amplified,
        @Nullable PlanetaryResourceSet resources
    ) {
        if (body == null) return Collections.emptyList();
        // 错误行星不能建造巨构。
        if (body instanceof SpecialCelestialBodyData s && s.isErrorPlanet()) {
            return Collections.emptyList();
        }
        int innermostRing = CelestialRefactorRegistry.getInnermostRing(body, amplified);
        int maxRing = amplified ? 5 : 2;
        List<CelestialRefactorOption> options = CelestialRefactorRegistry.getOptionsForRing(innermostRing, maxRing);

        // 行星开采器要求岩石或特殊行星拥有液体。
        if (!CelestialRefactorRegistry.hasLiquid(body)) {
            options.removeIf(opt -> "planet_exctractor".equals(opt.megastructure()));
        }

        // 巨行星抽取器仅适用于巨行星。
        if (!(body instanceof GiantPlanetData)) {
            options.removeIf(opt -> "giant_planet_exctractor".equals(opt.megastructure()));
        }

        // 星环对撞机仅适用于小型普通恒星，黑洞和中子星不能建造。
        if (!(body instanceof StarData star && star.size() < 48
            && star.bodyClass() != CelestialBodyClass.NEUTRON_STAR
            && star.bodyClass() != CelestialBodyClass.BLACK_HOLE)) {
            options.removeIf(opt -> "stellar_ring_collider".equals(opt.megastructure()));
        }

        // 恒星残骸不能建造恒星演化加速器。
        if (body instanceof StarData star
            && (star.bodyClass() == CelestialBodyClass.WHITE_DWARF
                || star.bodyClass() == CelestialBodyClass.NEUTRON_STAR
                || star.bodyClass() == CelestialBodyClass.BLACK_HOLE)) {
            options.removeIf(opt -> "stellar_evolution_accelerator".equals(opt.megastructure()));
        }

        // 磁星线圈仅适用于中子星。
        if (!(body instanceof StarData star && star.bodyClass() == CelestialBodyClass.NEUTRON_STAR)) {
            options.removeIf(opt -> "magnetar_coil".equals(opt.megastructure()));
        }

        // 小型恒星使用 R5 加速器模型，大型恒星使用 R6 模型。
        if (body instanceof StarData star) {
            boolean isLarge = star.size() >= 48;
            options.removeIf(opt -> "stellar_evolution_accelerator".equals(opt.megastructure())
                && ((isLarge && opt.ring() == 5) || (!isLarge && opt.ring() == 6)));
        }

        // 戴森球仅适用于普通恒星；黑洞、中子星和行星均不可建造。
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

        // 彭罗斯球仅适用于黑洞。
        if (!(body instanceof StarData star && star.bodyClass() == CelestialBodyClass.BLACK_HOLE)) {
            options.removeIf(opt -> "penrose_sphere".equals(opt.megastructure()));
        }

        // 虫洞稳定器仅适用于增幅模式下的黑洞。
        if (!(body instanceof StarData star && star.bodyClass() == CelestialBodyClass.BLACK_HOLE && amplified)) {
            options.removeIf(opt -> "wormhole_stabilizer".equals(opt.megastructure()));
        }

        // 物质解压器仅适用于中子星或黑洞。
        if (!(body instanceof StarData star
            && (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR
                || star.bodyClass() == CelestialBodyClass.BLACK_HOLE))) {
            options.removeIf(opt -> "matter_decompressor".equals(opt.megastructure()));
        }

        // 生态站要求存在生物资源且没有低级文明。
        if (resources != null) {
            options.removeIf(opt -> "eco_station".equals(opt.megastructure())
                && !CelestialRefactorRegistry.isEcoStationEligible(resources));
            // 神庙要求存在低级文明。
            options.removeIf(opt -> "temple".equals(opt.megastructure())
                && !resources.hasCivilization());
        }

        return options;
    }

    /** 判断天体资源是否满足生态站建造条件。 */
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

    /** 获取环范围 [innermostRing, maxRing] 内的巨构选项，内环可建造外环对应巨构。 */
    public static List<CelestialRefactorOption> getOptionsForRing(int innermostRing, int maxRing) {
        List<CelestialRefactorOption> options = new ArrayList<>();
        String prefix = "screen.anvilcraft.cfa.megastructure.";

        if (innermostRing <= 1 && 1 <= maxRing) {
            // R1 巨构，主要用于小型岩石行星。
            options.add(CelestialRefactorOption.withMaterial(
                1,
                "planet_excavator",
                CelestialRefactorRegistry.ringModel(1, "excavator"),
                prefix + "planet_excavator",
                ModBlocks.RUBY_PRISM.asItem(),
                16
            ));
            options.add(CelestialRefactorOption.withMaterial(
                1,
                "planet_exctractor",
                CelestialRefactorRegistry.ringModel(1, "exctractor"),
                prefix + "planet_exctractor",
                ModBlocks.PUMP.asItem(),
                16
            ));
            options.add(CelestialRefactorOption.withMaterial(
                1,
                "eco_station",
                CelestialRefactorRegistry.ringModel(1, "eco_station"),
                prefix + "eco_station",
                ModBlocks.TEMPERING_GLASS.asItem(),
                64
            ));
            options.add(CelestialRefactorOption.withMaterial(
                1,
                "temple",
                CelestialRefactorRegistry.ringModel(1, "temple"),
                prefix + "temple",
                Items.GOLD_BLOCK,
                64
            ));
        }
        if (innermostRing <= 2 && 2 <= maxRing) {
            // R2 巨构，主要用于小型巨行星。
            options.add(CelestialRefactorOption.withMaterial(
                2,
                "giant_planet_exctractor",
                CelestialRefactorRegistry.ringModel(2, "exctractor"),
                prefix + "giant_planet_exctractor",
                ModBlocks.PUMP.asItem(),
                32
            ));
        }
        if (innermostRing <= 4 && 4 <= maxRing) {
            // R4 巨构，主要用于小型恒星和致密天体。
            options.add(CelestialRefactorOption.withMaterial(
                4,
                "stellar_ring_collider",
                CelestialRefactorRegistry.ringModel(4, "collider"),
                prefix + "stellar_ring_collider",
                ModItems.STELLAR_RING_COMPONENT,
                8
            ));
            options.add(CelestialRefactorOption.withMaterial(
                4,
                "dyson_sphere_small",
                CelestialRefactorRegistry.ringModel(4, "dyson_sphere"),
                prefix + "dyson_sphere_small",
                ModItems.DYSON_SPHERE_COMPONENT,
                16
            ));
            options.add(CelestialRefactorOption.withMaterial(
                4,
                "magnetar_coil",
                CelestialRefactorRegistry.ringModel(4, "coil"),
                prefix + "magnetar_coil",
                ModItems.MAGNETAR_COIL_COMPONENT,
                4
            ));
            options.add(CelestialRefactorOption.withMaterial(
                4,
                "penrose_sphere",
                CelestialRefactorRegistry.ringModel(4, "penrose_sphere"),
                prefix + "penrose_sphere",
                ModItems.PENROSE_SPHERE_COMPONENT,
                8
            ));
            options.add(CelestialRefactorOption.withMaterial(
                4,
                "matter_decompressor",
                CelestialRefactorRegistry.ringModel(4, "matter_decompressor"),
                prefix + "matter_decompressor",
                ModItems.MATTER_DECOMPRESSOR_COMPONENT,
                2
            ));
            options.add(CelestialRefactorOption.withMaterial(
                4,
                "wormhole_stabilizer",
                CelestialRefactorRegistry.ringModel(4, "wormhole_stabilizer"),
                prefix + "wormhole_stabilizer",
                ModItems.WORMHOLE_STABILIZER_COMPONENT,
                4
            ));
            options.add(CelestialRefactorOption.withMaterial(
                5,
                "stellar_evolution_accelerator",
                CelestialRefactorRegistry.ringModel(5, "stellar_evolution_accelerator"),
                prefix + "stellar_evolution_accelerator",
                ModItems.STELLAR_EVOLUTION_ACCELERATOR_COMPONENT,
                8
            ));
        }
        if (innermostRing <= 5 && 5 <= maxRing) {
            // R5 巨构，主要用于大型恒星。
            options.add(CelestialRefactorOption.withMaterial(
                5,
                "dyson_sphere_large",
                CelestialRefactorRegistry.ringModel(5, "dyson_sphere"),
                prefix + "dyson_sphere_large",
                ModItems.DYSON_SPHERE_COMPONENT,
                32
            ));
            options.add(CelestialRefactorOption.withMaterial(
                6,
                "stellar_evolution_accelerator",
                CelestialRefactorRegistry.ringModel(6, "stellar_evolution_accelerator"),
                prefix + "stellar_evolution_accelerator",
                ModItems.STELLAR_EVOLUTION_ACCELERATOR_COMPONENT,
                8
            ));
        }
        return options;
    }

    private static Identifier ringModel(int ring, String megastructure) {
        return AnvilCraft.of(
            "block/celestial_forging_anvil_ring_" + ring + "_" + megastructure
        );
    }
}
