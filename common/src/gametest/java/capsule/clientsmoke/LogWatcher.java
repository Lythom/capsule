package capsule.clientsmoke;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Collects the log events showing a capsule problem: missing models or textures of the capsule namespace, warnings of
 * capsule loggers, and exceptions thrown through capsule code.
 */
class LogWatcher extends AbstractAppender {
    private static final Pattern MISSING_RESOURCE = Pattern.compile("(?i)(model|texture|blockstate|sprite|variant).*capsule:|capsule:.*(model|texture|missing)");

    final List<String> problems = new ArrayList<>();

    private LogWatcher() {
        super("CapsuleClientSmoke", null, null, true, Property.EMPTY_ARRAY);
    }

    static LogWatcher install() {
        LogWatcher watcher = new LogWatcher();
        watcher.start();
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.getConfiguration().getRootLogger().addAppender(watcher, Level.WARN, null);
        context.updateLoggers();
        return watcher;
    }

    @Override
    public void append(LogEvent event) {
        if (!event.getLevel().isMoreSpecificThan(Level.WARN)) return;
        String message = event.getMessage().getFormattedMessage();
        String logger = event.getLoggerName() == null ? "" : event.getLoggerName();
        boolean capsuleLogger = logger.startsWith("capsule.") && !logger.startsWith("capsule.clientsmoke");
        if (capsuleLogger || MISSING_RESOURCE.matcher(message).find() || throwsThroughCapsule(event.getThrown())) {
            synchronized (problems) {
                problems.add("[" + event.getLevel() + "] " + logger + ": " + message
                        + (event.getThrown() == null ? "" : " " + event.getThrown()));
            }
        }
    }

    private static boolean throwsThroughCapsule(Throwable thrown) {
        for (Throwable t = thrown; t != null; t = t.getCause()) {
            if (Arrays.stream(t.getStackTrace()).anyMatch(f -> f.getClassName().startsWith("capsule.") && !f.getClassName().startsWith("capsule.clientsmoke"))) {
                return true;
            }
        }
        return false;
    }
}
