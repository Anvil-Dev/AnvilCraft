package dev.dubhe.anvilcraft.util.recover;

import java.util.UUID;
import javax.annotation.Nullable;

public record RecoverEntry<T>(UUID id, @Nullable T value) {
}
