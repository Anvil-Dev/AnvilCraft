package dev.dubhe.anvilcraft.event.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CreativeGeneratorRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = AnvilCraft.MOD_ID)
public class RegisterAdditionalEventListener {

    /**
     * 注册模型
     */
    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(new ModelResourceLocation("anvilcraft", "crab_claw_holding_block", "inventory"));
        event.register(new ModelResourceLocation("anvilcraft", "crab_claw_holding_item", "inventory"));
        event.register(new ModelResourceLocation("anvilcraft", "heliostats_head", ""));
        event.register(new ModelResourceLocation("anvilcraft", "creative_generator_cube", ""));
    }
}
