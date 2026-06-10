package dev.dubhe.anvilcraft.api.component;

import java.util.Locale;

public class TranslatableFormatException extends IllegalArgumentException {
    public TranslatableFormatException(TranslatableContents component, String message) {
        super(String.format(Locale.ROOT, "Error parsing: %s: %s", component, message));
    }

    public TranslatableFormatException(TranslatableContents component, int index) {
        super(String.format(Locale.ROOT, "Invalid index %d requested for %s", index, component));
    }

    public TranslatableFormatException(TranslatableContents component, Throwable t) {
        super(String.format(Locale.ROOT, "Error while parsing: %s", component), t);
    }
}
