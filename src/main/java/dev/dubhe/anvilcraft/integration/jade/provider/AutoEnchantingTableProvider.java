package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public enum AutoEnchantingTableProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof AutoEnchantingTableBlockEntity blockEntity)) return;
        int cooldownTicks = blockEntity.getCooldownTicks();
        int totalTicks = AnvilCraft.CONFIG.autoEnchantingTableInterval;
        // 空闲时冷却保持在满值，写入 0 让客户端隐藏进度条（与充电器一致）
        if (cooldownTicks <= 0 || cooldownTicks >= totalTicks) {
            tag.putInt("auto_enchanting_table_cooldown_ticks", 0);
            tag.putInt("auto_enchanting_table_total_ticks", 0);
        } else {
            tag.putInt("auto_enchanting_table_cooldown_ticks", cooldownTicks);
            tag.putInt("auto_enchanting_table_total_ticks", totalTicks);
        }
    }

    @Override
    public Identifier getUid() {
        return AnvilCraft.of("auto_enchanting_table_provider");
    }
}
