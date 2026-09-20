package dev.dubhe.anvilcraft.api.amulet;

import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.api.amulet.def.IAmuletDefinition;
import dev.dubhe.anvilcraft.api.amulet.effect.IAmuletEffect;
import dev.dubhe.anvilcraft.api.event.AmuletEvent;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

@SuppressWarnings("DataFlowIssue")
public class AmuletManager {
    private static @Nullable SoftReference<AmuletManager> INSTANCE;

    public static AmuletManager get(HolderLookup.Provider registries) {
        if (AmuletManager.INSTANCE == null || AmuletManager.INSTANCE.get() == null) {
            AmuletManager.INSTANCE = new SoftReference<>(new AmuletManager(AmuletManager.extractDefinitions(registries)));
        }
        return AmuletManager.INSTANCE.get();
    }

    public static List<Holder.Reference<IAmuletDefinition>> extractDefinitions(HolderLookup.Provider registries) {
        return registries.lookupOrThrow(ModRegistryKeys.AMULET_DEF)
            .listElements()
            .toList();
    }

    public static void clear() {
        AmuletManager.INSTANCE = null;
    }

    private final List<Holder.Reference<IAmuletDefinition>> definitions;

    private AmuletManager(List<Holder.Reference<IAmuletDefinition>> definitions) {
        this.definitions = definitions;
    }

    public List<ItemStack> getAmuletsFromInventory(Player player) {
        List<ItemStack> founds = new ArrayList<>();
        NeoForge.EVENT_BUS.post(new AmuletEvent.Find(this, player, founds::add));
        List<ItemStack> amulets = new ArrayList<>();
        for (ItemStack found : founds) {
            this.processFoundStack(found, amulets);
        }
        return amulets;
    }

