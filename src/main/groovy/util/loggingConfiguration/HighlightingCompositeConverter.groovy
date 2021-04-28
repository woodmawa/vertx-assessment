package util.loggingConfiguration

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase
import static ch.qos.logback.core.pattern.color.ANSIConstants.*;

/**
 * implementation of highlight colour coding for log levels
 *
 *  requires jansi library on Windows classpath
 *
 *  takes the log level and returns the colour code to render on Ansi enabled appender output 
 */
class HighlightingCompositeConverter extends ForegroundCompositeConverterBase<ILoggingEvent> {

    @Override
    protected String getForegroundColorCode(ILoggingEvent event) {
        Level level = event.getLevel()
        switch (level.toInt()) {
            case Level.ERROR_INT:
                return BOLD + RED_FG
            case Level.WARN_INT:
                return YELLOW_FG
            case Level.INFO_INT:
                return CYAN_FG
            case Level.DEBUG_INT:
            case Level.TRACE_INT:
                return BLUE_FG

            default:
                return DEFAULT_FG
        }
    }
}
