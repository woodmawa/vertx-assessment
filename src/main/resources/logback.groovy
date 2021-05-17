import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.*
import ch.qos.logback.core.encoder.*
import ch.qos.logback.core.read.*
import ch.qos.logback.core.rolling.*
import ch.qos.logback.core.status.*
import ch.qos.logback.classic.net.*
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import org.fusesource.jansi.internal.WindowsSupport
import org.fusesource.jansi.internal.Kernel32
import org.slf4j.LoggerFactory

import static ch.qos.logback.classic.Level.*
import static org.fusesource.jansi.Ansi.Color.*

import static org.fusesource.jansi.internal.Kernel32.GetStdHandle
import static org.fusesource.jansi.internal.Kernel32.STD_OUTPUT_HANDLE

def appenderList = []

conversionRule("highlight", util.loggingConfiguration.HighlightingCompositeConverter)

final int VIRTUAL_TERMINAL_PROCESSING = 0x0004
long console = GetStdHandle(STD_OUTPUT_HANDLE)
int[] mode = new int[1]
def ansiEnabled = Kernel32.GetConsoleMode(console, mode)
//println "appended is ansi enabled $ansiEnabled"



//colour coded for display
appender("AnsiConsole", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        //logger{5} will shorten just to class name to x.y.z.ClassName form
        pattern = "[%d{HH:mm:ss.SSS}] %cyan([%thread]) %highlight([%level]) %magenta(%logger{5}) : %msg%n"
    }
}

appender("PlainConsole", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        //logger{5} will shorten just to class name to x.y.z.ClassName form
        pattern = "[%d{HH:mm:ss.SSS}] [%thread] [%level] %logger{5} : %msg%n"
    }
}

logger ("io.vertx.core", WARN)
logger ("io.netty", WARN)
logger ("ch.qos.logback.classic", WARN)
logger ("com.hazelcast", WARN)

logger "datastore", DEBUG
//logger "datastore", Level.DEBUG, ["Console"]  //causes it print it twice!



root(DEBUG, ["AnsiConsole"])
