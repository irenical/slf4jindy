package org.irenical.slf4j;

import gelf4j.logback.GelfAppender;

import org.irenical.jindy.Config;
import org.irenical.jindy.ConfigFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.spi.ContextAwareBase;

public class LoggerConfigurator extends ContextAwareBase implements Configurator {

  private static final String APPENDER_CONSOLE = "CONSOLE";
  private static final String APPENDER_GELF = "GELF";
  private static final String APPENDER_FILE = "FILE";

  private static final String LEVEL = "log.level";

  private static final String CONSOLE_ENABLED = "log.console.enabled";
  private static final String CONSOLE_PATTERN = "log.console.pattern";

  private static final String FILE_ENABLED = "log.file.enabled";
  private static final String FILE_PATTERN = "log.file.pattern";
  private static final String FILE_MAXSIZE = "log.file.maxsize";
  private static final String FILE_BACKUP_DATE_PATTERN = "log.file.backupdatepattern";
  private static final String FILE_PATH = "log.file.path";
  private static final String FILE_MAXBACKUPS = "log.file.maxbackups";

  private static final String GELF_ENABLED = "log.gelf.enabled";
  private static final String GELF_HOST = "log.gelf.host";
  private static final String GELF_PORT = "log.gelf.port";

  private static final String DEFAULT_PATTERN = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";
  private static final String DEFAULT_FILE_PATH = "./log/";
  private static final int DEFAULT_FILE_MAXBACKUPS = 5;
  private static final String DEFAULT_BACKUP_DATE_PATERN = "%d{yyyy-MM-dd}";

  private static final String EXT = ".log";

  private static final String SEP = "-";

  private static final Config CONFIG = ConfigFactory.getConfig();

  private static volatile boolean started = false;

  public LoggerConfigurator() {
  }

  @Override
  public void configure(LoggerContext loggerContext) {
    if (!started) {
      CONFIG.listen(LEVEL, this::updateLevel);
      CONFIG.listen(CONSOLE_ENABLED, this::updateConsole);
      CONFIG.listen(CONSOLE_PATTERN, this::updateConsole);
      CONFIG.listen(GELF_ENABLED, this::updateGelf);
      CONFIG.listen(GELF_HOST, this::updateGelf);
      CONFIG.listen(GELF_PORT, this::updateGelf);
      CONFIG.listen(FILE_ENABLED, this::updateFile);
      CONFIG.listen(FILE_PATTERN, this::updateFile);
      CONFIG.listen(FILE_MAXBACKUPS, this::updateFile);
      CONFIG.listen(FILE_MAXSIZE, this::updateFile);
      CONFIG.listen(FILE_PATH, this::updateFile);
      CONFIG.listen(FILE_BACKUP_DATE_PATTERN, this::updateFile);
      started = true;
    }
    loggerContext.reset();
    updateLevel();
    updateConsole();
    updateFile();
    updateGelf();
  }

