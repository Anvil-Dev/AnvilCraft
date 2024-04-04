package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(BeaconMenu.class)
public class BeaconMenuMixin {
    @Shadow
    @Final
    private BeaconMenu.PaymentSlot paymentSlot;

    @Shadow
    @Final
    private ContainerLevelAccess access;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject(method = "updateEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/BeaconMenu$PaymentSlot;remove(I)Lnet/minecraft/world/item/ItemStack;"))
    private void updateEffects(Optional<MobEffect> primaryEffect, Optional<MobEffect> secondaryEffect, CallbackInfo ci) {
        ItemStack item = this.paymentSlot.getItem();
        if (!item.is(ModItems.CURSED_GOLD_INGOT.get())) return;
        this.access.execute((level, pos) -> {
            if (!(level instanceof ServerLevel serverLevel)) return;
            MinecraftServer server = serverLevel.getServer();
            GameRules.BooleanValue rule = server.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE);
            if (!rule.get()) return;
            serverLevel.setWeatherParameters(0, ServerLevel.THUNDER_DURATION.sample(serverLevel.getRandom()), true, true);
        });
    }
}
