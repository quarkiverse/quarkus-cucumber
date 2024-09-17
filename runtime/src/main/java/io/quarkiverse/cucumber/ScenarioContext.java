package io.quarkiverse.cucumber;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.impl.ContextInstanceHandleImpl;

public class ScenarioContext implements InjectableContext {

    private final ConcurrentMap<Contextual<?>, ContextInstanceHandle<?>> instances = new ConcurrentHashMap<>();
    private final Lock beanLock = new ReentrantLock();

    @Override
    public void destroy() {
        for (var contextInstanceHandle : instances.values()) {
            contextInstanceHandle.destroy();
        }
        instances.clear();
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        try (var contextInstanceHandle = instances.remove(contextual)) {
            if (contextInstanceHandle != null) {
                contextInstanceHandle.destroy();
            }
        }
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ScenarioScope.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        var contextInstanceHandle = (ContextInstanceHandle<T>) instances.get(contextual);
        if (contextInstanceHandle != null) {
            return contextInstanceHandle.get();
        } else if (creationalContext != null) {
            beanLock.lock();
            try {
                T createdInstance = contextual.create(creationalContext);
                instances.put(
                        contextual,
                        new ContextInstanceHandleImpl<>(
                                (InjectableBean<T>) contextual,
                                createdInstance,
                                creationalContext));
                return createdInstance;
            } finally {
                beanLock.unlock();
            }
        } else {
            return null;
        }
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        return get(contextual, null);
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public ContextState getState() {
        return new ScenarioContextState(instances);
    }

    private record ScenarioContextState(
            Map<Contextual<?>, ContextInstanceHandle<?>> instances) implements ContextState {

        @Override
        public Map<InjectableBean<?>, Object> getContextualInstances() {
            return instances.values().stream()
                    .collect(Collectors.toMap(ContextInstanceHandle::getBean, ContextInstanceHandle::get));
        }
    }
}
