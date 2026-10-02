package dev.dubhe.anvilcraft.building;

import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 告示牌与悬挂告示牌不再随方块一次带字放下:书写、染色、发光、打蜡各拆成一条 DECORATE 操作,
 * 悦灵分别带上染料、荧光墨囊、蜂巢再上去加工,消耗与玩家手动加工一致。
 * 一条操作只能携带一种材料,所以正反面与四种加工必须各占一条,靠 {@code slot} 区分。
 */
public final class SignDecorationAdapter {
    /** 加工面向;顺序即 {@code slot} 编码,追加新项不会改变已持久化操作的含义。 */
    public enum Aspect {
        TEXT,
        COLOR,
        GLOW,
        WAX
    }

    private static final String FRONT_TEXT = "front_text";
    private static final String BACK_TEXT = "back_text";
    private static final String IS_WAXED = "is_waxed";
    private static final String MESSAGES = "messages";
    private static final String FILTERED_MESSAGES = "filtered_messages";
    private static final String COLOR = "color";
    private static final String HAS_GLOWING_TEXT = "has_glowing_text";

    private static final Aspect[] ASPECTS = Aspect.values();
    /** slot 0..7 的全掩码;{@link Aspect#WAX} 只用正面位,反面位留空不影响判断。 */
    public static final int MASK_ALL = (1 << (ASPECTS.length * 2)) - 1;

    private SignDecorationAdapter() {
    }

    /** 一条待加工项;{@code payload} 只存该项自己那点数据,其余字段交付时回落原版默认。 */
    public record Decoration(int slot, ItemStack material, @Nullable CompoundTag payload) {
    }

    /** 剥离后的方块实体配置,以及需要悦灵逐项加工的清单。 */
    public record Extracted(CompoundTag config, List<Decoration> decorations) {
    }

    public static boolean isSign(BlockState state) {
        return state.getBlock() instanceof SignBlock;
    }

    /** 低位存正反面,高位存面向。 */
    public static int slotOf(Aspect aspect, boolean front) {
        return aspect.ordinal() * 2 + (front ? 0 : 1);
    }

    @Nullable
    public static Aspect aspectOf(int slot) {
        if (slot < 0) return null;
        int index = slot / 2;
        return index >= ASPECTS.length ? null : ASPECTS[index];
    }

    public static boolean isFront(int slot) {
        return slot % 2 == 0;
    }

    /** 尚未加工项的位掩码;0 表示这块告示牌该显示什么就显示什么。 */
    public static int maskOf(int slot) {
        Aspect aspect = aspectOf(slot);
        return aspect == null ? 0 : 1 << slot;
    }

    /**
     * 抽出告示牌的文字、颜色、发光与蜡,并从配置里删掉,否则提交时载入配置会免费复原一遍。
     * 与 {@link BlockEntityContentAdapter} 抽走序列化槽物品是同一条约束。
     */
    public static Extracted extract(BlockState target, CompoundTag nbt, HolderLookup.Provider registries) {
        CompoundTag config = nbt.copy();
        if (!isSign(target)) {
            return new Extracted(config, List.of());
        }
        List<Decoration> decorations = new ArrayList<>();
        extractSide(config, FRONT_TEXT, true, registries, decorations);
        extractSide(config, BACK_TEXT, false, registries, decorations);
        if (config.getBooleanOr(IS_WAXED, false)) {
            decorations.add(new Decoration(
                slotOf(Aspect.WAX, true),
                new ItemStack(Items.HONEYCOMB),
                null
            ));
        }
        config.remove(IS_WAXED);
        return new Extracted(config, List.copyOf(decorations));
    }

    private static void extractSide(
        CompoundTag config,
        String key,
        boolean front,
        HolderLookup.Provider registries,
        List<Decoration> decorations
    ) {
        if (!(config.get(key) instanceof CompoundTag)) return;
        CompoundTag side = config.getCompoundOrEmpty(key);
        SignText text = SignText.DIRECT_CODEC
            .parse(registries.createSerializationContext(NbtOps.INSTANCE), side)
            .result()
            .orElse(null);
        if (text == null) throw new IllegalArgumentException("Invalid blueprint sign text");
        Tag messages = side.get(MESSAGES);
        config.remove(key);
        if (messages != null && hasText(text)) {
            decorations.add(new Decoration(
                slotOf(Aspect.TEXT, front),
                ItemStack.EMPTY,
                flattenMessages(text, registries)
            ));
        }
        if (text.getColor() != DyeColor.BLACK) {
            CompoundTag payload = new CompoundTag();
            payload.putString(COLOR, text.getColor().getSerializedName());
            decorations.add(new Decoration(
                slotOf(Aspect.COLOR, front),
                new ItemStack(BuiltInRegistries.ITEM.getValue(
                    Identifier.withDefaultNamespace(text.getColor().getSerializedName() + "_dye"))),
                payload
            ));
        }
        if (text.hasGlowingText()) {
            decorations.add(new Decoration(
                slotOf(Aspect.GLOW, front),
                new ItemStack(Items.GLOW_INK_SAC),
                null
            ));
        }
    }

    public static CompoundTag sanitize(BlockState state, CompoundTag nbt, HolderLookup.Provider registries) {
        CompoundTag blank = new CompoundTag();
        blank.put(FRONT_TEXT, blankSide(registries));
        blank.put(BACK_TEXT, blankSide(registries));
        blank.putBoolean(IS_WAXED, false);
        Extracted extracted = extract(state, nbt, registries);
        return compose(blank, extracted.decorations(), registries);
    }

