package net.akat;

import java.util.HashMap;
import java.util.Map;

public class ServiceLocator {
    private final Map<Class<?>, Object> services = new HashMap<>();

    public <T> void registerService(Class<T> serviceClass, T service) {
        services.put(serviceClass, service);
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) {
        return (T) services.get(serviceClass);
    }
}
