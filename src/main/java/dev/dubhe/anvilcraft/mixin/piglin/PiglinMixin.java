package dev.dubhe.anvilcraft.mixin.piglin;

import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.item.abnormal.ICursed;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Piglin.class)
public abstract class PiglinMixin extends AbstractPiglin {

    @Unique
    private static final int MIN_CURSED_ZOMBIFICATION_TIME = 60;
    @Unique
    private static final int MAX_CURSED_ZOMBIFICATION_TIME = 100;

    private PiglinMixin(EntityType<? extends AbstractPiglin> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(
        method = "holdInOffHand",
        at = @At(value = "HEAD")
    )
    private void startCursedZombification(ItemStack stack, CallbackInfo ci) {
        if (!(stack.getItem() instanceof ICursed)) return;
        this.timeInOverworld = CONVERSION_TIME - this.level().getRandom().nextIntBetweenInclusive(
            MIN_CURSED_ZOMBIFICATION_TIME, MAX_CURSED_ZOMBIFICATION_TIME);
        this.setData(ModDataAttachments.ZOMBIFICATED_BY_CURSE, true);
    }

    @Override
    public boolean isConverting() {
        if (this.getData(ModDataAttachments.ZOMBIFICATED_BY_CURSE)) {
            return true;
        }
        return super.isConverting();
    }
}
