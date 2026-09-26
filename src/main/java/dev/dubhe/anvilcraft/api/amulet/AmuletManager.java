package dev.dubhe.anvilcraft.api.amulet;

import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.api.amulet.def.IAmuletDefinition;
import dev.dubhe.anvilcraft.api.amulet.effect.IAmuletEffect;
import dev.dubhe.anvilcraft.api.event.AmuletEvent;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
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

    public List<ItemStack> getAmuletsFromInventory(LivingEntity entity) {
        List<ItemStack> founds = new ArrayList<>();
        NeoForge.EVENT_BUS.post(new AmuletEvent.Find(this, entity, founds::add));
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
    /// @param entity 佩戴护符的玩家
    /// @return 玩家身上所有护符展开后的效果
    public Map<IAmuletEffect, ItemStack> getActiveEffects(LivingEntity entity) {
        Map<IAmuletEffect, ItemStack> effects = new LinkedHashMap<>();
        Set<IAmuletEffect> triggered = AmuletManager.identityView();
        for (ItemStack stack : this.getAmuletsFromInventory(entity)) {
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
    public static Set<IAmuletEffect> identityView() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    /// 判断护符效果是否应在当前侧求值。
    ///
    /// <p>护符是代码注册的静态数据，效果判定全部由服务端负责，客户端只展示服务端同步的结果；
    /// 唯有需要参与客户端预测的重力计算与需要两侧同时拦截的交互例外。</p>
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean shouldEvaluate(LivingEntity entity) {
        return !entity.level().isClientSide();
    }

    /// 触发玩家身上所有护符的效果
    ///
    /// @param entity 佩戴护符的玩家
    /// @param ctx    本次触发的上下文
    public void trigger(LivingEntity entity, AmuletEffectContext ctx) {
        this.getActiveEffects(entity).forEach((effect, stack) -> effect.trigger(entity, stack, ctx));
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
