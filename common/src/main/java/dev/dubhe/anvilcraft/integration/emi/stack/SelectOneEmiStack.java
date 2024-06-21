package dev.dubhe.anvilcraft.integration.emi.stack;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SelectOne;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SetBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnItem;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.screen.tooltip.EmiTooltip;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SelectOneEmiStack extends EmiStack {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private final List<EmiStack> stacks = new ArrayList<>();
    private float maxChance = 0;

    private SelectOneEmiStack(@NotNull List<EmiStack> stacks) {
        for (EmiStack stack : stacks) {
            this.stacks.add(stack.copy());
            this.maxChance += stack.getChance();
        }
    }

    private SelectOneEmiStack(@NotNull SelectOne selectOne) {
        for (RecipeOutcome outcome : selectOne.getOutcomes()) {
            if (outcome instanceof SpawnItem item) {
                this.stacks.add(EmiStack.of(item.getResult()).setChance((float) outcome.getChance()));
            }
            if (outcome instanceof SetBlock block) {
                this.stacks.add(BlockStateEmiStack.of(block.getResult()).setChance((float) outcome.getChance()));
            }
        }
        for (EmiStack stack : this.stacks) {
            this.maxChance += stack.getChance();
        }
    }

    public static @NotNull SelectOneEmiStack of(@NotNull SelectOne selectOne) {
        return new SelectOneEmiStack(selectOne);
    }

    @Override
    public SelectOneEmiStack copy() {
        return new SelectOneEmiStack(this.stacks);
    }

    private @Nullable EmiIngredient get() {
        if (MINECRAFT.level == null || this.stacks.isEmpty()) return null;
        int index = (int) Math.floor((MINECRAFT.level.getGameTime() % (this.stacks.size() * 20)) / 20.0);
        return this.stacks.get(index);
    }

    @Override
    public void render(GuiGraphics draw, int x, int y, float delta, int flags) {
        EmiIngredient ingredient = this.get();
        if (ingredient != null) ingredient.render(draw, x, y, delta, flags);
    }

    @Override
    public boolean isEmpty() {
        return this.stacks.isEmpty();
    }

    @Override
    public CompoundTag getNbt() {
        return new CompoundTag();
    }

    @Override
    public float getChance() {
        EmiIngredient ingredient = this.get();
        if (ingredient != null) return ingredient.getChance();
        return super.getChance();
    }

    @Override
    public long getAmount() {
        EmiIngredient ingredient = this.get();
        if (ingredient != null) return ingredient.getAmount();
        return super.getAmount();
    }

    @Override
    public Object getKey() {
        return "SelectOne$" + this.stacks.hashCode();
    }

    @Override
    public boolean isEqual(EmiStack stack) {
        for (EmiStack stack1 : this.stacks) {
            if (stack1.isEqual(stack)) return true;
        }
        return super.isEqual(stack);
    }

    @Override
    public ResourceLocation getId() {
        return AnvilCraft.of("select_one_" + this.stacks.hashCode());
    }

    @Override
    public List<ClientTooltipComponent> getTooltip() {
        List<ClientTooltipComponent> list;
        EmiIngredient ingredient = this.get();
        if (ingredient != null) {
            list = ingredient.getTooltip();
            list.add(EmiTooltip.chance("produce", ingredient.getChance() / this.maxChance));
        } else list = List.of();
        return list;
    }

    @Override
    public List<Component> getTooltipText() {
        return List.of();
    }

    @Override
    public Component getName() {
        return Component.literal("SelectOne");
    }
}
