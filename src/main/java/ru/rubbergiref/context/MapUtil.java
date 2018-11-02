package ru.rubbergiref.context;

import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class MapUtil {
    private final boolean HAS_FAST_UTIL = ensureFastUtil();

    public <A,B> Map<A, B> synchronizedMap() {
        if (MapUtil.HAS_FAST_UTIL) {
            return Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
        }

        return new ConcurrentHashMap<>();
    }

    private boolean ensureFastUtil() {
        try {
            Class.forName("it.unimi.dsi.fastutil.objects.Object2ObjectMap");
            return true;
        } catch (final ClassNotFoundException e) {
            return false;
        }
    }
}
