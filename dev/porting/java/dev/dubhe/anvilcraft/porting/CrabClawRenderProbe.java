package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.item.CrabClawItemInHandRenderer;
import dev.dubhe.anvilcraft.client.renderer.item.ItemInHandRendererManager;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class CrabClawRenderProbe {
    private static @Nullable ItemInHandRendererManager manager;

    public static void verify(Minecraft client) {
        if (manager == null) {
            manager = new ItemInHandRendererManager(client.getItemModelResolver(), (entity, stack, context, pose, collector, light) -> {
                throw new IllegalStateException("Unexpected empty-hand fallback");
            });
        }
        var player = client.player;
        var originalArm = player.getMainArm();
        var main = player.getMainHandItem();
        var off = player.getOffhandItem();
        var carried = player.getInventory().getItem(9);
        var cases = List.of(
            new Case(new ItemStack(Items.RED_BED), CrabClawItemInHandRenderer.HOLDING_BLOCK, false, false),
            new Case(new ItemStack(Items.STONE), CrabClawItemInHandRenderer.HOLDING_BLOCK, false, false),
            new Case(new ItemStack(Items.STONE_SLAB), CrabClawItemInHandRenderer.HOLDING_BLOCK_SLAB, false, false),
            new Case(new ItemStack(Items.STONE_PRESSURE_PLATE), CrabClawItemInHandRenderer.HOLDING_BLOCK_PANEL, true, false),
            new Case(new ItemStack(Items.OAK_FENCE), CrabClawItemInHandRenderer.HOLDING_BLOCK, false, false),
            new Case(ModItems.PIPE.asStack(), CrabClawItemInHandRenderer.HOLDING_BLOCK_SLAB, false, false),
            new Case(ModItems.CHECK_VALVE.asStack(), CrabClawItemInHandRenderer.HOLDING_BLOCK_SLAB, false, false),
            new Case(new ItemStack(Items.TORCH), CrabClawItemInHandRenderer.HOLDING_ITEM, false, false),
            new Case(new ItemStack(Items.TRIDENT), CrabClawItemInHandRenderer.HOLDING_ITEM, false, true));
        try {
            for (var arm : HumanoidArm.values()) {
                player.setMainArm(arm);
                for (var test : cases) {
                    manager.setMainHandItem(test.stack);
                    manager.setOffHandItem(ModItems.CRAB_CLAW.asStack());
                    var pose = new PoseStack();
                    var nodes = new SubmitNodeStorage();
                    manager.crabClawItemRenderer.render(player, 0, 0, InteractionHand.MAIN_HAND, 0, test.stack, 0, pose, nodes, 0xF000F0);
                    var items = nodes.order(0).getItemSubmits();
                    var holding = client.getModelManager().getStandaloneModel(test.model);
                    if (items.size() != 1 || items.getFirst().quads() != holding.quads()) {
                        throw new IllegalStateException("Incorrect holding model: " + test.stack + " / " + arm);
                    }
                    int sign = arm == HumanoidArm.RIGHT ? 1 : -1;
                    var expected = new PoseStack();
                    if (test.model == CrabClawItemInHandRenderer.HOLDING_ITEM) {
                        expected.mulPose(Axis.ZP.rotationDegrees(5 * sign));
                        expected.scale(0.75F, 0.75F, 0.75F);
                        expected.translate(0, 0.45F, 0.02F);
                        if (test.spear) expected.translate(-0.23F * sign, 0, 0.07F);
                    } else {
                        expected.mulPose(Axis.YP.rotationDegrees(60 * sign));
                        expected.mulPose(Axis.XP.rotationDegrees(test.panel ? 45 : 25));
                        expected.scale(0.5F, 0.5F, 0.5F);
                        expected.translate(0.25F * sign, test.panel ? 0.2F : 0.4F, test.panel ? -0.35F : -0.1F);
                    }
                    if (!pose.last().pose().equals(expected.last().pose(), 0.00001F)) {
                        throw new IllegalStateException("Incorrect held-item transform: " + test.stack + " / " + arm);
                    }
                }
                var cfa = ModBlocks.CELESTIAL_FORGING_ANVIL.asStack();
                manager.setMainHandItem(cfa);
                var pose = new PoseStack();
                var nodes = new SubmitNodeStorage();
                manager.crabClawItemRenderer.render(player, 0, 0, InteractionHand.MAIN_HAND, 0, cfa, 0, pose, nodes, 0xF000F0);
                if (!pose.last().pose().equals(new Matrix4f()) || !nodes.order(0).getItemSubmits().isEmpty()) {
                    throw new IllegalStateException("Crab claw changed the CFA's own hand pose");
                }
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            player.getInventory().setItem(9, ModItems.BUILDING_ROD.asStack());
            manager.setMainHandItem(player.getMainHandItem());
            manager.setOffHandItem(ItemStack.EMPTY);
            var nodes = new SubmitNodeStorage();
            manager.render(player, 0, 0, InteractionHand.MAIN_HAND, 0, player.getMainHandItem(), 0, new PoseStack(), nodes, 0xF000F0);
            if (nodes.order(0).getItemSubmits().size() != 1) {
                throw new IllegalStateException("Carried building rod lost the shared claw path");
            }
            AnvilCraft.LOGGER.info("PORT_CRAB_PROBE_PASSED: 18 model/pose cases, CFA guard and carried building rod");
        } finally {
            player.setMainArm(originalArm);
            player.setItemInHand(InteractionHand.MAIN_HAND, main);
            player.setItemInHand(InteractionHand.OFF_HAND, off);
            player.getInventory().setItem(9, carried);
        }
    }

    public static void verifyThrowing(Minecraft client) {
        if (manager == null || !client.player.isUsingItem()) throw new IllegalStateException("Throw probe not initialized");
        manager.setMainHandItem(client.player.getMainHandItem());
        manager.setOffHandItem(ModItems.CRAB_CLAW.asStack());
        var nodes = new SubmitNodeStorage();
        manager.crabClawItemRenderer.render(client.player, 0, 0, InteractionHand.MAIN_HAND, 0,
            client.player.getMainHandItem(), 0, new PoseStack(), nodes, 0xF000F0);
        var holding = client.getModelManager().getStandaloneModel(CrabClawItemInHandRenderer.HOLDING_ITEM);
        var expected = new PoseStack();
        boolean left = client.player.getMainArm() == HumanoidArm.LEFT;
        expected.mulPose(Axis.XP.rotationDegrees(left ? -90 : 90));
        holding.transforms().getTransform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).apply(left, expected.last());
        var items = nodes.order(0).getItemSubmits();
        if (items.size() != 1 || !items.getFirst().pose().pose().equals(expected.last().pose(), 0.00001F)) {
            throw new IllegalStateException("Throwing claw did not rotate independently by 90 degrees");
        }
        AnvilCraft.LOGGER.info("PORT_CRAB_THROW_POSE_PASSED: {} / {}", client.player.getMainHandItem(), client.player.getMainArm());
    }

    private record Case(ItemStack stack, StandaloneModelKey<CrabClawItemInHandRenderer.HoldingModel> model, boolean panel, boolean spear) {
    }
}
