package net.treleas.context.di.injector;

import net.treleas.context.di.Di;
import net.treleas.context.di.annotation.Bean;
import net.treleas.context.di.annotation.Inject;
import net.treleas.context.di.annotation.Tag;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VarHandleInjector implements Injector {
    private static final Logger LOGGER = LoggerFactory.getLogger(VarHandleInjector.class);

    private final Map<Class<?>, List<FieldAccessor>> cache = new ConcurrentHashMap<>();
    private final MethodHandles.Lookup lookup = MethodHandles.lookup();

    VarHandleInjector() {
    }

    @Override
    public void inject(@NonNull Di di, @NonNull Object bean) {
        Class<?> clazz = bean.getClass();
        if (!clazz.isAnnotationPresent(Bean.class)) {
            return;
        }

        List<FieldAccessor> accessors = cache.computeIfAbsent(clazz, this::createAccessors);

        for (FieldAccessor accessor : accessors) {
            Object provider = (accessor.tag != null)
                    ? di.taggedBean(accessor.tag)
                    : di.classifiedBean(accessor.type);
            if (provider == null) {
                LOGGER.info("Bean not found for annotated field {}", accessor.type.getSimpleName());
                continue;
            }

            try {
                accessor.handle.set(bean, provider);
            } catch (Exception e) {
                LOGGER.error("Failed to inject via VarHandle", e);
            }
        }
    }

    private @NonNull List<FieldAccessor> createAccessors(@NonNull Class<?> clazz) {
        List<FieldAccessor> list = new ArrayList<>();
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    if (current.isRecord()) {
                        LOGGER.debug("Skipping field {} because {} is a record", field.getName(), current.getSimpleName());
                        continue;
                    }

                    try {
                        field.setAccessible(true);
                        Tag annotation = field.getAnnotation(Tag.class);
                        String tag = (annotation != null && !annotation.tag().isEmpty()) ? annotation.tag() : null;

                        VarHandle handle = MethodHandles.privateLookupIn(current, lookup)
                                .unreflectVarHandle(field);

                        list.add(new FieldAccessor(handle, field.getType(), tag));
                    } catch (Exception e) {
                        LOGGER.error("Failed to create VarHandle for {}", field.getName(), e);
                    }
                }
            }
            current = current.getSuperclass();
        }
        return list;
    }

    private record FieldAccessor(VarHandle handle, Class<?> type, String tag) {
    }
}
