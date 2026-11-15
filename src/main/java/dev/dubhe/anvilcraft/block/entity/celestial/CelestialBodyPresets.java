package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.celestial.SpecialCelestialBodyRecipe;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 预设只保存类型，放置时重新生成天体；普通掉落物仍保存已有天体。 */
public final class CelestialBodyPresets {
    public static final String TAG = "celestialPreset";
    public static final String NAME_PREFIX = "screen.anvilcraft.cfa.class.";
    public static final String BLACK_HOLE = NAME_PREFIX + "black_hole";
    public static final String WHITE_HOLE = "special:anvilcraft:special_celestial_body/white_hole";
    private static final String SPECIAL_PREFIX = "special:";
    private static final String SHATTERED = "builtin:shattered_planet";
    private static final String PLAYER_HEAD = "builtin:player_head";
    private static final String RED_DWARF = NAME_PREFIX + "mutated_red_dwarf";

    private CelestialBodyPresets() {
    }

    /** 每次读取当前配方集合，使数据包增加的特殊天体自动进入选择器。 */
    public static List<ItemStack> variants(Level level) {
        Map<String, ItemStack> variants = new LinkedHashMap<>();
        for (String name : CelestialBodyMatcher.presetNames()) {
            variants.put(name, stack(name, name));
        }
        for (String name : List.of(NAME_PREFIX + "white_dwarf", NAME_PREFIX + "neutron_star", BLACK_HOLE, RED_DWARF)) {
            variants.putIfAbsent(name, stack(name, name));
        }
        variants.put(NAME_PREFIX + "special.shattered_planet", stack(SHATTERED, NAME_PREFIX + "special.shattered_planet"));
        var recipes = RecipesRecord.getRecipes(level);
        if (recipes != null) {
            recipes.byType(ModRecipeTypes.SPECIAL_CELESTIAL_BODY.get()).stream()
                .sorted(Comparator.comparing(holder -> holder.id().identifier().toString()))
                .forEach(holder -> {
                    String name = NAME_PREFIX + "special." + holder.value().name();
                    variants.putIfAbsent(name, stack(SPECIAL_PREFIX + holder.id().identifier(), name));
                });
        }
        return List.copyOf(variants.values());
    }

    /** 制作携带预设标记的锻星砧，不在物品中保存随机种子或 UUID。 */
    public static ItemStack stack(String preset, String nameKey) {
        ItemStack stack = ModBlocks.CELESTIAL_FORGING_ANVIL.asStack();
        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        output.putString(TAG, preset);
        BlockItem.setBlockEntityData(stack, ModBlockEntities.CELESTIAL_FORGING_ANVIL.get(), output);
        stack.set(DataComponents.ITEM_NAME, Component.translatable(nameKey));
        return stack;
    }

    /** 读取预设标记，实际天体掉落物不含此标记。 */
    public static @Nullable String preset(ItemStack stack) {
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? null : data.copyTagWithoutId().getString(TAG).orElse(null);
    }

    /** 使用稳定参数绘制物品预览，预览数据不会成为放置结果。 */
    public static @Nullable CompoundTag preview(String preset, Level level) {
        CelestialBodyMatcher.Preset sample = sample(preset, level, RandomSource.create(0));
        if (sample == null) {
            return null;
        }
        CompoundTag tag = new CompoundTag();
        tag.put("celestialBody", sample.body().toTag());
        tag.putInt("ageAnvilCount", sample.time());
        tag.putInt("stellarMass", sample.mass());
        return tag;
    }

    /** 服务端每次调用均分配新的随机种子和恒星身份，并按对应规则生成资源。 */
    public static boolean apply(String preset, CelestialForgingAnvilBlockEntity owner, ServerLevel level) {
        long seed = level.getRandom().nextLong();
        CelestialBodyMatcher.Preset sample = sample(preset, level, RandomSource.create(seed));
        if (sample == null) {
            return false;
        }
        CelestialBodyData body = sample.body();
        if (body instanceof StarData star) {
            body = star.withBodyUuid(StarData.uuidFromBodySeed(seed));
        }
        SpecialCelestialBodyRecipe special = specialRecipe(preset, level);
        PlanetaryResourceSet resources = SHATTERED.equals(preset) ? ShatteredPlanet.createResources() : special == null
            ? PlanetResourceGenerator.generate(body, sample.time(), level, seed, null)
            : special.generateResources();
        owner.setBodySeed(seed);
        owner.setAgeAnvilCount(sample.time());
        owner.setStellarMass(sample.mass());
        owner.setCelestialBodyData(body);
        owner.setPlanetaryResourceSet(resources);
        owner.addToSearchHistory(body, resources);
        owner.setChanged();
        owner.syncToClient();
        return true;
    }

