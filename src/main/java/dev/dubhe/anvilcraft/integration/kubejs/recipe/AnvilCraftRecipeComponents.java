package dev.dubhe.anvilcraft.integration.kubejs.recipe;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipeOutcome;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipeTrigger;
import dev.dubhe.anvilcraft.recipe.multiblock.BlockPattern;
import dev.dubhe.anvilcraft.recipe.transform.NumericTagValuePredicate;
import dev.dubhe.anvilcraft.recipe.transform.TagModification;
import dev.dubhe.anvilcraft.recipe.transform.TransformOptions;
import dev.dubhe.anvilcraft.recipe.transform.TransformResult;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public class AnvilCraftRecipeComponents {

    public static final RecipeComponent<ResourceLocation> RESOURCE_LOCATION = new RecipeComponent<>() {

        @Override
        public Codec<ResourceLocation> codec() {
            return ResourceLocation.CODEC;
        }

        @Override
        public TypeInfo typeInfo() {
            return TypeInfo.of(ResourceLocation.class);
        }

        @Override
        public String toString() {
            return "resource_location";
        }
    };

    public static final RecipeComponent<IRecipeTrigger> TRIGGER = new RecipeComponent<>() {
        @Override
        public Codec<IRecipeTrigger> codec() {
            return CodecUtil.TRIGGER_CODEC;
        }

        @Override
        public TypeInfo typeInfo() {
            return TypeInfo.of(IRecipeTrigger.class);
        }

        @Override
        public String toString() {
            return "trigger";
        }
    };

    public static final RecipeComponent<IRecipePredicate<?>> RECIPE_PREDICATE = new RecipeComponent<>() {
        @Override
        public Codec<IRecipePredicate<?>> codec() {
            return CodecUtil.PREDICATE_CODEC;
        }

        @Override
        public TypeInfo typeInfo() {
            return TypeInfo.of(IRecipePredicate.class);
        }

        @Override
        public String toString() {
            return "recipe_predicate";
        }
    };

    public static final RecipeComponent<IRecipeOutcome<?>> RECIPE_OUTCOME = new RecipeComponent<>() {
        @Override
        public Codec<IRecipeOutcome<?>> codec() {
            return CodecUtil.OUTCOME_CODEC;
        }

        @Override
        public TypeInfo typeInfo() {
            return TypeInfo.of(IRecipeOutcome.class);
        }

        @Override
        public String toString() {
            return "recipe_outcome";
        }
    };

    public static final RecipeComponent<BlockPattern> BLOCK_PATTERN = new RecipeComponent<>() {

        @Override
        public Codec<BlockPattern> codec() {
            return BlockPattern.CODEC;
        }

        @Override
        public TypeInfo typeInfo() {
            return TypeInfo.of(BlockPattern.class);
        }

        @Override
        public String toString() {
            return "block_pattern";
        }
    };

    public static final RecipeComponent<EntityType<?>> ENTITY_TYPE = new RecipeComponent<>() {

        @Override
        public Codec<EntityType<?>> codec() {
            return CodecUtil.ENTITY_CODEC;
        }

        @Override
        public TypeInfo typeInfo() {
            return TypeInfo.of(EntityType.class);
        }

        @Override
        public String toString() {
            return "entity_type";
        }
    };

    public static final RecipeComponent<TransformResult> TRANSFORM_RESULT = new RecipeComponent<>() {

        @Override
        public Codec<TransformResult> codec() {
            return TransformResult.CODEC;
        }

        @Override
        public TypeInfo typeInfo() {
            return TypeInfo.of(TransformResult.class);
        }

        @Override
        public String toString() {
            return "transform_result";
        }
    };

    public static final RecipeComponent<NumericTagValuePredicate> NUMERIC_TAG_VALUE_PREDICATE = new RecipeComponent<>() {

        @Override
        public Codec<NumericTagValuePredicate> codec() {
            return NumericTagValuePredicate.CODEC;
        }

        @Override
        public TypeInfo typeInfo() {
            return TypeInfo.of(NumericTagValuePredicate.class);
        }

        @Override
        public String toString() {
            return "numeric_tag_value_predicate";
        }
    };

    public static final RecipeComponent<TagModification> TAG_MODIFICATION = new RecipeComponent<>() {

        @Override
        public Codec<TagModification> codec() {
            return TagModification.CODEC;
        }

        @Override
        public TypeInfo typeInfo() {
            return TypeInfo.of(TagModification.class);
        }

        @Override
        public String toString() {
            return "tag_modification";
        }
    };

    public static final RecipeComponent<TransformOptions> TRANSFORM_OPTIONS = new RecipeComponent<>() {
        @Override
        public Codec<TransformOptions> codec() {
            return TransformOptions.CODEC;
        }

        @Override
        public TypeInfo typeInfo() {
            return TypeInfo.of(TransformOptions.class);
        }

        @Override
        public String toString() {
            return "transform_options";
        }
    };
}