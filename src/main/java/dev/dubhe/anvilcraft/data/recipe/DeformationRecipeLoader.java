package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.frost.DeformationRecipe;
import dev.dubhe.anvilcraft.recipe.frost.EmptyFrostMaterialPredicate;
import dev.dubhe.anvilcraft.recipe.frost.RepairMaterialFrostMaterialPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

public class DeformationRecipeLoader {
    private static final List<String> ARMOR_PARTS = List.of(
        "helmet",
        "chestplate",
        "leggings",
        "boots"
    );
    private static final ResourceLocation WOODEN = ResourceLocation.withDefaultNamespace("wooden");
    private static final ResourceLocation STONE = ResourceLocation.withDefaultNamespace("stone");
    private static final ResourceLocation IRON = ResourceLocation.withDefaultNamespace("iron");
    private static final ResourceLocation GOLDEN = ResourceLocation.withDefaultNamespace("golden");
    private static final ResourceLocation DIAMOND = ResourceLocation.withDefaultNamespace("diamond");
    private static final ResourceLocation NETHERITE = ResourceLocation.withDefaultNamespace("netherite");
    private static final ResourceLocation CHAINMAIL = ResourceLocation.withDefaultNamespace("chainmail");
    private static final ResourceLocation AMETHYST = AnvilCraft.of("amethyst");
    private static final ResourceLocation ROYAL_STEEL = AnvilCraft.of("royal_steel");
    private static final ResourceLocation FROST_METAL = AnvilCraft.of("frost_metal");
    private static final ResourceLocation EMBER_METAL = AnvilCraft.of("ember_metal");

    public static void init(RegistrumRecipeProvider provider) {
        DeformationRecipeLoader.registerTools(provider, DeformationRecipeLoader.WOODEN);
        DeformationRecipeLoader.registerTools(provider, DeformationRecipeLoader.STONE);
        DeformationRecipeLoader.registerTools(provider, DeformationRecipeLoader.IRON);
        DeformationRecipeLoader.registerTools(provider, DeformationRecipeLoader.GOLDEN);
        DeformationRecipeLoader.registerTools(provider, DeformationRecipeLoader.DIAMOND);
        DeformationRecipeLoader.registerTools(provider, DeformationRecipeLoader.NETHERITE);
        DeformationRecipeLoader.registerTools(provider, DeformationRecipeLoader.AMETHYST);
        DeformationRecipeLoader.registerTools(provider, DeformationRecipeLoader.ROYAL_STEEL);
        DeformationRecipeLoader.registerTools(provider, DeformationRecipeLoader.FROST_METAL);
        DeformationRecipeLoader.registerTools(provider, DeformationRecipeLoader.EMBER_METAL);

        DeformationRecipeLoader.registerArmors(provider, DeformationRecipeLoader.CHAINMAIL);
        DeformationRecipeLoader.registerArmors(provider, DeformationRecipeLoader.IRON);
        DeformationRecipeLoader.registerArmors(provider, DeformationRecipeLoader.GOLDEN);
        DeformationRecipeLoader.registerArmors(provider, DeformationRecipeLoader.DIAMOND);
        DeformationRecipeLoader.registerArmors(provider, DeformationRecipeLoader.NETHERITE);

        DeformationRecipe.builder()
            .material(new EmptyFrostMaterialPredicate())
            .input(Items.BOW)
            .input(Items.CROSSBOW)
            .save(provider, "bowlikes");
    }

    private static void registerTools(RegistrumRecipeProvider provider, ResourceLocation prefix) {
        DeformationRecipeLoader.register(
            provider,
            DeformationRecipeLoader.items(prefix, PermutationRecipeLoader.WEAPONS_AND_TOOLS),
            1,
            prefix.getPath() + "_weapons_and_tools"
        );
    }

    private static void registerArmors(RegistrumRecipeProvider provider, ResourceLocation prefix) {
        DeformationRecipeLoader.register(
            provider,
            DeformationRecipeLoader.items(prefix, DeformationRecipeLoader.ARMOR_PARTS),
            2,
            prefix.getPath() + "_armors"
        );
    }

    /**
     * 生成一组形变配方，材料由装备自身的维修材料决定，浮霜金属锭可以作为通用维修材料，但需要双倍消耗。
     *
     * @param inputs 可以互相形变的装备
     * @param cost   维修材料的消耗数量
     * @param id     配方 id
     */
    private static void register(RegistrumRecipeProvider provider, List<Item> inputs, int cost, String id) {
        DeformationRecipe.Builder builder = DeformationRecipe.builder().material(RepairMaterialFrostMaterialPredicate.allowUniversal(cost));
        inputs.forEach(builder::input);
        builder.save(provider, id);
    }

    private static List<Item> items(ResourceLocation prefix, List<String> bases) {
        return bases.stream().map(base -> BuiltInRegistries.ITEM.get(prefix.withSuffix("_" + base))).toList();
    }
}
