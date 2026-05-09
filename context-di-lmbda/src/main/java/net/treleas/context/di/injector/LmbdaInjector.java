package net.treleas.context.di.injector;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.treleas.context.di.Di;
import net.treleas.context.di.annotation.Inject;
import net.treleas.context.di.annotation.Tag;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lanternpowered.lmbda.LambdaFactory;
import org.lanternpowered.lmbda.LambdaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class LmbdaInjector implements Injector {
    private static final Logger LOGGER = LoggerFactory.getLogger(LmbdaInjector.class);

    private final Reference2ObjectMap<Class<?>, Accessor[]> cache =
            Reference2ObjectMaps.synchronize(new Reference2ObjectOpenHashMap<>());
    private final MethodHandles.Lookup lookup = MethodHandles.lookup();

    private LmbdaInjector() {
    }

    @Override
    public void inject(@NonNull Di di, @NonNull Object bean) {
        Accessor[] accessors = cache.computeIfAbsent(bean.getClass(), this::createAccessors);

        for (Accessor accessor : accessors) {
            Object value = (accessor.tag != null)
                    ? di.taggedBean(accessor.tag)
                    : di.classifiedBean(accessor.type);

            if (value != null) {
                accessor.setter.accept(bean, value);
            }
        }
    }

    private Accessor[] createAccessors(Class<?> clazz) {
        List<Accessor> list = new ArrayList<>();
        Class<?> current = clazz;

        try {
            while (current != null && current != Object.class) {
                MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(current, lookup);

                for (Field field : current.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Inject.class)) {
                        if (current.isRecord()) {
                            LOGGER.debug("Skipping field {} because {} is a record", field.getName(), current.getSimpleName());
                            continue;
                        }

                        field.setAccessible(true);

                        Tag tagAnn = field.getAnnotation(Tag.class);
                        String tag = (tagAnn != null && !tagAnn.tag().isEmpty()) ? tagAnn.tag() : null;

                        MethodHandle setterHandle = privateLookup.unreflectSetter(field);
                        BiConsumer<Object, Object> setter = LambdaFactory.create(
                                new LambdaType<>() {
                                },
                                setterHandle
                        );

                        list.add(new Accessor(setter, field.getType(), tag));
                    }
                }
                current = current.getSuperclass();
            }
        } catch (IllegalAccessException e) {
            LOGGER.error("Failed to access fields for {}", clazz, e);
        }

        return list.toArray(new Accessor[0]);
    }

    private record Accessor(BiConsumer<Object, Object> setter, Class<?> type, @Nullable String tag) {
    }

    public static @NonNull Injector injector() {
        return new LmbdaInjector();
    }
}
