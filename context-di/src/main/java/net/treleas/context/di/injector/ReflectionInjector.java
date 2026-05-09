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

        Class<?> clazz = bean.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (clazz.isRecord()) {
                    LOGGER.debug("Skipping field {} because {} is a record", field.getName(), clazz.getSimpleName());
                    continue;
                }

                if (!field.isAnnotationPresent(Inject.class)) {
                    continue;
                }

                try {
                    Tag annotation = field.getAnnotation(Tag.class);
                    String tag = (annotation != null && !annotation.tag().isEmpty()) ? annotation.tag() : null;
                    Object provider = (tag != null)
                            ? di.taggedBean(tag)
                            : di.classifiedBean(field.getType());
                    if (provider != null) {
                        field.setAccessible(true);
                        field.set(bean, provider);
                    }
                } catch (IllegalAccessException e) {
                    LOGGER.error("Failed to inject field \"{}\"", field.getName(), e);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }
}