    private static CelestialBodyMatcher.@Nullable Preset sample(String preset, Level level, RandomSource random) {
        if (SHATTERED.equals(preset)) {
            return new CelestialBodyMatcher.Preset(ShatteredPlanet.createBody(),
                ShatteredPlanet.AGE_ANVIL_COUNT, ShatteredPlanet.MASS_ANVIL_COUNT, 64);
        }
        if (PLAYER_HEAD.equals(preset)) {
            CompoundTag profile = (CompoundTag) ResolvableProfile.CODEC
                .encodeStart(NbtOps.INSTANCE, ResolvableProfile.createResolved(UUIDUtil.createOfflineProfile("Steve"))).getOrThrow();
            return new CelestialBodyMatcher.Preset(SpecialCelestialBodyData.fromPlayerHead(profile, 1), 1, 1, 1);
        }
        if (preset.startsWith(SPECIAL_PREFIX)) {
            SpecialCelestialBodyRecipe recipe = specialRecipe(preset, level);
            return recipe == null ? null : new CelestialBodyMatcher.Preset(
                SpecialCelestialBodyData.fromRecipe(recipe, preset.substring(SPECIAL_PREFIX.length())),
                recipe.time(), recipe.mass(), recipe.energy());
        }
        CelestialBodyMatcher.Preset sample = CelestialBodyMatcher.samplePreset(preset, random);
        if (sample != null) {
            return sample;
        }
        if (RED_DWARF.equals(preset)) {
            sample = CelestialBodyMatcher.samplePreset(NAME_PREFIX + "brown_dwarf", random);
            if (sample == null || !(sample.body() instanceof GiantPlanetData brown)) {
                return null;
            }
            int[] rgb = CelestialBodyMatcher.getStarColor(sample.energy());
            StarData star = new StarData(CelestialBodyClass.M_MAIN, brown.size(), rgb[0], rgb[1], rgb[2],
                0, 2, brown.magneticFieldStrength(), sample.energy(), null, true);
            return new CelestialBodyMatcher.Preset(star, sample.time(), sample.mass(), sample.energy());
        }
        StellarTerminal.Kind kind = switch (preset) {
            case NAME_PREFIX + "white_dwarf" -> StellarTerminal.Kind.WHITE_DWARF;
            case NAME_PREFIX + "neutron_star" -> StellarTerminal.Kind.NEUTRON_STAR;
            case BLACK_HOLE -> StellarTerminal.Kind.BLACK_HOLE;
            default -> null;
        };
        if (kind == null) {
            return null;
        }
        List<StellarTrack> tracks = StellarTrackLibrary.tracks().stream()
            .filter(track -> track.definition().terminal().kind() == kind).toList();
        if (tracks.isEmpty()) {
            return null;
        }
        StellarTrack track = tracks.get(random.nextInt(tracks.size()));
        sample = CelestialBodyMatcher.sampleStellarMass(track.definition().massAnvils(), random);
        if (sample == null || !(sample.body() instanceof StarData star)) {
            return null;
        }
        StellarTerminal terminal = track.definition().terminal();
        boolean dwarf = kind == StellarTerminal.Kind.WHITE_DWARF;
        int energy = dwarf ? 47 : 64;
        int[] rgb = dwarf ? CelestialBodyMatcher.getStarColor(energy)
            : kind == StellarTerminal.Kind.BLACK_HOLE ? new int[]{0, 0, 0} : new int[]{255, 255, 255};
        CelestialBodyClass bodyClass = CelestialBodyClass.valueOf(kind.name());
        int rotation = kind == StellarTerminal.Kind.BLACK_HOLE ? 1 : Math.min(5, star.rotationSpeed() + (dwarf ? 1 : 2));
        int magnetic = Math.min(dwarf ? 5 : 6, star.magneticFieldStrength() + (dwarf ? 1 : 2));
        StarData remnant = new StarData(bodyClass, terminal.size(), rgb[0], rgb[1], rgb[2],
            star.axialTilt(), rotation, magnetic, energy, null);
        return new CelestialBodyMatcher.Preset(remnant, sample.time() + 1, terminal.massAnvils(), energy);
    }

    private static @Nullable SpecialCelestialBodyRecipe specialRecipe(String preset, Level level) {
        if (!preset.startsWith(SPECIAL_PREFIX)) {
            return null;
        }
        Identifier id = Identifier.tryParse(preset.substring(SPECIAL_PREFIX.length()));
        var recipes = RecipesRecord.getRecipes(level);
        if (id == null || recipes == null) {
            return null;
        }
        for (RecipeHolder<SpecialCelestialBodyRecipe> holder : recipes.byType(ModRecipeTypes.SPECIAL_CELESTIAL_BODY.get())) {
            if (holder.id().identifier().equals(id)) {
                return holder.value();
            }
        }
        return null;
    }
}
