#!/usr/bin/env sh

if [ "$APP_LOG_VERSION" = "true" ];
then
  java -version
fi

# jdk.jshell is opened to support the exhaustive-execution

jre/bin/java \
  --add-opens jdk.jshell/jdk.jshell=ALL-UNNAMED \
  -jar app.jar \
  -XX:+UseZGC \
  -XX:+UseZGC \
  -Xmx"$APP_HEAP_LIMIT" \
  -Xms"$APP_HEAP_MINIMUM" \
  "$@"