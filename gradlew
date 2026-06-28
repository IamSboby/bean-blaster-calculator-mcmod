#!/usr/bin/env sh
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P) || exit 1
cd "$APP_HOME" || exit 1
JAVA_EXE="${JAVA_HOME:+$JAVA_HOME/bin/}java"
exec "$JAVA_EXE" -Xmx256m -Xms64m -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
