package dev.dubhe.anvilcraft.api.component;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.chat.Style;
import net.neoforged.fml.ModList;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter(AccessLevel.PROTECTED)
public class ModNameContents implements ComponentContents {
    public static final MapCodec<ModNameContents> CODEC = CodecUtil.mapCodec(
        Codec.STRING
            .fieldOf("mod_id")
            .forGetter(ModNameContents::getId),
        ModNameContents::new
    );
    private static final String KEY = "component_content.anvilcraft.mod_name.unknown";
    private final String id;
    private @Nullable Language decomposedWith;
    private List<FormattedText> decomposedParts = ImmutableList.of();

    public ModNameContents(String id) {
        this.id = id;
    }

    private void decompose() {
        Language currentLanguage = Language.getInstance();
        if (currentLanguage == this.decomposedWith) {
            return;
        }

        this.decomposedWith = currentLanguage;

        Optional<String> modNameOp = ModList.get()
            .getModContainerById(this.id)
            .map(container -> container.getModInfo().getDisplayName());
        if (modNameOp.isPresent()) {
            this.decomposedParts = ImmutableList.of(FormattedText.of(modNameOp.get()));
            return;
        }

        Component unknown = Component.translatable(ModNameContents.KEY, this.id);
        this.decomposedParts = ImmutableList.of(unknown);
    }

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> output, Style currentStyle) {
        this.decompose();

        for (FormattedText part : this.decomposedParts) {
            Optional<T> result = part.visit(output, currentStyle);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> output) {
        this.decompose();

        for (FormattedText part : this.decomposedParts) {
            Optional<T> result = part.visit(output);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    @Override
    public MutableComponent resolve(ResolutionContext context, int recursionDepth) throws CommandSyntaxException {
        return MutableComponent.create(new ModNameContents(this.id));
    }

    @Override
    public MapCodec<? extends ComponentContents> codec() {
        return ModNameContents.CODEC;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ModNameContents that)) return false;
        return Objects.equals(this.getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getId());
    }
}
