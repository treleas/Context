package di;

import net.treleas.context.di.Di;
import net.treleas.context.di.Lifecycle;
import net.treleas.context.di.injector.Injector;
import net.treleas.context.di.pool.BeanPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiTest {

    @Mock
    private BeanPool beanPool;
    @Mock
    private Injector injector;
    private Di di;

    @BeforeEach
    void setUp() {
        di = Di.create(beanPool, injector);
    }

    @Test
    void shouldInjectAndMountWhenAppendingBean() {
        // Given
        Object owner = new Object();
        MyService service = mock(MyService.class);

        // When
        di.appendBean(owner, MyService.class, service);

        // Then
        verify(injector).inject(di, service);
        verify(service).mount();
        verify(beanPool).appendBeam(owner, MyService.class, null, service);
    }

    @Test
    void shouldPreferTaggedBeanOverClassified() {
        // Given
        String tag = "special";
        String taggedValue = "I am tagged";
        String classValue = "I am class-based";

        when(beanPool.taggedBean(tag)).thenReturn(taggedValue);

        // When
        String result = di.bean(String.class, tag);

        // Then
        assertThat(result).isEqualTo(taggedValue);
        verify(beanPool, never()).classifiedBean(any()); // До класса не дошли
    }

    @Test
    void shouldUnmountWhenRemovingOwnedBeans() {
        // Given
        Object owner = new Object();
        MyService service = mock(MyService.class);
        //noinspection rawtypes,unchecked
        when(beanPool.removeOwned(owner)).thenReturn((Collection) List.of(service));

        // When
        di.removeOwned(owner);

        // Then
        verify(service).unmount(); // Проверяем, что жизненный цикл завершен
    }

    @Test
    void shouldReturnEmptyOptionalIfBeanNotFound() {
        // Given
        when(beanPool.classifiedBean(String.class)).thenReturn(null);

        // When
        Optional<String> result = di.safeBean(String.class);

        // Then
        assertThat(result).isEmpty();
    }

    interface MyService extends Lifecycle {
    }
}