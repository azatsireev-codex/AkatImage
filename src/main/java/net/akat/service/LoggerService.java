package net.akat.service;

public interface LoggerService {
    void info(String message);
    void warning(String message);
    void error(String message);
    void debug(String message);
}