    /** 提交时把配置与已交付的加工项合成最终 NBT;未交付项保持原版空白默认。 */
    public static CompoundTag compose(
        @Nullable CompoundTag config,
        List<Decoration> delivered,
        HolderLookup.Provider registries
    ) {
        CompoundTag result = config == null ? new CompoundTag() : config.copy();
        if (delivered.isEmpty()) return result;
        composeSide(result, FRONT_TEXT, true, delivered, registries);
        composeSide(result, BACK_TEXT, false, delivered, registries);
        for (Decoration decoration : delivered) {
            if (aspectOf(decoration.slot()) == Aspect.WAX) {
                result.putBoolean(IS_WAXED, true);
            }
        }
        return result;
    }

    private static void composeSide(
        CompoundTag result,
        String key,
        boolean front,
        List<Decoration> delivered,
        HolderLookup.Provider registries
    ) {
        CompoundTag text = null;
        CompoundTag color = null;
        boolean glowing = false;
        boolean any = false;
        for (Decoration decoration : delivered) {
            Aspect aspect = aspectOf(decoration.slot());
            if (aspect == null || aspect == Aspect.WAX) continue;
            if (isFront(decoration.slot()) != front) continue;
            if (aspect == Aspect.TEXT) {
                text = decoration.payload();
            } else if (aspect == Aspect.COLOR) {
                color = decoration.payload();
            } else {
                glowing = true;
            }
            any = true;
        }
        if (!any) return;
        CompoundTag side = blankSide(registries);
        // 加工项里只有压平后的 messages,不会有 filtered_messages,合成时也不补
        if (text != null) {
            Tag messages = text.get(MESSAGES);
            if (messages != null) {
                side.put(MESSAGES, messages.copy());
            }
        }
        if (color != null) {
            side.putString(COLOR, color.getStringOr(COLOR, ""));
        }
        if (glowing) {
            side.putBoolean(HAS_GLOWING_TEXT, true);
        }
        result.put(key, side);
    }

    /**
     * 客户端按未加工掩码裁剪蓝图快照 NBT:文字未写就换成空白四行,颜色与发光未做就删字段。
     * 掩码只标注真的排了加工操作而尚未交付的项,所以没有拆分计划的告示牌传 0 即原样渲染。
     */
    public static CompoundTag stripPending(CompoundTag full, int pending, HolderLookup.Provider registries) {
        if ((pending & MASK_ALL) == 0) return full;
        CompoundTag result = full.copy();
        stripPendingSide(result, FRONT_TEXT, true, pending, registries);
        stripPendingSide(result, BACK_TEXT, false, pending, registries);
        if (pending(pending, Aspect.WAX, true)) {
            result.remove(IS_WAXED);
        }
        return result;
    }

    private static void stripPendingSide(
        CompoundTag result,
        String key,
        boolean front,
        int pending,
        HolderLookup.Provider registries
    ) {
        if (!(result.get(key) instanceof CompoundTag)) return;
        CompoundTag side = result.getCompoundOrEmpty(key).copy();
        if (pending(pending, Aspect.TEXT, front)) {
            Tag blank = blankSide(registries).get(MESSAGES);
            if (blank == null) return;
            side.put(MESSAGES, blank);
            side.remove(FILTERED_MESSAGES);
        }
        if (pending(pending, Aspect.COLOR, front)) {
            side.remove(COLOR);
        }
        if (pending(pending, Aspect.GLOW, front)) {
            side.remove(HAS_GLOWING_TEXT);
        }
        result.put(key, side);
    }

    private static boolean pending(int pending, Aspect aspect, boolean front) {
        return (pending & maskOf(slotOf(aspect, front))) != 0;
    }

    private static boolean hasText(SignText text) {
        for (int line = 0; line < SignText.LINES; line++) {
            if (!text.getMessage(line, false).getString().isEmpty()) return true;
        }
        return false;
    }

    /**
     * 悦灵只会照着蓝图抄字面文本,不会连点击事件一起抄。
     * 蓝图来源不可信:带 {@code run_command} 的告示牌点一下就以命令方块权限执行,
     * 玩家可以就地建一块牌子把自己切成创造,所以每行只保留可见字符,样式与交互事件全部丢掉。
     */
    private static CompoundTag flattenMessages(SignText text, HolderLookup.Provider registries) {
        DynamicOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        SignText flattened = new SignText();
        for (int line = 0; line < SignText.LINES; line++) {
            flattened = flattened.setMessage(line, Component.literal(text.getMessage(line, false).getString()));
        }
        Tag encoded = SignText.DIRECT_CODEC.encodeStart(ops, flattened).result().orElse(null);
        CompoundTag payload = new CompoundTag();
        if (encoded instanceof CompoundTag compound) {
            Tag messages = compound.get(MESSAGES);
            if (messages != null) {
                payload.put(MESSAGES, messages);
            }
        }
        return payload;
    }

    /** 原版空白告示牌的一面,保证 {@code messages} 必填字段一定存在。 */
    private static CompoundTag blankSide(HolderLookup.Provider registries) {
        DynamicOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        Tag encoded = SignText.DIRECT_CODEC.encodeStart(ops, new SignText()).result().orElse(null);
        return encoded instanceof CompoundTag compound ? compound : new CompoundTag();
    }
}
