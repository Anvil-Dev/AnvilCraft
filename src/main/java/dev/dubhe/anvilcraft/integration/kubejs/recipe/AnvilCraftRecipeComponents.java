package dev.dubhe.anvilcraft.integration.kubejs.recipe;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.recipe.trigger.IRecipeTrigger;
import dev.anvilcraft.lib.util.CodecUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.multiblock.BlockPattern;
import dev.dubhe.anvilcraft.recipe.transform.NumericTagValuePredicate;
import dev.dubhe.anvilcraft.recipe.transform.TagModification;
import dev.dubhe.anvilcraft.recipe.transform.TransformOptions;
import dev.dubhe.anvilcraft.recipe.transform.TransformResult;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public class AnvilCraftRecipeComponents {

    public static final RecipeComponent<ResourceLocation> RESOURCE_LOCATION = new RecipeComponent<>() {

        public static final RecipeComponentType<ResourceLocation> TYPE = RecipeComponentType.unit(
            AnvilCraft.of("resource_location"),
            RESOURCE_LOCATION
        );

        @Override
        public RecipeComponentType<?> type() {
            return TYPE;
        }

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
        public static final RecipeComponentType<IRecipeTrigger> TYPE = RecipeComponentType.unit(
            AnvilCraft.of("trigger"),
            TRIGGER
        );

        @Override
        public RecipeComponentType<?> type() {
            return TYPE;
        }

        @Override
        public Codec<IRecipeTrigger> codec() {
            return IRecipeTrigger.CODEC;
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
        public static final RecipeComponentType<IRecipePredicate<?>> TYPE = RecipeComponentType.unit(
            AnvilCraft.of("recipe_predicate"),
            RECIPE_PREDICATE
        );

        @Override
        public RecipeComponentType<?> type() {
            return TYPE;
        }

        @Override
        public Codec<IRecipePredicate<?>> codec() {
            return IRecipePredicate.CODEC;
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
        public static final RecipeComponentType<IRecipeOutcome<?>> TYPE = RecipeComponentType.unit(
            AnvilCraft.of("recipe_outcome"),
            RECIPE_OUTCOME
        );

        @Override
        public RecipeComponentType<?> type() {
            return TYPE;
        }

        @Override
        public Codec<IRecipeOutcome<?>> codec() {
            return IRecipeOutcome.CODEC;
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
        public static final RecipeComponentType<BlockPattern> TYPE = RecipeComponentType.unit(
            AnvilCraft.of("block_pattern"),
            BLOCK_PATTERN
        );

        @Override
        public RecipeComponentType<?> type() {
            return TYPE;
        }

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
        public static final RecipeComponentType<EntityType<?>> TYPE = RecipeComponentType.unit(
            AnvilCraft.of("entity_type"),
            ENTITY_TYPE
        );

        @Override
        public RecipeComponentType<?> type() {
            return TYPE;
        }

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
        public static final RecipeComponentType<TransformResult> TYPE = RecipeComponentType.unit(
            AnvilCraft.of("transform_result"),
            TRANSFORM_RESULT
        );

        @Override
        public RecipeComponentType<?> type() {
            return TYPE;
        }

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
        public static final RecipeComponentType<NumericTagValuePredicate> TYPE = RecipeComponentType.unit(
            AnvilCraft.of("numeric_tag_value_predicate"),
            NUMERIC_TAG_VALUE_PREDICATE
        );

        @Override
        public RecipeComponentType<?> type() {
            return TYPE;
        }

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
        public static final RecipeComponentType<TagModification> TYPE = RecipeComponentType.unit(
            AnvilCraft.of("tag_modification"),
            TAG_MODIFICATION
        );

        @Override
        public RecipeComponentType<?> type() {
            return TYPE;
        }

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
        public static final RecipeComponentType<TransformOptions> TYPE = RecipeComponentType.unit(
            AnvilCraft.of("transform_options"),
            TRANSFORM_OPTIONS
        );

        @Override
        public RecipeComponentType<?> type() {
            return TYPE;
        }
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