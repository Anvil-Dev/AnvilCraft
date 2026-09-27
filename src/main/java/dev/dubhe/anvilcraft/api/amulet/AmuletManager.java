package dev.dubhe.anvilcraft.api.amulet;

import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.api.amulet.def.IAmuletDefinition;
import dev.dubhe.anvilcraft.api.amulet.effect.IAmuletEffect;
import dev.dubhe.anvilcraft.api.event.AmuletEvent;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.lang.ref.SoftReference;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.annotation.Nullable;

public class AmuletManager {
    private static @Nullable SoftReference<AmuletManager> INSTANCE;

    public static AmuletManager get(HolderLookup.Provider registries) {
        if (AmuletManager.INSTANCE == null || AmuletManager.INSTANCE.get() == null) {
            AmuletManager.INSTANCE = new SoftReference<>(new AmuletManager(AmuletManager.extractDefinitions(registries)));
        }
        return Objects.requireNonNull(AmuletManager.INSTANCE.get());
    }

    public static List<Holder.Reference<IAmuletDefinition>> extractDefinitions(HolderLookup.Provider registries) {
        return registries.lookupOrThrow(ModRegistryKeys.AMULET_DEF)
            .listElements()
            .toList();
    }

    public static void clear() {
        AmuletManager.INSTANCE = null;
    }

    public void clear(UUID id) {
        for (Map<UUID, CacheEntry> value : this.cache.values()) {
            value.remove(id);
        }
    }

    private final WeakHashMap<RegistryAccess, Map<UUID, CacheEntry>> cache = new WeakHashMap<>();
    private final List<Holder.Reference<IAmuletDefinition>> definitions;

    private AmuletManager(List<Holder.Reference<IAmuletDefinition>> definitions) {
        this.definitions = definitions;
    }

    public List<ItemStack> findAmulets(LivingEntity entity) {
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

    /// 获取实体身上所有护符展开后的效果，以及提供该效果的护符物品堆
    ///
    /// <p>包覆类护符展开后与被包覆护符共用同一批效果实例，这里按引用判等去重，
    /// 保证同时佩戴二者时同一效果只会触发一次。</p>
    ///
    /// @param entity 佩戴护符的实体
    /// @return 实体身上所有护符展开后的效果
    public Map<IAmuletEffect, ItemStack> getActiveEffects(LivingEntity entity) {
        List<ItemStack> amulets = this.findAmulets(entity);
        UUID id = entity.getUUID();
        RegistryAccess registries = entity.registryAccess();
        Map<UUID, CacheEntry> cache = this.cache.computeIfAbsent(registries, ignore -> new HashMap<>());
        CacheEntry entry = cache.get(id);
        if (entry != null && entry.isCacheHit(amulets, registries)) {
            return entry.effects();
        }

        Map<IAmuletEffect, ItemStack> effects = new LinkedHashMap<>();
        Set<IAmuletEffect> triggered = AmuletManager.identityView();
        for (ItemStack stack : amulets) {
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
        cache.put(id, new CacheEntry(amulets, registries, effects));
        return effects;
    }

    /// 以引用判等的视角看待一组护符效果
    ///
    /// @return 按引用判等的空效果集合
    public static Set<IAmuletEffect> identityView() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    /// 判断护符效果是否应在当前侧求值。
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean shouldEvaluate(LivingEntity entity) {
        return !entity.level().isClientSide();
    }

    /// 触发实体身上所有护符的效果
    ///
    /// @param entity 佩戴护符的实体
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

    /// 判断能充当给定护符的护符是否已在实体身上生效
    ///
    /// @param entity 佩戴护符的实体
    /// @param amulet 给定护符的资源键
    /// @return 能充当给定护符的护符是否已在实体身上生效
    private boolean isAmuletActive(LivingEntity entity, ResourceKey<Amulet> amulet) {
        Amulet target = ModRegistries.AMULET.get(amulet);
        if (target == null) {
            return false;
        }
        Set<IAmuletEffect> effects = target.getFlattenEffects();
        if (effects.isEmpty()) {
            return false;
        }
        Set<IAmuletEffect> triggered = AmuletManager.identityView();
        triggered.addAll(this.getActiveEffects(entity).keySet());
        return triggered.containsAll(effects);
    }

    /// 判断给定护符定义对应的护符是否已佩戴在实体身上
    ///
    /// @param entity 佩戴护符的实体
    /// @param def    给定护符的定义
    /// @return 给定护符定义对应的护符是否已佩戴在实体身上
    private boolean isAmuletActive(LivingEntity entity, Holder<IAmuletDefinition> def) {
        ItemStack target = def.value().create();
        List<ItemStack> amulets = this.findAmulets(entity);
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

    private record CacheEntry(int size, byte[] sha256, Map<IAmuletEffect, ItemStack> effects) {
        public CacheEntry(List<ItemStack> stacks, RegistryAccess registries, Map<IAmuletEffect, ItemStack> effects) {
            this(stacks.size(), CacheEntry.sha256StackList(stacks, registries), effects);
        }

        public boolean isCacheHit(List<ItemStack> stacks, RegistryAccess registries) {
            return this.size == stacks.size()
                   && this.sha256 == CacheEntry.sha256StackList(stacks, registries);
        }

        @SneakyThrows
        private static byte[] sha256StackList(List<ItemStack> list, RegistryAccess registries) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            CacheEntry.updateInt(md, list.size());

            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries, ConnectionType.NEOFORGE);
            try {
                for (ItemStack stack : list) {
                    buf.clear();

                    ItemStack.STREAM_CODEC.encode(buf, stack);
                    byte[] encoded = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), false);

                    CacheEntry.updateInt(md, encoded.length);
                    md.update(encoded);
                }
            } finally {
                buf.release();
            }

            return md.digest();
        }

        private static void updateInt(MessageDigest md, int v) {
            md.update((byte) (v >>> 24));
            md.update((byte) (v >>> 16));
            md.update((byte) (v >>> 8));
            md.update((byte) v);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CacheEntry that)) return false;
            return this.size() == that.size()
                   && this.sha256() == that.sha256();
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.size(), Arrays.hashCode(this.sha256()));
        }
    }
}
