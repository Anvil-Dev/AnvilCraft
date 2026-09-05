package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.worldgen.TheMonolith;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

/** 石碑事件：首次登月生成石碑；石碑范围内的书汇聚附魔粒子，最终转化为铁砧工艺手册。 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class TheMonolithEventListener {
    /** 书转化为手册所需的持续时长（tick）。 */
    private static final int TRANSFORM_TICKS = 320;
    /**
     * 每本书的转化进度。以实体为键的弱引用表，书被拾取后实体消失、进度随 GC 清除，
     * 物品本身不带任何附加数据，拾取后可与普通书正常堆叠。
     */
    private static final Map<Entity, Integer> PROGRESS = new WeakHashMap<>();

    /** 玩家进入 Mun 时，在落点附近生成全局唯一的石碑。 */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getTo() != CelestialTravelManager.MUN_LEVEL) return;
        if (!(event.getEntity().getServer() instanceof MinecraftServer server)) return;
        ServerLevel mun = server.getLevel(CelestialTravelManager.MUN_LEVEL);
        if (mun == null) return;
        TheMonolith.ensureGenerated(mun);
    }

    /** 玩家登录时若已身处 Mun（跨版本升级的存档），同样补生成石碑。 */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        if (level.dimension() != CelestialTravelManager.MUN_LEVEL) return;
        TheMonolith.ensureGenerated(level);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (level.dimension() != CelestialTravelManager.MUN_LEVEL) return;
        boolean isBook = false;
        if (entity instanceof ItemEntity itemEntity) {
            isBook = itemEntity.getItem().is(Items.BOOK);
        } else if (entity instanceof ItemFrame itemFrame) {
            // ItemFrame 同时覆盖发光物品展示框
            isBook = itemFrame.getItem().is(Items.BOOK);
        }
        if (!isBook) return;
        TheMonolith.State state = TheMonolith.State.get(level);
        if (!state.isInRange(entity.blockPosition())) {
            PROGRESS.remove(entity);
            return;
        }
        int progress = PROGRESS.merge(entity, 1, Integer::sum);
        spawnConvergingParticles(level, entity);
        if (progress < TRANSFORM_TICKS) return;
        PROGRESS.remove(entity);
        ItemStack manual = new ItemStack(ModItems.GUIDE_BOOK.get());
        if (entity instanceof ItemEntity itemEntity) {
            itemEntity.setItem(manual);
        } else if (entity instanceof ItemFrame itemFrame) {
            itemFrame.setItem(manual, false);
        }
        level.playSound(
            null, entity.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F
        );
    }

    /** 实体离开世界时清除其转化进度（如书被拾取、展示框被破坏）。 */
    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        PROGRESS.remove(event.getEntity());
    }

    /** 在书的周围生成附魔文字粒子并汇聚到书上。 */
    private static void spawnConvergingParticles(ServerLevel level, Entity book) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < 2; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 2.5 + random.nextDouble() * 2.5;
            // ENCHANT 粒子从“生成位置 + 速度向量”处出发、飞回生成位置（与附魔台粒子一致），
            // 因此生成位置取书本位置、向量指向外围起点
            level.sendParticles(
                ParticleTypes.ENCHANT,
                book.getX(), book.getY() + 0.5, book.getZ(),
                0,
                Math.cos(angle) * distance, random.nextDouble() * 2.0 - 0.5, Math.sin(angle) * distance,
                1.0
            );
        }
    }
}
