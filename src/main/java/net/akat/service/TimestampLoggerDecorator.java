package net.akat.service;

public class TimestampLoggerDecorator implements LoggerService {
    private final LoggerService wrapped;

    public TimestampLoggerDecorator(LoggerService wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void info(String message) {
        wrapped.info("[" + java.time.LocalDateTime.now() + "] " + message);
    }

    @Override
    public void warning(String message) {
        wrapped.warning("[" + java.time.LocalDateTime.now() + "] " + message);
    }

    @Override
    public void error(String message) {
        wrapped.error("[" + java.time.LocalDateTime.now() + "] " + message);
    }

    @Override
    public void debug(String message) {
        wrapped.debug("[" + java.time.LocalDateTime.now() + "] " + message);
    }
}
