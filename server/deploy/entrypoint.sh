#!/usr/bin/env sh

if [ "$APP_LOG_VERSION" = "true" ];
then
  java -version
fi

jre/bin/java -jar app.jar \
  -XX:+UseZGC \
  -XX:+UseZGC \
  -Xmx"$APP_HEAP_LIMIT" \
  -Xms"$APP_HEAP_MINIMUM" \
  "$@"

