import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.*
import ch.qos.logback.core.encoder.*
import ch.qos.logback.core.read.*
import ch.qos.logback.core.rolling.*
import ch.qos.logback.core.status.*
import ch.qos.logback.classic.net.*
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import static ch.qos.logback.classic.Level.*
import static org.fusesource.jansi.Ansi.Color.*

conversionRule("highlight", util.loggingConfiguration.HighlightingCompositeConverter)

appender("Console", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        //logger{0} will shorten just to class name
        pattern = "[%d{HH:mm:ss.SSS}] %cyan([%thread]) %highlight([%level]) %magenta(%logger{5}) : %msg%n"
    }
}


logger ("io.vertx.core", WARN)
logger ("io.netty", WARN)
logger ("ch.qos.logback.classic", WARN)

logger "datastore", DEBUG
//logger "datastore", Level.DEBUG, ["Console"]  //causes it print it twice!



root(DEBUG, ["Console"])