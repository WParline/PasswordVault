#!/bin/sh
APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
APP_HOME=`dirname "$0"`
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
JAVA_EXE=java
if [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_EXE="$JAVA_HOME/bin/java"
fi
exec "$JAVA_EXE" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
