package dev.dubhe.anvilcraft.item;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.mixin.accessor.VillagerAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ExpGemItem extends Item {

    public ExpGemItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
        Level level,
        Player player,
        InteractionHand usedHand
    ) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        int count = player.isShiftKeyDown() ? itemStack.getCount() : 1;
        player.giveExperiencePoints(AnvilCraft.CONFIG.world.expFluidXpPerBlock * count);
        itemStack.consume(count, player);
        player.getCooldowns().addCooldown(this, 5);
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    /**
     * 右键实体
     */
    public static InteractionResult useEntity(Player player, Entity target, ItemStack stack) {
        if (!(target instanceof Villager villager)) return InteractionResult.PASS;
        if (villager.level().isClientSide()) return InteractionResult.PASS;
        if (villager.getAge() >= 0) {
            if (!canLevelUp(villager)) return InteractionResult.PASS;

            updateVillager(villager);
            stack.consume(1, player);
            return InteractionResult.SUCCESS;
        } else {
            villager.ageUp(AnvilCraft.CONFIG.equipment.expGemAgeAddition, true);
            stack.consume(1, player);
            return InteractionResult.SUCCESS;
        }
    }

    public static boolean canLevelUp(Villager villager) {
        VillagerData data = villager.getVillagerData();
        return villager.getAge() >= 0
            && data.getProfession() != VillagerProfession.NONE
            && VillagerData.canLevelUp(data.getLevel());
    }

    public static void updateVillager(Villager villager) {
        int villagerXp = villager.getVillagerXp() + AnvilCraft.CONFIG.equipment.expGemVillagerXp;
        villager.setVillagerXp(villagerXp);

        VillagerAccessor accessor = Util.cast(villager);
        if (accessor.invokeShouldIncreaseLevel()) {
            accessor.setUpdateMerchantTimer(40);
            accessor.setIncreaseProfessionLevelOnUpdate(true);
        }
    }
}
