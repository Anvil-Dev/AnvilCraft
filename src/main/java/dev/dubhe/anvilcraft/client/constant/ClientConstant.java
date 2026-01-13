package dev.dubhe.anvilcraft.client.constant;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.recipe.util.CodecUtil;
import net.minecraft.client.RecipeBookCategories;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientConstant {
    public static final Codec<RecipeBookCategories> CATEGORIES_CODEC = CodecUtil.enumCodecInLowerName(RecipeBookCategories.class);
}
