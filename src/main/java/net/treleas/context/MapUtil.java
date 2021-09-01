package net.treleas.context;

import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class MapUtil {
    public <A,B> @NotNull Map<A, B> synchronizedMap() {
        try {
            return Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
        } catch (final Exception ignored) {
            return new ConcurrentHashMap<>();
        }
    }
}
