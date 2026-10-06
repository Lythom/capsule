package capsule.gametest;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the warnings and errors logged until it is closed.
 */
public class LogCapture extends AbstractAppender implements AutoCloseable {
    private final List<String> messages = new ArrayList<>();

    private LogCapture() {
        super("CapsuleGameTestLogCapture", null, null, true, Property.EMPTY_ARRAY);
    }

    public static LogCapture open() {
        LogCapture capture = new LogCapture();
        capture.start();
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.getConfiguration().getRootLogger().addAppender(capture, Level.WARN, null);
        context.updateLoggers();
        return capture;
    }

    @Override
    public synchronized void append(LogEvent event) {
        messages.add(event.getMessage().getFormattedMessage());
    }

    public synchronized List<String> messages() {
        return List.copyOf(messages);
    }

    public List<String> containing(String text) {
        return messages().stream().filter(m -> m.contains(text)).toList();
    }

    @Override
    public void close() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.getConfiguration().getRootLogger().removeAppender(getName());
        context.updateLoggers();
        stop();
    }
}
