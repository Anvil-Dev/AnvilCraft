package dev.dubhe.anvilcraft.data.tags;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumTagsProvider;
import dev.dubhe.anvilcraft.init.entity.ModVillagers;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.village.poi.PoiType;

public class PoiTypeTagLoader {
    /// 初始化兴趣点类型标签
    ///
    /// @param provider 提供器
    public static void init(RegistrumTagsProvider<PoiType> provider) {
        provider.rawBuilder(PoiTypeTags.ACQUIRABLE_JOB_SITE)
            .addElement(ModVillagers.JEWELER_POI.getId());
    }
}
