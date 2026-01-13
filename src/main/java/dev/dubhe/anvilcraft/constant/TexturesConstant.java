package dev.dubhe.anvilcraft.constant;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.resources.ResourceLocation;

public class TexturesConstant {
    public static final ResourceLocation DISABLED_SLOT = texture("gui/container/machine/disabled_slot");
    public static final ResourceLocation ERROR_SPRITE = texture("gui/container/smithing/error");

    public static final ResourceLocation EMBER_GRINDSTONE_BUTTON = texture(
        "gui/container/smithing/ember_grindstone_button"
    );
    public static final ResourceLocation EMBER_GRINDSTONE_SLIDER = texture(
        "gui/container/smithing/ember_grindstone_slider"
    );
    public static final ResourceLocation SHULKER_CONTAINER_BG = texture(
        "gui/container/shulker_container/background"
    );
    public static final ResourceLocation SHULKER_CONTAINER_CANCEL = texture(
        "gui/container/shulker_container/cancel"
    );
    public static final ResourceLocation SHULKER_CONTAINER_CONFIRM = texture(
        "gui/container/shulker_container/confirm"
    );
    public static final ResourceLocation SHULKER_CONTAINER_CATEGORY = texture(
        "gui/container/shulker_container/category"
    );
    public static final ResourceLocation SHULKER_CONTAINER_CATEGORY_ADD = texture(
        "gui/container/shulker_container/category_add"
    );
    public static final ResourceLocation SHULKER_CONTAINER_CATEGORY_SETTING = texture(
        "gui/container/shulker_container/category_setting"
    );
    public static final ResourceLocation SHULKER_CONTAINER_CATEGORY_SETTING_BG = texture(
        "gui/container/shulker_container/category_setting_background"
    );
    public static final ResourceLocation SHULKER_CONTAINER_NBT_FOLD = texture(
        "gui/container/shulker_container/nbt_fold"
    );
    public static final ResourceLocation SHULKER_CONTAINER_NBT_UNFOLD = texture(
        "gui/container/shulker_container/nbt_unfold"
    );
    public static final ResourceLocation SHULKER_CONTAINER_PUT = texture(
        "gui/container/shulker_container/put"
    );
    public static final ResourceLocation SHULKER_CONTAINER_TAKE = texture(
        "gui/container/shulker_container/take"
    );
    public static final ResourceLocation SHULKER_CONTAINER_SEQUENTIAL_ORDER = texture(
        "gui/container/shulker_container/sequential_order"
    );
    public static final ResourceLocation SHULKER_CONTAINER_REVERSE_ORDER = texture(
        "gui/container/shulker_container/reverse_order"
    );
    public static final ResourceLocation SHULKER_CONTAINER_SEARCH_CLEAR = texture(
        "gui/container/shulker_container/search_clear"
    );
    public static final ResourceLocation SHULKER_CONTAINER_SEARCH_RETENTION = texture(
        "gui/container/shulker_container/search_retention"
    );
    public static final ResourceLocation SHULKER_CONTAINER_SORT_BY_NUMBER = texture(
        "gui/container/shulker_container/sort_by_number"
    );
    public static final ResourceLocation SHULKER_CONTAINER_SORT_BY_MOD = texture(
        "gui/container/shulker_container/sort_by_mod"
    );
    public static final ResourceLocation SHULKER_CONTAINER_SORT_BY_NAME = texture(
        "gui/container/shulker_container/sort_by_name"
    );
    public static final ResourceLocation SHULKER_CONTAINER_SLIDER_BIG = texture(
        "gui/container/shulker_container/slider_big"
    );
    public static final ResourceLocation SHULKER_CONTAINER_SLIDER_SMALL = texture(
        "gui/container/shulker_container/slider_small"
    );
    public static final ResourceLocation SHULKER_CONTAINER_SHARE_OFF = texture(
        "gui/container/shulker_container/share_off"
    );
    public static final ResourceLocation SHULKER_CONTAINER_SHARE_ON = texture(
        "gui/container/shulker_container/share_on"
    );
    public static final ResourceLocation SHULKER_CONTAINER_UPGRADE = texture(
        "gui/container/shulker_container/upgrade"
    );
    public static final ResourceLocation SHULKER_CONTAINER_UPGRADE_BACK = texture(
        "gui/container/shulker_container/upgrade_back"
    );
    public static final ResourceLocation SHULKER_CONTAINER_UPGRADE_BG = texture(
        "gui/container/shulker_container/upgrade_background"
    );
    public static final ResourceLocation SHULKER_CONTAINER_UPGRADE_CONFIRM = texture(
        "gui/container/shulker_container/upgrade_confirm"
    );
    public static final ResourceLocation SHULKER_CONTAINER_UPGRADE_PROGRESS = texture(
        "gui/container/shulker_container/upgrade_progress"
    );
    public static final ResourceLocation SHULKER_CONTAINER_FOLDED_SLOT = texture(
        "gui/container/shulker_container/folded_slot"
    );

    private static ResourceLocation texture(String path) {
        return AnvilCraft.of("textures/" + path + ".png");
    }
}
