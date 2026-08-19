package dev.dubhe.anvilcraft.init.item.tabs;

import dev.anvilcraft.lib.v2.registrum.util.CreativeTabSection;
import dev.anvilcraft.lib.v2.registrum.util.CreativeTabSections;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

public class ItemsSections extends DisplayItemsGenerator {
    @Override
    public void accept() {
        if (!(this.output instanceof CreativeTabSections sections)) {
            return;
        }
        sections.section(
            CreativeTabSection.builder(
                AnvilCraft.of("textures/gui/creative_inventory/section/items/fluids.png")
            )
            .text(Component.translatable("anvilcraft.creative.section.items.fluids"))
            .build(),
            content -> {

            }
        );
        sections.section(
            CreativeTabSection.builder(
                AnvilCraft.of("textures/gui/creative_inventory/section/items/foods.png")
            )
            .text(Component.translatable("anvilcraft.creative.section.items.foods"))
            .build(),
            content -> {

            }
        );
        sections.section(
            CreativeTabSection.builder(
                AnvilCraft.of("textures/gui/creative_inventory/section/items/guns.png")
            )
            .text(Component.translatable("anvilcraft.creative.section.items.guns"))
            .build(),
            content -> {

            }
        );
        sections.section(
            CreativeTabSection.builder(
                AnvilCraft.of("textures/gui/creative_inventory/section/items/magic.png")
            )
            .text(Component.translatable("anvilcraft.creative.section.items.magic"))
            .build(),
            content -> {

            }
        );
        sections.section(
            CreativeTabSection.builder(
                AnvilCraft.of("textures/gui/creative_inventory/section/items/materials.png")
            )
            .text(Component.translatable("anvilcraft.creative.section.items.materials"))
            .build(),
            content -> {

            }
        );
        sections.section(
            CreativeTabSection.builder(
                AnvilCraft.of("textures/gui/creative_inventory/section/items/power.png")
            )
            .text(Component.translatable("anvilcraft.creative.section.items.power"))
            .build(),
            content -> {

            }
        );
        sections.section(
            CreativeTabSection.builder(
                AnvilCraft.of("textures/gui/creative_inventory/section/items/produced.png")
            )
            .text(Component.translatable("anvilcraft.creative.section.items.produced"))
            .build(),
            content -> {

            }
        );
        sections.section(
            CreativeTabSection.builder(
                AnvilCraft.of("textures/gui/creative_inventory/section/items/smithing_template.png")
            )
            .text(Component.translatable("anvilcraft.creative.section.items.smithing_template"))
            .build(),
            content -> {

            }
        );
        sections.section(
            CreativeTabSection.builder(
                AnvilCraft.of("textures/gui/creative_inventory/section/items/tools.png")
            )
            .text(Component.translatable("anvilcraft.creative.section.items.tools"))
            .build(),
            content -> {

            }
        );
        // 小类物品填好后删除下面两行兜底
        CreativeModeTab.ItemDisplayParameters parameters = this.itemDisplayParameters;
        if (parameters == null) return;
        new Ingredients().accept(parameters, sections);
        new ToolsAndUtilities().accept(parameters, sections);
    }
}
