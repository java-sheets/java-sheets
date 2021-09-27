<div align="center" style="text-align:center">
  <img src="/assets/logo.svg" width="150" alt="JSheets Logo">
</div>

# JSheets

Jsheet lets you create and share Java snippets, ranging from single expressions
to complex classes, methods and even Markdown comments.

<div align="center" style="text-align:center">
  <img src="/assets/screenshots/small-light.png" alt="Small">
</div>

## Deployment & Configuration

### Deploy Via Docker

The best way to deploy **JSheets** is using [Docker](https://docs.docker.com/).

The **official image** is `ehenoma/jsheets`.

Run the following for a minimal deployment:

`docker run -d -p 8080:8080 --name jsheets ehenoma/jsheets`

A full installation can be deployed using
[Docker Compose](https://docs.docker.com/compose/)

```yml
version: '3.7'
services:
  server:
    image: ehenoma/jsheets:latest
    environment:
      JSHEETS_SERVER_PORT: 8080
      JSHEETS_MONGODB_URI: mongodb://root:root@document-store/jsheets
    ports:
      - "8080:8080"
    networks:
      - database
  document-store:
    container_name: document-store
    image: mongo:latest
    environment:
      MONGO_INITDB_ROOT_USERNAME: root
      MONGO_INITDB_ROOT_PASSWORD: root
    networks:
      - database

networks:
  database:
```

### Environment Configuration

The application is configured using environment variables.

| Field | Default | Description |
|-------|---------|-------------|
| `JSHEETS_SERVER_PORT` | `8080` | Port that the Server listens on |
| `JSHEETS_MONGODB_URI` | - | MongoDB [Connection String](https://docs.mongodb.com/manual/reference/connection-string/) |
| `JSHEETS_SERVER_CACHE_STATIC_FILES` | `false` | If enabled, the server caches static files in memory |
