package org.irenical.slf4j;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.joran.GenericConfigurator;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.irenical.jindy.Config;
import org.irenical.jindy.ConfigFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class LoggerConfigurator extends GenericConfigurator implements Configurator {

  private static final String APPENDER_CONSOLE = "CONSOLE";
  private static final String APPENDER_FILE = "FILE";

  private static final String LEVEL = "log.level";

  private static final String CONSOLE_ENABLED = "log.console.enabled";
  private static final String CONSOLE_PATTERN = "log.console.pattern";

  private static final String FILE_ENABLED = "log.file.enabled";
  private static final String FILE_PATTERN = "log.file.pattern";
  private static final String FILE_BACKUP_DATE_PATTERN = "log.file.backupdatepattern";
  private static final String FILE_PATH = "log.file.path";
  private static final String FILE_MAXBACKUPS = "log.file.maxbackups";

  private static final String DEFAULT_PATTERN = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";
  private static final String DEFAULT_FILE_PATH = "./log/";
  private static final int DEFAULT_FILE_MAXBACKUPS = 5;
  private static final String DEFAULT_BACKUP_DATE_PATERN = "%d{yyyy-MM-dd}";

  private static final String LOGGING_LEVEL_CHANGE_PROPAGATOR_ENABLED = "logging.levelchangepropagator.enabled";

  private static final String EXT = ".log";

  private static final String SEP = "-";

  private static volatile boolean started = false;

  private final Config CONFIG = ConfigFactory.getConfig();

  public LoggerConfigurator() {
  }

  @Override
  protected void addInstanceRules(RuleStore rs) {

  }

  @Override
  protected void addImplicitRules(Interpreter interpreter) {

  }

  protected void initListeners() {
    CONFIG.listen(LEVEL, this::updateLevel);
    CONFIG.listen(CONSOLE_ENABLED, this::updateConsole);
    CONFIG.listen(CONSOLE_PATTERN, this::updateConsole);
    CONFIG.listen(FILE_ENABLED, this::updateFile);
    CONFIG.listen(FILE_PATTERN, this::updateFile);
    CONFIG.listen(FILE_MAXBACKUPS, this::updateFile);
    CONFIG.listen(FILE_PATH, this::updateFile);
    CONFIG.listen(FILE_BACKUP_DATE_PATTERN, this::updateFile);
  }

  @Override
  public void configure(LoggerContext loggerContext) {
    if (!started) {
      initListeners();
      started = true;
    }
    loggerContext.reset();
    installJulBridge();
    updateLevel();
    updateConsole();
    updateFile();
  }

  private void installJulBridge() {
    LoggerContext loggerContext = (LoggerContext) getContext();

    if (!SLF4JBridgeHandler.isInstalled()) {
      SLF4JBridgeHandler.removeHandlersForRootLogger();
      SLF4JBridgeHandler.install();
    }
    String got = System.getProperty(LOGGING_LEVEL_CHANGE_PROPAGATOR_ENABLED);
    if (got == null || got.equalsIgnoreCase("true")) {
      LevelChangePropagator julLevelChanger = new LevelChangePropagator();
      julLevelChanger.setContext(loggerContext);
      julLevelChanger.setResetJUL(true);
      julLevelChanger.start();
      loggerContext.addListener(julLevelChanger);
    }
  }

  private void updateFile() {
    try {
      LoggerContext loggerContext = (LoggerContext) getContext();
      Logger logbackLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

      RollingFileAppender<ILoggingEvent> fileAppender = (RollingFileAppender<ILoggingEvent>) logbackLogger.getAppender(APPENDER_FILE);
      if (CONFIG.getBoolean(FILE_ENABLED, false)) {
        logbackLogger.detachAppender(fileAppender);

        fileAppender = new RollingFileAppender<>();
        fileAppender.setName(APPENDER_FILE);
        fileAppender.setContext(loggerContext);

        String file = CONFIG.getString(FILE_PATH, DEFAULT_FILE_PATH);
        if (!file.endsWith("/")) {
          file += "/";
        }

        fileAppender.setFile(file + CONFIG.getString("application") + EXT);

        TimeBasedRollingPolicy<ILoggingEvent> rollPolicy = new TimeBasedRollingPolicy<>();
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

  private void updateConsole() {
    LoggerContext loggerContext = (LoggerContext) getContext();
    Logger logbackLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

    ConsoleAppender<ILoggingEvent> consoleAppender = (ConsoleAppender<ILoggingEvent>) logbackLogger.getAppender(APPENDER_CONSOLE);
    if (CONFIG.getBoolean(CONSOLE_ENABLED, true)) {
      logbackLogger.detachAppender(consoleAppender);

      consoleAppender = new ConsoleAppender<>();
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
