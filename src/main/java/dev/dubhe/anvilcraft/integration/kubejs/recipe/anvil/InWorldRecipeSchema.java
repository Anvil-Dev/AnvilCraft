package dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftKubeRecipe;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftRecipeComponents;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.IDRecipeConstructor;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipeOutcome;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipeTrigger;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.DamageAnvil;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.SetBlock;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.SpawnItem;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasBlock;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasBlockIngredient;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.item.HasItem;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.item.HasItemIngredient;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.BooleanComponent;
import dev.latvian.mods.kubejs.recipe.component.ComponentRole;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface InWorldRecipeSchema {
    @SuppressWarnings({"unused", "DuplicatedCode"})
    class InWorldKubeRecipe extends AnvilCraftKubeRecipe {
        private Vec3 offset = Vec3.ZERO;

        public InWorldKubeRecipe icon(ItemStack icon) {
            this.setValue(ICON, icon);
            this.save();
            return this;
        }

        public InWorldKubeRecipe trigger(IRecipeTrigger trigger) {
            this.setValue(TRIGGER, trigger);
            this.save();
            return this;
        }

        public InWorldKubeRecipe addConflicting(IRecipePredicate<?> predicate) {
            this.computeIfAbsent(CONFLICTING, ArrayList::new).add(predicate);
            this.save();
            return this;
        }

        public InWorldKubeRecipe addNonConflicting(IRecipePredicate<?> predicate) {
            this.computeIfAbsent(NON_CONFLICTING, ArrayList::new).add(predicate);
            this.save();
            return this;
        }

        public InWorldKubeRecipe addOutcome(IRecipeOutcome<?> outcome) {
            this.computeIfAbsent(OUTCOMES, ArrayList::new).add(outcome);
            this.save();
            return this;
        }

        public InWorldKubeRecipe priority(int priority) {
            this.setValue(PRIORITY, priority);
            this.save();
            return this;
        }

        public InWorldKubeRecipe compatible(boolean compatible) {
            this.setValue(COMPATIBLE, compatible);
            this.save();
            return this;
        }

        // 添加偏移量设置方法
        public InWorldKubeRecipe offset(Vec3 offset) {
            this.offset = offset;
            return this;
        }

        public InWorldKubeRecipe offset(double x, double y, double z) {
            return this.offset(new Vec3(x, y, z));
        }

        public InWorldKubeRecipe below(double below) {
            return this.offset(Vec3.ZERO.subtract(0, below, 0));
        }

        public InWorldKubeRecipe below() {
            return this.below(1);
        }

        public InWorldKubeRecipe above(double above) {
            return this.offset(Vec3.ZERO.add(0, above, 0));
        }

        public InWorldKubeRecipe above() {
            return this.above(1);
        }

        // HasItem 相关方法
        public InWorldKubeRecipe hasItem(ItemLike... items) {
            return this.addPredicate(HasItem.builder().of(items).offset(this.offset).build());
        }

        public InWorldKubeRecipe hasItem(Vec3 offset, ItemLike... items) {
            return this.addPredicate(HasItem.builder().of(items).offset(offset).build());
        }

        public InWorldKubeRecipe hasItem(double x, double y, double z, ItemLike... items) {
            return this.addPredicate(HasItem.builder().of(items).offset(x, y, z).build());
        }

        public InWorldKubeRecipe hasItem(String itemTag) {
            ResourceLocation rl = ResourceLocation.tryParse(itemTag);
            if (rl == null) {
                ConsoleJS.SERVER.error("Invalid item tag: " + itemTag);
                return this;
            }
            TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), rl);
            return this.addPredicate(HasItem.builder().of(tag).offset(this.offset).build());
        }

        public InWorldKubeRecipe hasItem(Vec3 offset, String itemTag) {
            ResourceLocation rl = ResourceLocation.tryParse(itemTag);
            if (rl == null) {
                ConsoleJS.SERVER.error("Invalid item tag: " + itemTag);
                return this;
            }
            TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), rl);
            return this.addPredicate(HasItem.builder().of(tag).offset(offset).build());
        }

        public InWorldKubeRecipe hasItem(double x, double y, double z, String itemTag) {
            ResourceLocation rl = ResourceLocation.tryParse(itemTag);
            if (rl == null) {
                ConsoleJS.SERVER.error("Invalid item tag: " + itemTag);
                return this;
            }
            TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), rl);
            return this.addPredicate(HasItem.builder().of(tag).offset(x, y, z).build());
        }

        // HasItemIngredient 相关方法
        public InWorldKubeRecipe hasItemIngredient(ItemLike... items) {
            return this.addPredicate(HasItemIngredient.builder().of(items).offset(this.offset).build());
        }

        public InWorldKubeRecipe hasItemIngredient(Vec3 offset, ItemLike... items) {
            return this.addPredicate(HasItemIngredient.builder().of(items).offset(offset).build());
        }

        public InWorldKubeRecipe hasItemIngredient(double x, double y, double z, ItemLike... items) {
            return this.addPredicate(HasItemIngredient.builder().of(items).offset(x, y, z).build());
        }

        public InWorldKubeRecipe hasItemIngredient(String itemTag) {
            ResourceLocation rl = ResourceLocation.tryParse(itemTag);
            if (rl == null) {
                ConsoleJS.SERVER.error("Invalid item tag: " + itemTag);
                return this;
            }
            TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), rl);
            return this.addPredicate(HasItemIngredient.builder().of(tag).offset(this.offset).build());
        }

        public InWorldKubeRecipe hasItemIngredient(Vec3 offset, String itemTag) {
            ResourceLocation rl = ResourceLocation.tryParse(itemTag);
            if (rl == null) {
                ConsoleJS.SERVER.error("Invalid item tag: " + itemTag);
                return this;
            }
            TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), rl);
            return this.addPredicate(HasItemIngredient.builder().of(tag).offset(offset).build());
        }

        public InWorldKubeRecipe hasItemIngredient(double x, double y, double z, String itemTag) {
            ResourceLocation rl = ResourceLocation.tryParse(itemTag);
            if (rl == null) {
                ConsoleJS.SERVER.error("Invalid item tag: " + itemTag);
                return this;
            }
            TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), rl);
            return this.addPredicate(HasItemIngredient.builder().of(tag).offset(x, y, z).build());
        }

        // HasBlock 相关方法
        public InWorldKubeRecipe hasBlock(Block... blocks) {
            return this.addPredicate(HasBlock.builder().of(blocks).offset(this.offset).build());
        }

        public InWorldKubeRecipe hasBlock(Vec3 offset, Block... blocks) {
            return this.addPredicate(HasBlock.builder().of(blocks).offset(offset).build());
        }

        public InWorldKubeRecipe hasBlock(double x, double y, double z, Block... blocks) {
            return this.addPredicate(HasBlock.builder().of(blocks).offset(x, y, z).build());
        }

        public InWorldKubeRecipe hasBlock(Collection<Block> blocks) {
            return this.addPredicate(HasBlock.builder().of(blocks).offset(this.offset).build());
        }

        public InWorldKubeRecipe hasBlock(Vec3 offset, Collection<Block> blocks) {
            return this.addPredicate(HasBlock.builder().of(blocks).offset(offset).build());
        }

        public InWorldKubeRecipe hasBlock(double x, double y, double z, Collection<Block> blocks) {
            return this.addPredicate(HasBlock.builder().of(blocks).offset(x, y, z).build());
        }

        public InWorldKubeRecipe hasBlock(String blockTag) {
            ResourceLocation rl = ResourceLocation.tryParse(blockTag);
            if (rl == null) {
                ConsoleJS.SERVER.error("Invalid block tag: " + blockTag);
                return this;
            }
            TagKey<Block> tag = TagKey.create(BuiltInRegistries.BLOCK.key(), rl);
            return this.addPredicate(HasBlock.builder().of(tag).offset(this.offset).build());
        }

        public InWorldKubeRecipe hasBlock(Vec3 offset, String blockTag) {
            ResourceLocation rl = ResourceLocation.tryParse(blockTag);
            if (rl == null) {
                ConsoleJS.SERVER.error("Invalid block tag: " + blockTag);
                return this;
            }
            TagKey<Block> tag = TagKey.create(BuiltInRegistries.BLOCK.key(), rl);
            return this.addPredicate(HasBlock.builder().of(tag).offset(offset).build());
        }

        public InWorldKubeRecipe hasBlock(double x, double y, double z, String blockTag) {
            ResourceLocation rl = ResourceLocation.tryParse(blockTag);
            if (rl == null) {
                ConsoleJS.SERVER.error("Invalid block tag: " + blockTag);
                return this;
            }
            TagKey<Block> tag = TagKey.create(BuiltInRegistries.BLOCK.key(), rl);
            return this.addPredicate(HasBlock.builder().of(tag).offset(x, y, z).build());
        }

        public <T extends Comparable<T>> InWorldKubeRecipe hasBlock(Vec3 offset, @NotNull BlockState state) {
            HasBlock.Builder builder = HasBlock.builder();
            Block block = state.getBlock();
            builder.of(block);
            builder.offset(offset);
            BlockState defaultState = block.defaultBlockState();
            for (Property<?> property : state.getProperties()) {
                Comparable<?> value = state.getValue(property);
                Comparable<?> defaultValue = defaultState.getValue(property);
                if (value.equals(defaultValue)) continue;
                //noinspection unchecked
                builder.with((Property<T>) property, (T) value);
            }
            return this.addPredicate(builder.build());
        }

        public <T extends Comparable<T>> InWorldKubeRecipe hasBlock(double x, double y, double z, BlockState state) {
            return this.hasBlock(new Vec3(x, y, z), state);
        }

        public <T extends Comparable<T>> InWorldKubeRecipe hasBlock(BlockState state) {
            return this.hasBlock(this.offset, state);
        }

        // HasBlockIngredient 相关方法
        public InWorldKubeRecipe hasBlockIngredient(Block... blocks) {
            return this.addPredicate(HasBlockIngredient.builder().of(blocks).offset(this.offset).build());
        }

        public InWorldKubeRecipe hasBlockIngredient(Vec3 offset, Block... blocks) {
            return this.addPredicate(HasBlockIngredient.builder().of(blocks).offset(offset).build());
        }

        public InWorldKubeRecipe hasBlockIngredient(double x, double y, double z, Block... blocks) {
            return this.addPredicate(HasBlockIngredient.builder().of(blocks).offset(new Vec3(x, y, z)).build());
        }

        public InWorldKubeRecipe hasBlockIngredient(Collection<Block> blocks) {
            return this.addPredicate(HasBlockIngredient.builder().of(blocks).offset(this.offset).build());
        }

        public InWorldKubeRecipe hasBlockIngredient(Vec3 offset, Collection<Block> blocks) {
            return this.addPredicate(HasBlockIngredient.builder().of(blocks).offset(offset).build());
        }

        public InWorldKubeRecipe hasBlockIngredient(double x, double y, double z, Collection<Block> blocks) {
            return this.addPredicate(HasBlockIngredient.builder().of(blocks).offset(new Vec3(x, y, z)).build());
        }

        public InWorldKubeRecipe hasBlockIngredient(String blockTag) {
            ResourceLocation rl = ResourceLocation.tryParse(blockTag);
            if (rl == null) {
                ConsoleJS.SERVER.error("Invalid block tag: " + blockTag);
                return this;
            }
            TagKey<Block> tag = TagKey.create(BuiltInRegistries.BLOCK.key(), rl);
            return this.addPredicate(HasBlockIngredient.builder().of(tag).offset(this.offset).build());
        }

        public InWorldKubeRecipe hasBlockIngredient(Vec3 offset, String blockTag) {
            ResourceLocation rl = ResourceLocation.tryParse(blockTag);
            if (rl == null) {
                ConsoleJS.SERVER.error("Invalid block tag: " + blockTag);
                return this;
            }
            TagKey<Block> tag = TagKey.create(BuiltInRegistries.BLOCK.key(), rl);
            return this.addPredicate(HasBlockIngredient.builder().of(tag).offset(offset).build());
        }

        public InWorldKubeRecipe hasBlockIngredient(double x, double y, double z, String blockTag) {
            ResourceLocation rl = ResourceLocation.tryParse(blockTag);
            if (rl == null) {
                ConsoleJS.SERVER.error("Invalid block tag: " + blockTag);
                return this;
            }
            TagKey<Block> tag = TagKey.create(BuiltInRegistries.BLOCK.key(), rl);
            return this.addPredicate(HasBlockIngredient.builder().of(tag).offset(new Vec3(x, y, z)).build());
        }

        public <T extends Comparable<T>> InWorldKubeRecipe hasBlockIngredient(Vec3 offset, @NotNull BlockState state) {
            HasBlockIngredient.Builder builder = HasBlockIngredient.builder();
            Block block = state.getBlock();
            BlockState defaultState = block.defaultBlockState();
            builder.of(block);
            builder.offset(offset);
            for (Property<?> property : state.getProperties()) {
                Comparable<?> value = state.getValue(property);
                if (value.equals(defaultState.getValue(property))) continue;
                //noinspection unchecked
                builder.with((Property<T>) property, (T) value);
            }
            return this.addPredicate(builder.build());
        }

        public <T extends Comparable<T>> InWorldKubeRecipe hasBlockIngredient(double x, double y, double z, BlockState state) {
            return this.hasBlockIngredient(new Vec3(x, y, z), state);
        }

        public <T extends Comparable<T>> InWorldKubeRecipe hasBlockIngredient(BlockState state) {
            return this.hasBlockIngredient(this.offset, state);
        }

        // HasCauldron 相关方法

        public InWorldKubeRecipe hasCauldron() {
            return this.addPredicate(HasCauldron.builder().empty().offset(this.offset).build());
        }

        public InWorldKubeRecipe hasCauldron(Vec3 offset) {
            return this.addPredicate(HasCauldron.builder().empty().offset(offset).build());
        }

        public InWorldKubeRecipe hasCauldron(double x, double y, double z) {
            return this.addPredicate(HasCauldron.builder().empty().offset(new Vec3(x, y, z)).build());
        }

        public InWorldKubeRecipe hasCauldron(String fluid) {
            ResourceLocation rl = ResourceLocation.tryParse(fluid);
            if (rl == null) {
                ConsoleJS.SERVER.error("Invalid fluid: " + fluid);
                return this;
            }
            return this.addPredicate(HasCauldron.builder().fluid(rl).offset(this.offset).build());
        }

        public InWorldKubeRecipe hasCauldron(Vec3 offset, String fluid) {
            ResourceLocation rl = ResourceLocation.tryParse(fluid);
            if (rl == null) {
                ConsoleJS.SERVER.error("Invalid fluid: " + fluid);
                return this;
            }
            return this.addPredicate(HasCauldron.builder().fluid(rl).offset(offset).build());
        }

        public InWorldKubeRecipe hasCauldron(double x, double y, double z, String fluid) {
            ResourceLocation rl = ResourceLocation.tryParse(fluid);
            if (rl == null) {
                ConsoleJS.SERVER.error("Invalid fluid: " + fluid);
                return this;
            }
            return this.addPredicate(HasCauldron.builder().fluid(rl).offset(new Vec3(x, y, z)).build());
        }

        public InWorldKubeRecipe hasCauldron(String fluid, int consume) {
            ResourceLocation rl = ResourceLocation.tryParse(fluid);
            if (rl == null) {
                ConsoleJS.SERVER.error("Invalid fluid: " + fluid);
                return this;
            }
            return this.addPredicate(HasCauldron.builder().fluid(rl).offset(this.offset).consume(consume).build());
        }

        public InWorldKubeRecipe hasCauldron(Vec3 offset, String fluid, int consume) {
            ResourceLocation rl = ResourceLocation.tryParse(fluid);
            if (rl == null) {
                ConsoleJS.SERVER.error("Invalid fluid: " + fluid);
                return this;
            }
            return this.addPredicate(HasCauldron.builder().fluid(rl).offset(offset).consume(consume).build());
        }

        public InWorldKubeRecipe hasCauldron(double x, double y, double z, String fluid, int consume) {
            ResourceLocation rl = ResourceLocation.tryParse(fluid);
            if (rl == null) {
                ConsoleJS.SERVER.error("Invalid fluid: " + fluid);
                return this;
            }
            return this.addPredicate(HasCauldron.builder().fluid(rl).offset(new Vec3(x, y, z)).consume(consume).build());
        }

        public InWorldKubeRecipe hasCauldron(Block cauldron) {
            return this.addPredicate(HasCauldron.builder().cauldron(cauldron).offset(this.offset).build());
        }

        public InWorldKubeRecipe hasCauldron(Vec3 offset, Block cauldron) {
            return this.addPredicate(HasCauldron.builder().cauldron(cauldron).offset(offset).build());
        }

        public InWorldKubeRecipe hasCauldron(double x, double y, double z, Block cauldron) {
            return this.addPredicate(HasCauldron.builder().cauldron(cauldron).offset(new Vec3(x, y, z)).build());
        }

        public InWorldKubeRecipe hasCauldron(Block cauldron, int consume) {
            return this.addPredicate(HasCauldron.builder().cauldron(cauldron).offset(this.offset).consume(consume).build());
        }

        public InWorldKubeRecipe hasCauldron(Vec3 offset, Block cauldron, int consume) {
            return this.addPredicate(HasCauldron.builder().cauldron(cauldron).offset(offset).consume(consume).build());
        }

        public InWorldKubeRecipe hasCauldron(double x, double y, double z, Block cauldron, int consume) {
            return this.addPredicate(HasCauldron.builder().cauldron(cauldron).offset(new Vec3(x, y, z)).consume(consume).build());
        }

        // Outcome 相关方法
        public InWorldKubeRecipe spawnItem(Vec3 offset, double chance, ItemStack stack) {
            return this.addOutcome(SpawnItem.builder().offset(offset).count((float) chance).item(stack).build());
        }

        public InWorldKubeRecipe spawnItem(Vec3 offset, ItemStack stack) {
            return this.spawnItem(offset, 1, stack);
        }

        public InWorldKubeRecipe spawnItem(double x, double y, double z, double chance, ItemStack stack) {
            return this.spawnItem(new Vec3(x, y, z), chance, stack);
        }

        public InWorldKubeRecipe spawnItem(double x, double y, double z, ItemStack stack) {
            return this.spawnItem(new Vec3(x, y, z), stack);
        }

        public InWorldKubeRecipe spawnItem(ItemStack stack) {
            return this.spawnItem(this.offset, stack);
        }

        public InWorldKubeRecipe setBlock(Vec3 offset, double chance, BlockState state) {
            return this.addOutcome(SetBlock.builder().block(state).offset(offset).chance((float) chance).build());
        }

        public InWorldKubeRecipe setBlock(Vec3 offset, BlockState state) {
            return this.setBlock(offset, 1, state);
        }

        public InWorldKubeRecipe setBlock(double x, double y, double z, double chance, BlockState state) {
            return this.setBlock(new Vec3(x, y, z), chance, state);
        }

        public InWorldKubeRecipe setBlock(double x, double y, double z, BlockState state) {
            return this.setBlock(new Vec3(x, y, z), state);
        }

        public InWorldKubeRecipe setBlock(BlockState state) {
            return this.setBlock(this.offset, state);
        }

        public InWorldKubeRecipe damageAnvil() {
            return this.addOutcome(new DamageAnvil());
        }

        // 通用谓词添加方法
        public InWorldKubeRecipe addPredicate(@NotNull IRecipePredicate<?> predicate) {
            if (predicate.getType().conflict()) {
                this.computeIfAbsent(CONFLICTING, ArrayList::new).add(predicate);
            } else {
                this.computeIfAbsent(NON_CONFLICTING, ArrayList::new).add(predicate);
            }
            this.save();
            return this;
        }

        @Override
        protected void validate() {
            if (getValue(TRIGGER) == null) {
                throw new KubeRuntimeException("Trigger is Empty!").source(sourceLine);
            }
        }
    }

    RecipeKey<ItemStack> ICON = ItemStackComponent.STRICT_ITEM_STACK
        .key("icon", ComponentRole.OTHER)
        .optional(ItemStack.EMPTY)
        .alwaysWrite();
    RecipeKey<IRecipeTrigger> TRIGGER = AnvilCraftRecipeComponents.TRIGGER
        .key("trigger", ComponentRole.INPUT)
        .defaultOptional();
    RecipeKey<List<IRecipePredicate<?>>> CONFLICTING = AnvilCraftRecipeComponents.RECIPE_PREDICATE
        .asList()
        .key("conflicting", ComponentRole.INPUT)
        .defaultOptional();
    RecipeKey<List<IRecipePredicate<?>>> NON_CONFLICTING = AnvilCraftRecipeComponents.RECIPE_PREDICATE
        .asList()
        .key("non_conflicting", ComponentRole.INPUT)
        .defaultOptional();
    RecipeKey<List<IRecipeOutcome<?>>> OUTCOMES = AnvilCraftRecipeComponents.RECIPE_OUTCOME
        .asList()
        .key("outcomes", ComponentRole.OUTPUT)
        .defaultOptional();
    RecipeKey<Integer> PRIORITY = NumberComponent.INT
        .key("priority", ComponentRole.OTHER)
        .optional(0)
        .alwaysWrite();
    RecipeKey<Boolean> COMPATIBLE = BooleanComponent.BOOLEAN
        .key("compatible", ComponentRole.OTHER)
        .optional(true)
        .alwaysWrite();

    RecipeSchema SCHEMA = new RecipeSchema(ICON, TRIGGER, CONFLICTING, NON_CONFLICTING, OUTCOMES, PRIORITY, COMPATIBLE)
        .factory(new KubeRecipeFactory(AnvilCraft.of("in_world"), InWorldKubeRecipe.class, InWorldKubeRecipe::new))
        .constructor(ICON, TRIGGER, CONFLICTING, NON_CONFLICTING, OUTCOMES, PRIORITY, COMPATIBLE)
        .constructor(TRIGGER, CONFLICTING, NON_CONFLICTING, OUTCOMES)
        .constructor(new IDRecipeConstructor())
        .constructor();
}