package dev.dubhe.anvilcraft.util;

import com.google.gson.JsonElement;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public interface Serializable {
    void toNetwork(@NotNull FriendlyByteBuf buffer);

    @NotNull JsonElement toJson();
}
