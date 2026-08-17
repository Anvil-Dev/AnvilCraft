package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import dev.dubhe.anvilcraft.saved.setting.mode.BalanceMode;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 物品均衡模式的服务端处理：
 * <ul>
 *   <li>补货：主/副手物品使用耗尽时，从绑定存储站取出同种物品补充满一组。</li>
 *   <li>存入：背包中某物品超过一组时，只保留一组在身上，多余部分自动存入绑定存储站。</li>
 * </ul>
 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class PlayerBalanceHandler {
    /** 上次记录的玩家手持物品，用于在物品用尽时得知其种类。键为主/副手槽位索引。 */
    private static final Map<UUID, ItemStack[]> PREV_HANDS = new HashMap<>();
    /** 上次记录的主手选中快捷槽位索引（用于区分“物品用完”与“切换快捷栏”）。 */
    private static final Map<UUID, Integer> PREV_SELECTED = new HashMap<>();
    /** 玩家自上次存入扫描以来的 tick 计数，用于节流避免每 tick 全量扫描。 */
    private static final Map<UUID, Integer> DEPOSIT_COOLDOWN = new HashMap<>();
    /** 玩家本 tick 是否使用过主手/副手物品（用于区分“主动使用耗尽”与“物品被移走”）。 */
    private static final Map<UUID, Boolean> USED_THIS_TICK = new HashMap<>();
    /** 存入扫描间隔（tick），约 1 秒扫描一次足够响应捡取/被动获得。 */
    private static final int DEPOSIT_INTERVAL = 20;

    /** 玩家退出时清理其所有追踪状态，避免静态 Map 永久残留。 */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            PlayerBalanceHandler.PREV_HANDS.remove(uuid);
            PlayerBalanceHandler.PREV_SELECTED.remove(uuid);
            PlayerBalanceHandler.DEPOSIT_COOLDOWN.remove(uuid);
            PlayerBalanceHandler.USED_THIS_TICK.remove(uuid);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        BalanceMode mode = PlayerSettings
            .getSetting(serverPlayer.registryAccess(), serverPlayer.getGameProfile().getId())
            .balanceMode();
        if (mode == BalanceMode.OFF) {
            PlayerBalanceHandler.PREV_HANDS.remove(serverPlayer.getUUID());
            PlayerBalanceHandler.PREV_SELECTED.remove(serverPlayer.getUUID());
            PlayerBalanceHandler.DEPOSIT_COOLDOWN.remove(serverPlayer.getUUID());
            PlayerBalanceHandler.USED_THIS_TICK.remove(serverPlayer.getUUID());
            return;
        }
        if (mode.depositEnabled()) {
            int cd = PlayerBalanceHandler.DEPOSIT_COOLDOWN.merge(serverPlayer.getUUID(), 1, Integer::sum);
            if (cd >= PlayerBalanceHandler.DEPOSIT_INTERVAL) {
                PlayerBalanceHandler.DEPOSIT_COOLDOWN.put(serverPlayer.getUUID(), 0);
                StorageServerStub.depositExcess(serverPlayer);
            }
        } else {
            PlayerBalanceHandler.DEPOSIT_COOLDOWN.remove(serverPlayer.getUUID());
        }
        if (mode.restockEnabled()) {
            PlayerBalanceHandler.trackAndRestock(serverPlayer);
        }
        // 使用标记仅在当 tick 有效
        PlayerBalanceHandler.USED_THIS_TICK.remove(serverPlayer.getUUID());
    }

    /** 玩家右键使用物品（食物、药水、弓、放置等）时标记，用于补货判定。 */
    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerBalanceHandler.USED_THIS_TICK.put(player.getUUID(), true);
        }
    }

    /** 玩家左键攻击/挖掘（消耗耐久或投掷）时标记，用于补货判定。 */
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerBalanceHandler.USED_THIS_TICK.put(player.getUUID(), true);
        }
    }

    /** 玩家左键点击方块/实体（挖掘、攻击）时标记，用于补货判定。 */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerBalanceHandler.USED_THIS_TICK.put(player.getUUID(), true);
        }
    }

    private static void trackAndRestock(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Inventory inventory = player.getInventory();
        int selected = inventory.selected;
        Integer prevSelected = PlayerBalanceHandler.PREV_SELECTED.get(uuid);
        boolean sameSlot = prevSelected == null || prevSelected == selected;
        boolean used = PlayerBalanceHandler.USED_THIS_TICK.getOrDefault(uuid, false);

        ItemStack[] prev = PlayerBalanceHandler.PREV_HANDS.get(uuid);
        ItemStack[] current = {
            player.getMainHandItem().copy(),
            player.getOffhandItem().copy()
        };
        if (prev != null) {
            // 主手：仅当本 tick 主动使用过物品、仍停留在同一快捷槽位且该槽物品用尽时才补货，
            // 切换快捷槽位或物品被容器/JEI 移走不算主动使用，不触发补货。
            PlayerBalanceHandler.tryRestockSlot(player, 0, prev[0], player.getMainHandItem(), sameSlot, used);
            // 副手：槽位固定(40)，同样需要主动使用标记。
            PlayerBalanceHandler.tryRestockSlot(player, 1, prev[1], player.getOffhandItem(), true, used);
        }
        PlayerBalanceHandler.PREV_HANDS.put(uuid, current);
        PlayerBalanceHandler.PREV_SELECTED.put(uuid, selected);
    }

    private static void tryRestockSlot(ServerPlayer player, int handIndex, ItemStack prev, ItemStack now, boolean sameSlot, boolean used) {
        // 物品用尽：本 tick 主动使用过、仍停留在同一槽位、上一 tick 仍有该物品、现在该槽位为空，
        // 且鼠标指针上没有物品（捏起物品到指针不算用完，不补货）
        if (used && sameSlot && now.isEmpty() && !prev.isEmpty() && player.containerMenu.getCarried().isEmpty()) {
            int inventorySlot = handIndex == 0 ? player.getInventory().selected : 40;
            StorageServerStub.restockHand(player, prev, inventorySlot);
        }
    }
}
