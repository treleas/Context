package net.treleas.context.di.injector;

import net.treleas.context.di.Di;
import net.treleas.context.di.annotation.Bean;
import net.treleas.context.di.annotation.Inject;
import net.treleas.context.di.annotation.Tag;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class ReflectionInjector implements Injector {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectionInjector.class);

    ReflectionInjector() {
    }

    @Override
    public void inject(@NonNull Di di, @NonNull Object bean) {
        if (!bean.getClass().isAnnotationPresent(Bean.class)) {
            return;
        }

        Class<?> current = bean.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (current.isRecord()) {
                    LOGGER.debug("Skipping field {} because {} is a record", field.getName(), current.getSimpleName());
                    continue;
                }

                if (!field.isAnnotationPresent(Inject.class)) {
                    continue;
                }

                try {
                    Tag annotation = field.getAnnotation(Tag.class);
                    String tag = (annotation != null && !annotation.tag().isEmpty()) ? annotation.tag() : null;
                    Object value = (tag != null)
                            ? di.taggedBean(tag)
                            : di.classifiedBean(field.getType());
                    if (value == null) {
                        LOGGER.info("Bean not found for annotated field {}", field.getName());
                        continue;
                    }

                    field.setAccessible(true);
                    field.set(bean, value);
                } catch (IllegalAccessException e) {
                    LOGGER.error("Failed to inject field \"{}\"", field.getName(), e);
                }
            }
            current = current.getSuperclass();
        }
    }
}
