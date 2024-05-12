package dev.dubhe.anvilcraft.util;

import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnchantmentDisableUtil {

    @Getter
    private List<Enchantment> disableEnchantmentList = new ArrayList<>();

    protected static final List<String> DEFAULT_DISABLE_ENCHANTMENTS
        = Arrays.asList(
            "anvilcraft:harvest",
            "anvilcraft:beheading",
            "anvilcraft:felling"
        );

    public EnchantmentDisableUtil() {
        addAllDisableEnchantment(DEFAULT_DISABLE_ENCHANTMENTS);
    }


    /**
     * 添加禁用在附魔台中禁用的附魔
     *
     * @param enchantmentLocation 附魔命名路径
     * @return Boolean
     */
    public boolean addDisableEnchantment(String enchantmentLocation) {
        ResourceLocation enchantmentId = ResourceLocation.tryParse(enchantmentLocation);
        Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(enchantmentId);
        return disableEnchantmentList.add(enchantment);
    }

    /**
     * 添加禁用在附魔台中禁用的附魔
     *
     * @param enchantmentPath 附魔命名路径
     * @return Boolean
     */
    public boolean addDisableEnchantment(String enchantmentNamespace, String enchantmentPath) {
        ResourceLocation enchantmentId = ResourceLocation.tryBuild(enchantmentNamespace, enchantmentPath);
        Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(enchantmentId);
        return disableEnchantmentList.add(enchantment);
    }

    /**
     * 添加禁用在附魔台中禁用的附魔
     *
     * @param enchantmentIdStringList 附魔命名值列表
     * @return Boolean
     */
    public boolean addAllDisableEnchantment(List<String> enchantmentIdStringList) {
        if (enchantmentIdStringList.toArray().length == 0) {
            return false;
        }
        for (String string : enchantmentIdStringList) {
            addDisableEnchantment(string);
        }
        return true;
    }

}
