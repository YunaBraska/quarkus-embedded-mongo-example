#!/usr/bin/env bash
set -e
APP_NAME="quarkus/example_app"
DIR="$(dirname "$0")"
"${DIR}/mvnw" clean compile -DskipTests=true
"${DIR}/mvnw" package -DskipTests=true -Pnative -Dquarkus.native.container-build=true
docker build -f "${DIR}/src/main/docker/Dockerfile.native" -t "${APP_NAME}" .
docker run -i --rm -p 8080:8080 ${APP_NAME}