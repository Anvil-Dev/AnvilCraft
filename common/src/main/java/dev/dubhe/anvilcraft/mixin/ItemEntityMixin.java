package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
abstract class ItemEntityMixin extends Entity {
    public ItemEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 slowDown(Vec3 instance, double dx, double dy, double dz) {
        ItemEntity ths = (ItemEntity) (Object) this;
        if (ths.level().getBlockState(ths.blockPosition()).is(ModBlocks.HOLLOW_MAGNET_BLOCK.get()))
            dy *= 0.2;
        return instance.add(dx, dy, dz);
    }

    private boolean needMagnetization = true;
    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void magnetization(CallbackInfo ci){
        if (this.getServer() == null) return;
        ItemEntity ths = (ItemEntity) (Object) this;
        if (!ths.level().getBlockState(ths.blockPosition()).is(ModBlocks.HOLLOW_MAGNET_BLOCK.get())) return;
        if (ths.getOwner() == null || !(ths.getOwner() instanceof ServerPlayer)) return;
        ItemStack itemStack = ths.getItem();
        if (!itemStack.is(Items.IRON_INGOT)) return;
        if (itemStack.getCount() != 1) return;
        if (!this.needMagnetization) return;
        this.needMagnetization = false;
        int random = (int) (Math.random() * 20);
        if (random == 0){
            ths.setItem(new ItemStack(ModItems.MAGNET_INGOT));
        }
    }
}
