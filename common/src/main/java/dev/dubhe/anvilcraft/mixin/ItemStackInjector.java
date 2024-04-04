package dev.dubhe.anvilcraft.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.dubhe.anvilcraft.util.IItemStackInjector;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemStack.class)
abstract class ItemStackInjector implements IItemStackInjector {
    @Shadow
    public abstract boolean hasTag();

    @Shadow
    public abstract @Nullable CompoundTag getTag();

    @Shadow
    public abstract int getCount();

    @Shadow
    public abstract void setTag(@Nullable CompoundTag compoundTag);

    @Shadow
    public abstract CompoundTag getOrCreateTag();

    @Shadow
    public abstract Item getItem();

    @Override
    public JsonElement anvilcraft$toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("item", BuiltInRegistries.ITEM.getKey(this.getItem()).toString());
        if (this.getCount() > 1) object.addProperty("count", this.getCount());
        if (this.hasTag()) object.addProperty("data", this.getOrCreateTag().toString());
        return object;
    }
}