  private void updateFile() {
    try {
      LoggerContext loggerContext = (LoggerContext) getContext();
      Logger logbackLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

      RollingFileAppender<ILoggingEvent> fileAppender = (RollingFileAppender<ILoggingEvent>) logbackLogger.getAppender(APPENDER_FILE);
      if (CONFIG.getBoolean(FILE_ENABLED, false)) {
        logbackLogger.detachAppender(fileAppender);

        fileAppender = new RollingFileAppender<ILoggingEvent>();
        fileAppender.setName(APPENDER_FILE);
        fileAppender.setContext(loggerContext);

        String file = CONFIG.getString(FILE_PATH, DEFAULT_FILE_PATH);
        if (!file.endsWith("/")) {
          file += "/";
        }

        fileAppender.setFile(file + CONFIG.getString("application") + EXT);

        TimeBasedRollingPolicy<ILoggingEvent> rollPolicy = new TimeBasedRollingPolicy<ILoggingEvent>();
        rollPolicy.setContext(loggerContext);
        rollPolicy.setFileNamePattern(file + CONFIG.getString("application") + SEP + CONFIG.getString(FILE_BACKUP_DATE_PATTERN, DEFAULT_BACKUP_DATE_PATERN) + EXT);
        rollPolicy.setMaxHistory(CONFIG.getInt(FILE_MAXBACKUPS, DEFAULT_FILE_MAXBACKUPS));
        rollPolicy.setParent(fileAppender);
        fileAppender.setRollingPolicy(rollPolicy);
        fileAppender.setTriggeringPolicy(rollPolicy);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern(CONFIG.getString(FILE_PATTERN, DEFAULT_PATTERN));
        fileAppender.setEncoder(encoder);

        logbackLogger.addAppender(fileAppender);
        rollPolicy.start();
        encoder.start();
        fileAppender.start();

      } else {
        logbackLogger.detachAppender(fileAppender);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void updateGelf() {
    try {
      LoggerContext loggerContext = (LoggerContext) getContext();
      Logger logbackLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

      GelfAppender<ILoggingEvent> gelfAppender = (GelfAppender<ILoggingEvent>) logbackLogger.getAppender(APPENDER_GELF);

      if (CONFIG.getBoolean(GELF_ENABLED, false)) {
        logbackLogger.detachAppender(gelfAppender);
        gelfAppender = new GelfAppender<>();
        gelfAppender.setHost(CONFIG.getMandatoryString(GELF_HOST));
        gelfAppender.setPort(CONFIG.getMandatoryInt(GELF_PORT));
        gelfAppender.setCompressedChunking(true);
        gelfAppender.setDefaultFields("{\"environment\": \"" + CONFIG.getString("application") + "\", \"cluster\": \"unknown\", \"facility\": \"unknown\", \"application\": \"" + CONFIG.getString("application") + "\"}");
        gelfAppender.setAdditionalFields("{\"level\": \"level\", \"logger\": \"loggerName\", \"thread_name\": \"threadName\", \"exception\": \"exception\", \"time_stamp\": \"timestampMs\"}");
        gelfAppender.setName(APPENDER_GELF);
        gelfAppender.setContext(loggerContext);
        gelfAppender.start();
        logbackLogger.addAppender(gelfAppender);
      } else {
        logbackLogger.detachAppender(gelfAppender);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void updateConsole() {
    LoggerContext loggerContext = (LoggerContext) getContext();
    Logger logbackLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

    ConsoleAppender<ILoggingEvent> consoleAppender = (ConsoleAppender<ILoggingEvent>) logbackLogger.getAppender(APPENDER_CONSOLE);
    if (CONFIG.getBoolean(CONSOLE_ENABLED, true)) {
      logbackLogger.detachAppender(consoleAppender);

      consoleAppender = new ConsoleAppender<ILoggingEvent>();
      consoleAppender.setContext(loggerContext);
      consoleAppender.setName(APPENDER_CONSOLE);

      PatternLayoutEncoder encoder = new PatternLayoutEncoder();
      encoder.setContext(loggerContext);
      encoder.setPattern(CONFIG.getString(CONSOLE_PATTERN, DEFAULT_PATTERN));
      consoleAppender.setEncoder(encoder);
      encoder.start();
      consoleAppender.start();

      logbackLogger.addAppender(consoleAppender);
    } else {
      logbackLogger.detachAppender(consoleAppender);
    }
  }

  private void updateLevel() {
    LoggerContext loggerContext = (LoggerContext) getContext();
    Logger logbackLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    String level = CONFIG.getString(LEVEL, "DEBUG");
    switch (level) {
    case "ERROR":
      logbackLogger.setLevel(Level.ERROR);
      break;
    case "WARN":
      logbackLogger.setLevel(Level.WARN);
      break;
    case "INFO":
      logbackLogger.setLevel(Level.INFO);
      break;
    case "DEBUG":
      logbackLogger.setLevel(Level.DEBUG);
      break;
    case "TRACE":
      logbackLogger.setLevel(Level.TRACE);
      break;
    case "ALL":
      logbackLogger.setLevel(Level.ALL);
      break;
    default:
      logbackLogger.setLevel(Level.OFF);
    }
  }

}
