# SLF4Jindy

A jar containing an slf4j binding (logback), configured dynamically using jindy and with a bunch of slf4j bridging libs.

### Usage
Add SLF4Jindy dependency to your project

```xml
<dependency>
    <groupId>org.irenical.slf4j</groupId>
    <artifactId>slf4jindy</artifactId>
    <version>1.1.0</version>
</dependency>
```

...along with a Jindy binding, such as:
```xml
<dependency>
    <groupId>org.irenical.jindy</groupId>
    <artifactId>jindy-commons-impl</artifactId>
    <version>1.1.4</version>
</dependency>
```

You can learn more about Jindy at https://github.com/irenical/jindy

SLF4Jindy will try to read the following properties. The values bellow are the default ones.
```properties
log.level=DEBUG

log.console.enabled=true
log.console.pattern=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n

log.file.enabled=false
log.file.pattern=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
log.file.backupdatepattern=%d{yyyy-MM-dd}
log.file.path=./log/
log.file.maxbackups=5
```
