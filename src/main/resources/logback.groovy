import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.*
import ch.qos.logback.core.encoder.*
import ch.qos.logback.core.read.*
import ch.qos.logback.core.rolling.*
import ch.qos.logback.core.status.*
import ch.qos.logback.classic.net.*
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import static ch.qos.logback.classic.Level.*

appender("Console", ConsoleAppender) {
    append = true
    encoder(PatternLayoutEncoder) {
        pattern = "%level %logger - %msg%n"
    }
}

logger "io.netty", Level.WARN,["console"]
logger "datastore", Level.INFO,["console"]

root(DEBUG, ["Console"])