package dev.dubhe.anvilcraft.api.component;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import lombok.Getter;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.fml.loading.FMLLoader;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslatableContents implements ComponentContents {
    public static final Object[] NO_ARGS = new Object[0];
    private static final Codec<Object> PRIMITIVE_ARG_CODEC = ExtraCodecs.JAVA.validate(TranslatableContents::filterAllowedArguments);
    @SuppressWarnings("NullableProblems")
    private static final Codec<Object> ARG_CODEC = Codec.either(
        TranslatableContents.PRIMITIVE_ARG_CODEC,
        ComponentSerialization.CODEC
    ).xmap(
        e -> e.map(Objects::requireNonNull, component -> Objects.requireNonNullElse(component.tryCollapseToString(), component)),
        o -> o instanceof Component c
             ? Either.right(c)
             : Either.left(Objects.requireNonNull(o, "Translation argument"))
    );
    public static final MapCodec<TranslatableContents> MAP_CODEC = CodecUtil.mapCodec(
        Codec.STRING
            .fieldOf("translate")
            .forGetter(o -> o.key),
        ComponentSerialization.flatRestrictedCodec(Integer.MAX_VALUE)
            .lenientOptionalFieldOf("fallback")
            .forGetter(o -> Optional.ofNullable(o.fallback)),
        TranslatableContents.ARG_CODEC
            .listOf()
            .optionalFieldOf("with")
            .forGetter(o -> TranslatableContents.adjustArgs(o.args)),
        TranslatableContents::create
    );
    private static final FormattedText TEXT_PERCENT = FormattedText.of("%");
    private static final FormattedText TEXT_NULL = FormattedText.of("null");
    @Getter
    private final String key;
    @Getter
    private final @Nullable Component fallback;
    @Getter
    private final Object[] args;
    private @Nullable Language decomposedWith;
    private List<FormattedText> decomposedParts = ImmutableList.of();
    private static final Pattern FORMAT_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

    private static DataResult<Object> filterAllowedArguments(@Nullable Object result) {
        return !TranslatableContents.isAllowedPrimitiveArgument(result)
               ? DataResult.error(() -> "This value needs to be parsed as component")
               : DataResult.success(result);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isAllowedPrimitiveArgument(@Nullable Object object) {
        return object instanceof Number || object instanceof Boolean || object instanceof String;
    }

    private static Optional<List<Object>> adjustArgs(Object[] args) {
        return args.length == 0 ? Optional.empty() : Optional.of(Arrays.asList(args));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static Object[] adjustArgs(Optional<List<Object>> args) {
        return args.map(a -> a.isEmpty() ? TranslatableContents.NO_ARGS : a.toArray()).orElse(TranslatableContents.NO_ARGS);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static TranslatableContents create(String key, Optional<Component> fallback, Optional<List<Object>> args) {
        return new TranslatableContents(key, fallback.orElse(null), TranslatableContents.adjustArgs(args));
    }

    public TranslatableContents(String key, @Nullable Component fallback, Object[] args) {
        this.key = key;
        this.fallback = fallback;
        this.args = args;
        // Neo: This is transitively called by some static initializers. To allow using Minecraft classes from tests
        // without fully initializing FML, we disable the validation if FML is not initialized.
        var loader = FMLLoader.getCurrentOrNull();
        if (loader != null && !loader.isProduction()) {
            for (Object arg : this.args) {
                if (!(arg instanceof Component) && !TranslatableContents.isAllowedPrimitiveArgument(arg)) {
                    throw new IllegalArgumentException(
                        "TranslatableContents' arguments must be either a Component, Number, Boolean, or a String. Was given " + arg
                        + " for " + this.key);
                }
            }
        }
    }

    @Override
    public MapCodec<TranslatableContents> codec() {
        return TranslatableContents.MAP_CODEC;
    }

    private void decompose() {
        Language currentLanguage = Language.getInstance();
        if (currentLanguage != this.decomposedWith) {
            this.decomposedWith = currentLanguage;

            Component langComponent = currentLanguage.getComponent(this.key);
            if (langComponent != null) {
                this.decomposedParts = ImmutableList.of(langComponent);
                return;
            }

            String format;
            if (this.fallback != null) {
                format = currentLanguage.getOrDefault(this.key, this.key);
                if (format.equals(this.key)) {
                    format = this.fallback.getString();
                }
            } else {
                format = currentLanguage.getOrDefault(this.key);
            }

            try {
                ImmutableList.Builder<FormattedText> parts = ImmutableList.builder();
                this.decomposeTemplate(format, parts::add);
                this.decomposedParts = parts.build();
            } catch (TranslatableFormatException var4) {
                this.decomposedParts = ImmutableList.of(FormattedText.of(format));
            }
        }
    }

    private void decomposeTemplate(String template, Consumer<FormattedText> decomposedParts) {
        Matcher matcher = TranslatableContents.FORMAT_PATTERN.matcher(template);

        try {
            int replacementIndex = 0;
            int current = 0;

            while (matcher.find(current)) {
                int start = matcher.start();
                int end = matcher.end();
                if (start > current) {
                    String prefix = template.substring(current, start);
                    if (prefix.indexOf(37) != -1) {
                        throw new IllegalArgumentException();
                    }

                    decomposedParts.accept(FormattedText.of(prefix));
                }

                String formatType = matcher.group(2);
                String formatString = template.substring(start, end);
                if ("%".equals(formatType) && "%%".equals(formatString)) {
                    decomposedParts.accept(TranslatableContents.TEXT_PERCENT);
                } else {
                    if (!"s".equals(formatType)) {
                        throw new TranslatableFormatException(this, "Unsupported format: '" + formatString + "'");
                    }

                    String possiblePositionIndex = matcher.group(1);
                    int index = possiblePositionIndex != null ? Integer.parseInt(possiblePositionIndex) - 1 : replacementIndex++;
                    decomposedParts.accept(this.getArgument(index));
                }

                current = end;
            }

            if (current < template.length()) {
                String tail = template.substring(current);
                if (tail.indexOf(37) != -1) {
                    throw new IllegalArgumentException();
                }

                decomposedParts.accept(FormattedText.of(tail));
            }
        } catch (IllegalArgumentException var12) {
            throw new TranslatableFormatException(this, var12);
        }
    }

    private FormattedText getArgument(int index) {
        if (index >= 0 && index < this.args.length) {
            Object arg = this.args[index];
            if (arg instanceof Component componentArg) {
                return componentArg;
            } else {
                // noinspection ConstantValue
                return arg == null ? TranslatableContents.TEXT_NULL : FormattedText.of(arg.toString());
            }
        } else {
            throw new TranslatableFormatException(this, index);
        }
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
        Object[] argsCopy = new Object[this.args.length];

        for (int i = 0; i < argsCopy.length; i++) {
            Object param = this.args[i];
            if (param instanceof Component component) {
                argsCopy[i] = ComponentUtils.resolve(context, component, recursionDepth);
            } else {
                argsCopy[i] = param;
            }
        }

        return MutableComponent.create(new TranslatableContents(this.key, this.fallback, argsCopy));
    }

    @Override
    public boolean equals(Object o) {
        return this == o
               || o instanceof TranslatableContents that
                  && Objects.equals(this.key, that.key)
                  && Objects.equals(this.fallback, that.fallback)
                  && Arrays.equals(this.args, that.args);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(this.key);
        result = 31 * result + Objects.hashCode(this.fallback);
        return 31 * result + Arrays.hashCode(this.args);
    }

    @Override
    public String toString() {
        return "translation{key='"
               + this.key
               + "'"
               + (this.fallback != null ? ", fallback='" + this.fallback + "'" : "")
               + ", args="
               + Arrays.toString(this.args)
               + "}";
    }
}
