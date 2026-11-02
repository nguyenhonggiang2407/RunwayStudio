#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
if [ ! -f target/runway-store-2.0.0.jar ]; then ./mvnw clean verify; fi
exec java -jar target/runway-store-2.0.0.jar
