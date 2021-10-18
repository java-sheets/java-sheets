#!/usr/bin/env bash
docker-compose -f monitoring.yml -f main.yml "$@"