package dev.dubhe.anvilcraft.datafix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

import java.util.Objects;
import java.util.Optional;

public class AutoCrafterRenameFix extends DataFix {
    private final String name;

    public AutoCrafterRenameFix(Schema outputSchema) {
        super(outputSchema, false);
        this.name = "anvilcraft:auto_crafter";
    }

    @Override
    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.BLOCK_NAME);
        Type<Pair<String, String>> type2 = DSL.named(
                References.BLOCK_NAME.typeName(),
                NamespacedSchema.namespacedString()
        );
        if (!Objects.equals(type, type2)) {
            throw new IllegalStateException("block type is not what was expected.");
        } else {
            TypeRewriteRule typeRewriteRule = this.fixTypeEverywhere(
                    this.name + " for block",
                    type2,
                    (ops) -> (pair) -> pair.mapSecond(this::fixBlock)
            );
            TypeRewriteRule typeRewriteRule2 = this.fixTypeEverywhereTyped(
                    this.name + " for block_state",
                    this.getInputSchema().getType(References.BLOCK_STATE),
                    (typed) -> typed.update(DSL.remainderFinder(), (dynamic) -> {
                        Optional<String> nameOptional = dynamic.get("Name").asString().result();
                        Dynamic<?> dyn = nameOptional.isPresent()
                                ? dynamic.set("Name", dynamic.createString(this.fixBlock(nameOptional.get())))
                                : dynamic;
                        System.out.println("dyn = " + dyn);
                        return dyn;
                    }));
            return TypeRewriteRule.seq(typeRewriteRule, typeRewriteRule2);
        }
    }

    private String fixBlock(String name) {
        return name.equals("anvilcraft:auto_crafter") ? "anvilcraft:batch_crafter" : name;
    }
}
