package dev.dubhe.anvilcraft.saved.multiphase.fixer;

import dev.dubhe.anvilcraft.saved.datafixer.DataFixer;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class V0_1 extends DataFixer {
    @Override
    public double version() {
        return 0.1;
    }

    @Override
    public CompoundTag fixData(CompoundTag nbt, HolderLookup.Provider registries) {
        ListTag multiphases = new ListTag();
        for (Tag tag : nbt.getList("Multiphases", Tag.TAG_COMPOUND)) {
            CompoundTag old = Util.cast(tag);
            CompoundTag now = new CompoundTag();
            now.put(old.getUUID("id").toString(), old.get("content"));
            multiphases.add(now);
        }
        nbt.put("multiphases", multiphases);
        ListTag recovers = new ListTag();
        for (Tag tag : nbt.getList("Recovers", Tag.TAG_COMPOUND)) {
            CompoundTag old = Util.cast(tag);
            CompoundTag now = new CompoundTag();
            now.put(old.getUUID("id").toString(), old.get("content"));
            recovers.add(now);
        }
        nbt.put("recovers", recovers);
        return nbt;
    }
}
