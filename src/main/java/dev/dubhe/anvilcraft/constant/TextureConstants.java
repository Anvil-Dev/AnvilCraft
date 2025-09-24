package dev.dubhe.anvilcraft.constant;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.resources.ResourceLocation;

public class TextureConstants {
    public static final ResourceLocation SHULKER_CONTAINER_BG = TextureConstants.texture("gui/container/shulker_container/background");

    public static final ResourceLocation SHULKER_CONTAINER_CANCEL = TextureConstants.texture("gui/container/shulker_container/cancel");
    public static final ResourceLocation SHULKER_CONTAINER_CONFIRM = TextureConstants.texture("gui/container/shulker_container/confirm");

    public static final ResourceLocation SHULKER_CONTAINER_CATEGORY = TextureConstants.texture("gui/container/shulker_container/category");
    public static final ResourceLocation SHULKER_CONTAINER_CATEGORY_ADD = TextureConstants.texture("gui/container/shulker_container/category_add");
    public static final ResourceLocation SHULKER_CONTAINER_CATEGORY_SETTING = TextureConstants.texture("gui/container/shulker_container/category_setting");
    public static final ResourceLocation SHULKER_CONTAINER_CATEGORY_SETTING_BG = TextureConstants.texture("gui/container/shulker_container/category_setting_background");

    public static final ResourceLocation SHULKER_CONTAINER_NBT_FOLD = TextureConstants.texture("gui/container/shulker_container/nbt_fold");
    public static final ResourceLocation SHULKER_CONTAINER_NBT_UNFOLD = TextureConstants.texture("gui/container/shulker_container/nbt_unfold");

    public static final ResourceLocation SHULKER_CONTAINER_PUT = TextureConstants.texture("gui/container/shulker_container/put");
    public static final ResourceLocation SHULKER_CONTAINER_TAKE = TextureConstants.texture("gui/container/shulker_container/take");

    public static final ResourceLocation SHULKER_CONTAINER_SEQUENTIAL_ORDER = TextureConstants.texture("gui/container/shulker_container/sequential_order");
    public static final ResourceLocation SHULKER_CONTAINER_REVERSE_ORDER = TextureConstants.texture("gui/container/shulker_container/reverse_order");

    public static final ResourceLocation SHULKER_CONTAINER_SEARCH_RETENTION = TextureConstants.texture("gui/container/shulker_container/search_retention");
    public static final ResourceLocation SHULKER_CONTAINER_SEARCH_CLEAR = TextureConstants.texture("gui/container/shulker_container/search_clear");

    public static final ResourceLocation SHULKER_CONTAINER_SORT_BY_NUMBER = TextureConstants.texture("gui/container/shulker_container/sort_by_number");
    public static final ResourceLocation SHULKER_CONTAINER_SORT_BY_MOD = TextureConstants.texture("gui/container/shulker_container/sort_by_mod");
    public static final ResourceLocation SHULKER_CONTAINER_SORT_BY_NAME = TextureConstants.texture("gui/container/shulker_container/sort_by_name");

    public static final ResourceLocation SHULKER_CONTAINER_SLIDER_BIG = TextureConstants.texture("gui/container/shulker_container/slider_big");
    public static final ResourceLocation SHULKER_CONTAINER_SLIDER_SMALL = TextureConstants.texture("gui/container/shulker_container/slider_small");

    public static final ResourceLocation SHULKER_CONTAINER_SLIDER_UPGRADE = TextureConstants.texture("gui/container/shulker_container/upgrade");

    private static ResourceLocation texture(String path) {
        return AnvilCraft.of("textures/" + path + ".png");
    }
}
