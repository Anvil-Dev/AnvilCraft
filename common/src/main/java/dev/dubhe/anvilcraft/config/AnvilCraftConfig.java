package dev.dubhe.anvilcraft.config;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.AutoGen;
import dev.isxander.yacl3.config.v2.api.autogen.CustomDescription;
import dev.isxander.yacl3.config.v2.api.autogen.DoubleSlider;
import dev.isxander.yacl3.config.v2.api.autogen.IntSlider;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.platform.YACLPlatform;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;


public class AnvilCraftConfig {

    public static final ConfigClassHandler<AnvilCraftConfig> HANDLER = ConfigClassHandler.createBuilder(AnvilCraftConfig.class)
            .id(AnvilCraft.of("config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(YACLPlatform.getConfigDir().resolve(AnvilCraft.MOD_ID + ".json5"))
                    .setJson5(true)
                    .build())
            .build();

    @AutoGen(category = "common")
    @IntSlider(step = 1, min = 1, max = 64)
    @CustomDescription("config.anvilcraft.option.anvilEfficiency")
    @SerialEntry(comment = "Maximum number of items processed by the anvil at the same time")
    public int anvilEfficiency = 64;

    @AutoGen(category = "common")
    @IntSlider(step = 1, min = 1, max = 8)
    @CustomDescription("config.anvilcraft.option.lightningStrikeDepth")
    @SerialEntry(comment = "Maximum depth a lightning strike can reach")
    public int lightningStrikeDepth = 2;

    @AutoGen(category = "common")
    @IntSlider(step = 1, min = 1, max = 16)
    @CustomDescription("config.anvilcraft.option.magnetAttractsDistance")
    @SerialEntry(comment = "Maximum distance a magnet attracts")
    public int magnetAttractsDistance = 5;

    @AutoGen(category = "common")
    @DoubleSlider(step = 0.5, min = 0.5, max = 16)
    @CustomDescription("config.anvilcraft.option.magnetItemAttractsRadius")
    @SerialEntry(comment = "Maximum radius a handheld magnet attracts")
    public double magnetItemAttractsRadius = 8;

    @AutoGen(category = "common")
    @IntSlider(step = 1, min = 1, max = 16)
    @CustomDescription("config.anvilcraft.option.redstoneEmpRadius")
    @SerialEntry(comment = "Redstone EMP distance generated per block dropped by the anvil")
    public int redstoneEmpRadius = 6;

    @AutoGen(category = "common")
    @IntSlider(step = 1, min = 1, max = 64)
    @CustomDescription("config.anvilcraft.option.redstoneEmpMaxRadius")
    @SerialEntry(comment = "Maximum distance of redstone EMP")
    public int redstoneEmpMaxRadius = 24;

    @AutoGen(category = "common")
    @IntSlider(step = 1, min = 1, max = 64)
    @CustomDescription("config.anvilcraft.option.chuteMaxCooldown")
    @SerialEntry(comment = "Maximum cooldown time of chute (in ticks)")
    public int chuteMaxCooldown = 4;

    public static Screen getConfigScreen(@Nullable Screen parent) {
//        return YetAnotherConfigLib.createBuilder()
//                .title(Component.translatable("config.screen.anvilcraft"))
//                .category(ConfigCategory.createBuilder()
//                        .name(Component.translatable("config.screen.anvilcraft.common"))
//                        .tooltip(Component.translatable("config.screen.anvilcraft.common.tooltip"))
//                        .option(Option.<Integer>createBuilder()
//                                .name(Component.translatable("config.anvilcraft.option.anvilEfficiency"))
//                                .description(OptionDescription.of(Component.translatable("config.anvilcraft.comment.anvilEfficiency")))
//                                .binding(64, () -> this.anvilEfficiency, newVal -> this.anvilEfficiency = newVal)
//                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
//                                        .range(1, 64)
//                                        .step(1))
//                                .build())
//
//                        .build())
//                .build()
//                .generateScreen(parent);

        return HANDLER.generateGui().generateScreen(parent);
    }
}