#!/usr/bin/env sh

if [ "$APP_LOG_VERSION" = "true" ];
then
  java -version
fi

# jdk.jshell is opened to support the exhaustive-execution
# feature of EvaluationEngines on the server. It can be removed if
# the server itself does not evaluate snippets.

jre/bin/java -jar app.jar \
  -XX:+UseZGC \
  -XX:+UseZGC \
  -Xmx"$APP_HEAP_LIMIT" \
  -Xms"$APP_HEAP_MINIMUM" \
  --add-opens jdk.jshell/jdk.jshell=ALL-UNNAMED \
  "$@"