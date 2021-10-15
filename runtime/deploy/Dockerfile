FROM openjdk:17-alpine AS link-java

MAINTAINER "merlinosayimwen@gmail.com"

ARG APP_HEAP_LIMIT=1G
ENV APP_HEAP_LIMIT=$APP_HEAP_LIMIT

ARG APP_HEAP_MINIMUM=512m
ENV APP_HEAP_MINIMUM=$APP_HEAP_MINIMUM

ARG APP_LOG_VERSION=false
ENV APP_LOG_VERSION=$APP_LOG_VERSION

WORKDIR /usr/src/build

# Context is parent directory
COPY ./runtime/build/libs libs

RUN mv ./libs/app.jar app.jar

RUN jdeps --ignore-missing-deps -q --multi-release 17  \
    --print-module-deps \
    --class-path libs/* \
    app.jar > deps.info

# link using zip compression
RUN jlink --verbose  \
    --compress 2  \
    --strip-java-debug-attributes  \
    --no-header-files  \
    --no-man-pages  \
    --output jre  \
    --add-modules $(cat deps.info)

FROM alpine:latest
WORKDIR /app

COPY --from=link-java /usr/src/build/jre jre
COPY --from=link-java /usr/src/build/libs/* libs/
COPY --from=link-java /usr/src/build/app.jar app.jar
ADD /runtime/deploy/entrypoint.sh entrypoint.sh
ADD /website/build static/
RUN chmod +x entrypoint.sh

EXPOSE 8080

ENTRYPOINT ./entrypoint.sh