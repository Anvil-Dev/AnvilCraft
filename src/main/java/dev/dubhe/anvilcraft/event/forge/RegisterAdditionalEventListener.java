package dev.dubhe.anvilcraft.event.forge;

import dev.anvilcraft.lib.event.SubscribeEvent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CreativeGeneratorRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD, modid = AnvilCraft.MOD_ID)
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
