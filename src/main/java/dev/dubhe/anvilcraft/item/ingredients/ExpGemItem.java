package dev.dubhe.anvilcraft.item.ingredients;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.block.fluid.ExpFluidBlock;
import dev.dubhe.anvilcraft.mixin.accessor.VillagerAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ExpGemItem extends Item {
    public static final int VILLAGER_XP = 20;
    public static final int AGE_ADDITION = 2 * 60;

    public ExpGemItem(Properties properties) {
        super(properties.useCooldown(0.25F));
    }

    @Override
    public InteractionResult use(
        Level level,
        Player player,
        InteractionHand usedHand
    ) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        int count = player.isShiftKeyDown() ? itemStack.getCount() : 1;
        player.giveExperiencePoints(ExpFluidBlock.XP_POINTS * count);
        itemStack.consume(count, player);
        return dev.dubhe.anvilcraft.util.Util.sidedSuccess(level);
    }

    /**
     * 右键实体
     */
    public static InteractionResult useEntity(Player player, Entity target, ItemStack stack) {
        if (!(target instanceof Villager villager)) return InteractionResult.PASS;
        if (villager.level().isClientSide()) return InteractionResult.PASS;
        if (villager.getAge() >= 0) {
            // 只对有职业且不满级的村民生效
            VillagerData villagerData = villager.getVillagerData();
            int villagerLevel = villagerData.level();
            if (villagerData.profession() == VillagerProfession.NONE) return InteractionResult.PASS;
            if (!VillagerData.canLevelUp(villagerLevel)) return InteractionResult.PASS;

            updateVillager(villager);
            stack.consume(1, player);
            return InteractionResult.SUCCESS;
        } else {
            villager.ageUp(AGE_ADDITION, true);
            stack.consume(1, player);
            return InteractionResult.SUCCESS;
        }
    }

    public static void updateVillager(Villager villager) {
        int villagerXp = villager.getVillagerXp() + VILLAGER_XP;
        villager.setVillagerXp(villagerXp);

        VillagerAccessor accessor = Util.cast(villager);
        if (accessor.invokeShouldIncreaseLevel()) {
            accessor.setUpdateMerchantTimer(40);
            accessor.setIncreaseProfessionLevelOnUpdate(true);
        }
    }
}