    private void processFoundStack(ItemStack found, List<ItemStack> amulets) {
        AmuletEvent.ProcessFound event = new AmuletEvent.ProcessFound(this, found);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return;
        }
        List<ItemStack> extracted = event.getExtracted();
        if (extracted.isEmpty()) {
            if (found.has(ModComponents.AMULET)) {
                amulets.add(found);
            }
            return;
        }
        for (ItemStack amulet : extracted) {
            if (!amulet.has(ModComponents.AMULET)) {
                continue;
            }
            amulets.add(amulet);
        }
    }

    /// 获取玩家身上所有护符展开后的效果，以及提供该效果的护符物品堆
    ///
    /// <p>包覆类护符展开后与被包覆护符共用同一批效果实例，这里按引用判等去重，
    /// 保证同时佩戴二者时同一效果只会触发一次。</p>
    ///
    /// @param player 佩戴护符的玩家
    /// @return 玩家身上所有护符展开后的效果
    private Map<IAmuletEffect, ItemStack> getActiveEffects(Player player) {
        Map<IAmuletEffect, ItemStack> effects = new LinkedHashMap<>();
        Set<IAmuletEffect> triggered = AmuletManager.identityView();
        for (ItemStack stack : this.getAmuletsFromInventory(player)) {
            Amulet amulet = this.getAmulet(stack);
            if (amulet == null) {
                continue;
            }
            for (IAmuletEffect effect : amulet.getFlattenEffects()) {
                if (triggered.add(effect)) {
                    effects.put(effect, stack);
                }
            }
        }
        return effects;
    }

    /// 以引用判等的视角看待一组护符效果
    ///
    /// @return 按引用判等的空效果集合
    private static Set<IAmuletEffect> identityView() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    /// 判断护符效果是否应在当前侧求值。
    ///
    /// <p>护符是代码注册的静态数据，效果判定全部由服务端负责，客户端只展示服务端同步的结果；
    /// 唯有需要参与客户端预测的重力计算（{@link #ignoresGravity}）与需要两侧同时拦截的交互
    /// （{@link #tryTame}）例外。</p>
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean shouldEvaluate(Player player) {
        return !player.level().isClientSide();
    }

    /// 触发玩家身上所有护符的效果
    ///
    /// @param player 佩戴护符的玩家
    /// @param ctx    本次触发的上下文
    private void trigger(Player player, AmuletEffectContext ctx) {
        this.getActiveEffects(player).forEach((effect, stack) -> effect.trigger(player, stack, ctx));
    }

    public void inventoryTick(ServerPlayer player) {
        Map<IAmuletEffect, ItemStack> active = this.getActiveEffects(player);
        Set<IAmuletEffect> triggered = AmuletManager.identityView();
        triggered.addAll(active.keySet());
        AmuletEffectContext enabled = new AmuletEffectContext();
        enabled.set(ModAmuletEffectContextKeys.ENABLED, true);
        active.forEach((effect, stack) -> effect.trigger(player, stack, enabled));
        AmuletEffectContext disabled = new AmuletEffectContext();
        disabled.set(ModAmuletEffectContextKeys.ENABLED, false);
        for (Amulet amulet : ModRegistries.AMULET) {
            Set<IAmuletEffect> effects = amulet.getFlattenEffects();
            if (effects.isEmpty() || triggered.containsAll(effects)) {
                continue;
            }
            // 这里只撤回该护符自身效果提供的状态，它包覆的护符会各自被判定到
            for (IAmuletEffect effect : amulet.getEffects()) {
                effect.trigger(player, ItemStack.EMPTY, disabled);
            }
        }
    }

    public boolean shouldImmune(ServerPlayer player, DamageSource source) {
        AmuletEffectContext ctx = new AmuletEffectContext();
        ctx.set(ModAmuletEffectContextKeys.DAMAGE_SOURCE, source);
        this.trigger(player, ctx);
        return ctx.get(ModAmuletEffectContextKeys.IMMUNE_DAMAGE).orElse(false);
    }

    /// 获取玩家的护符提供的交易折扣率
    ///
    /// <p>没有任何护符提供折扣时返回 0，表示不打折；{@link dev.dubhe.anvilcraft.api.amulet.effect.DiscountAmuletEffect}
    /// 里的缺省值 1 则是「已有折扣率」的基准，两者含义不同，不要互换。</p>
    ///
    /// @param player 佩戴护符的玩家
    /// @return 玩家的交易折扣率
    public float getDiscountRate(Player player) {
        if (!AmuletManager.shouldEvaluate(player)) {
            return 0F;
        }
        AmuletEffectContext ctx = new AmuletEffectContext();
        this.trigger(player, ctx);
        return ctx.get(ModAmuletEffectContextKeys.DISCOUNT_RATE).orElse(0F);
    }

    /// 判断玩家是否免疫给定药水效果
    ///
    /// @param player        佩戴护符的玩家
    /// @param effect        待判定的药水效果
    /// @param consumingFood 玩家是否处于进食中
    /// @return 玩家是否免疫给定药水效果
    public boolean isImmuneToMobEffect(Player player, MobEffectInstance effect, boolean consumingFood) {
        if (!AmuletManager.shouldEvaluate(player)) {
            return false;
        }
        AmuletEffectContext ctx = new AmuletEffectContext();
        ctx.set(ModAmuletEffectContextKeys.MOB_EFFECT, effect);
        ctx.set(ModAmuletEffectContextKeys.CONSUMING_FOOD, consumingFood);
        this.trigger(player, ctx);
        return ctx.get(ModAmuletEffectContextKeys.IMMUNE_MOB_EFFECT).orElse(false);
    }

    /// 判断给定生物是否无视玩家
    ///
    /// @param player 佩戴护符的玩家
    /// @param mob    待判定的生物
    /// @return 给定生物是否无视玩家
    public boolean shouldIgnoreTarget(Player player, LivingEntity mob) {
        return this.shouldIgnoreTarget(player, mob.getType());
    }

    /// 判断给定类型的生物是否无视玩家
    ///
    /// @param player  佩戴护符的玩家
    /// @param mobType 待判定的生物类型
    /// @return 给定类型的生物是否无视玩家
    public boolean shouldIgnoreTarget(Player player, EntityType<?> mobType) {
        if (!AmuletManager.shouldEvaluate(player)) {
            return false;
        }
        AmuletEffectContext ctx = new AmuletEffectContext();
        ctx.set(ModAmuletEffectContextKeys.MOB_TYPE, mobType);
        this.trigger(player, ctx);
        return ctx.get(ModAmuletEffectContextKeys.IGNORE_MOB).orElse(false);
    }

    public boolean isImmuneToKnockback(Player player) {
        if (!AmuletManager.shouldEvaluate(player)) {
            return false;
        }
        AmuletEffectContext ctx = new AmuletEffectContext();
        this.trigger(player, ctx);
        return ctx.get(ModAmuletEffectContextKeys.IMMUNE_KNOCKBACK).orElse(false);
    }

    public boolean isImmuneToVibration(Player player) {
        if (!AmuletManager.shouldEvaluate(player)) {
            return false;
        }
        AmuletEffectContext ctx = new AmuletEffectContext();
        this.trigger(player, ctx);
        return ctx.get(ModAmuletEffectContextKeys.IMMUNE_VIBRATION).orElse(false);
    }

    public boolean ignoresGravity(Player player) {
        AmuletEffectContext ctx = new AmuletEffectContext();
        this.trigger(player, ctx);
        return ctx.get(ModAmuletEffectContextKeys.IGNORE_GRAVITY).orElse(false);
    }

    public boolean isImmuneToAbnormalItems(Player player) {
        AmuletEffectContext ctx = new AmuletEffectContext();
        this.trigger(player, ctx);
        return ctx.get(ModAmuletEffectContextKeys.IMMUNE_ABNORMAL_ITEM).orElse(false);
    }

    /// 尝试驯服给定动物
    ///
    /// @param player 佩戴护符的玩家
    /// @param animal 待驯服的动物
    /// @return 是否有护符处理了本次交互
    public boolean tryTame(Player player, TamableAnimal animal) {
        AmuletEffectContext ctx = new AmuletEffectContext();
        ctx.set(ModAmuletEffectContextKeys.INTERACT_TARGET, animal);
        this.trigger(player, ctx);
        return ctx.get(ModAmuletEffectContextKeys.HANDLE_INTERACT).orElse(false);
    }

    public void tryRaffle(ServerPlayer player, DamageSource source) {
        Holder.Reference<IAmuletDefinition> trying = null;
        ItemStack amulet = null;

        RandomSource random = player.getRandom();
        List<Holder.Reference<IAmuletDefinition>> defs = this.getDefinitionMatchedDamage(player, source);
        if (defs.isEmpty()) {
            return;
        }

        List<Holder.Reference<IAmuletDefinition>> shuffled = new ArrayList<>(defs);
        shuffled.sort(Comparator.comparingInt(ignored -> random.nextInt()));
        for (Holder.Reference<IAmuletDefinition> def : shuffled) {
            amulet = def.value().create();
            ResourceKey<Amulet> key = amulet.get(ModComponents.AMULET);
            if (key == null || !this.isAmuletActive(player, key)) {
                trying = def;
                break;
            }
        }

        if (trying == null) {
            return;
        }

        int probability = Math.min(this.getRaffleProbability(player, trying), 100);
        if (random.nextInt(100) < probability) {
            player.getInventory().placeItemBackInInventory(amulet.copy());
            this.setRaffleProbability(player, trying, 0);
        } else {
            probability = NeoForge.EVENT_BUS.post(new AmuletEvent.ModifyRaffleProbability(
                this,
                player,
                source,
                trying,
                probability + 10
            )).getProbability();
            this.setRaffleProbability(player, trying, Math.clamp(probability, 0, 100));
        }
    }

    public List<Holder.Reference<IAmuletDefinition>> getDefinitionMatchedDamage(ServerPlayer victim, DamageSource source) {
        List<Holder.Reference<IAmuletDefinition>> results = new ArrayList<>();
        for (Holder.Reference<IAmuletDefinition> def : this.definitions) {
            if (def.value().mayObtain(victim, source)) {
                results.add(def);
            }
        }
        return results;
    }

    public int getRaffleProbability(Player player, Holder<IAmuletDefinition> def) {
        if (this.isAmuletActive(player, def)) {
            return 0;
        }
        return AmuletManager.getStoredRaffleProbability(player, def);
    }

    public static int getStoredRaffleProbability(Player player, Holder<IAmuletDefinition> def) {
        return player.getData(ModDataAttachments.AMULET_RAFFLE_PROBABILITY).getProbability(def);
    }

    /// 判断能充当给定护符的护符是否已在玩家身上生效
    ///
    /// @param player 佩戴护符的玩家
    /// @param amulet 给定护符的资源键
    /// @return 能充当给定护符的护符是否已在玩家身上生效
    public boolean isAmuletActive(Player player, ResourceKey<Amulet> amulet) {
        Amulet target = ModRegistries.AMULET.get(amulet);
        if (target == null) {
            return false;
        }
        Set<IAmuletEffect> effects = target.getFlattenEffects();
        if (effects.isEmpty()) {
            return false;
        }
        Set<IAmuletEffect> triggered = AmuletManager.identityView();
        triggered.addAll(this.getActiveEffects(player).keySet());
        return triggered.containsAll(effects);
    }

    /// 判断给定护符定义对应的护符是否已佩戴在玩家身上
    ///
    /// @param player 佩戴护符的玩家
    /// @param def    给定护符的定义
    /// @return 给定护符定义对应的护符是否已佩戴在玩家身上
    public boolean isAmuletActive(Player player, Holder<IAmuletDefinition> def) {
        ItemStack target = def.value().create();
        List<ItemStack> amulets = this.getAmuletsFromInventory(player);
        return amulets.stream().anyMatch(stack -> ItemStack.isSameItem(stack, target));
    }

    public void setRaffleProbability(ServerPlayer player, Holder<IAmuletDefinition> def, int probability) {
        AmuletRaffleProbability arp = player.getData(ModDataAttachments.AMULET_RAFFLE_PROBABILITY);
        if (!this.isAmuletActive(player, def)) {
            arp.setProbability(def, probability);
        } else {
            arp.setProbability(def, 0);
        }
    }

    /// 获取给定物品堆上的护符
    ///
    /// @param stack 给定的护符物品堆
    /// @return 给定物品堆上的护符，若物品堆没有护符则为 `null`
    public @Nullable Amulet getAmulet(ItemStack stack) {
        ResourceKey<Amulet> key = stack.get(ModComponents.AMULET);
        return key == null ? null : ModRegistries.AMULET.get(key);
    }
}
