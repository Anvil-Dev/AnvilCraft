package dev.dubhe.anvilcraft.data;

import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumBlockModelGenerator;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.core.Direction;

/**
 * 在物理客户端初始化模型数据生成，避免专用服务器加载客户端模型类型。
 */
public class AnvilCraftClientDatagen {
    /**
     * 在数据生成器构造前注册客户端模型回调。
     */
    public static void init() {
        AnvilCraft.REGISTRUM.addDataGenerator(ProviderType.BLOCKSTATE, AnvilCraftClientDatagen::generateLargeCauldronFire);
    }

    private static void generateLargeCauldronFire(RegistrumBlockModelGenerator provider) {
        TextureSlot fire0 = TextureSlot.create("0");
        TextureSlot fire1 = TextureSlot.create("1");
        provider.getBuilder()
            .parent(AnvilCraft.of("block/fire_cauldron_fire4"))
            .texture(fire0, AnvilCraft.of("block/large_cauldron_fire_0"))
            .texture(fire1, AnvilCraft.of("block/large_cauldron_fire_1"))
            .texture(TextureSlot.PARTICLE, AnvilCraft.of("block/large_cauldron_fire_0"))
            .transformTemplate(template -> template
                .element(element -> element
                    .from(8, 15, 0.8F).to(8, 31, 15.2F)
                    .shade(false)
                    .rotation(rotation -> rotation.origin(8, 22, 8).singleAxis(Direction.Axis.Y, 45).rescale(true))
                    .face(Direction.EAST, face -> face.uvs(0, 0, 16, 16).texture(fire0))
                    .face(Direction.WEST, face -> face.uvs(0, 0, 16, 16).texture(fire0)))
                .element(element -> element
                    .from(0.8F, 15, 8).to(15.2F, 31, 8)
                    .shade(false)
                    .rotation(rotation -> rotation.origin(8, 22, 8).singleAxis(Direction.Axis.Y, 45).rescale(true))
                    .face(Direction.NORTH, face -> face.uvs(0, 0, 16, 16).texture(fire1))
                    .face(Direction.SOUTH, face -> face.uvs(0, 0, 16, 16).texture(fire1))))
            .build(AnvilCraft.of("block/large_cauldron_fire"));
    }
}
