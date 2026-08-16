#!/bin/sh
# Vetra Backend — Maven Wrapper Script
# Delegates to the Maven installation referenced in maven-wrapper.properties.
# Usage: ./mvnw [mvn goals]

set -e

# Resolve script directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MAVEN_PROJECTBASEDIR="$SCRIPT_DIR"

# Download Maven if not already cached
WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPS="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

if [ -f "$WRAPPER_PROPS" ]; then
  DISTRIBUTION_URL=$(grep distributionUrl "$WRAPPER_PROPS" | sed 's/.*=//')
fi

M2_HOME=${M2_HOME:-"$HOME/.m2/wrapper/dists/apache-maven-3.9.9"}

if [ -f "$M2_HOME/bin/mvn" ]; then
  exec "$M2_HOME/bin/mvn" "$@"
fi

# Fall back to system Maven if wrapper hasn't downloaded yet
if command -v mvn > /dev/null 2>&1; then
  exec mvn "$@"
fi

# Try local tmp install (used during CI bootstrap)
if [ -f "/tmp/apache-maven-3.9.9/bin/mvn" ]; then
  exec /tmp/apache-maven-3.9.9/bin/mvn "$@"
fi

echo "Error: Maven not found. Install Maven or run: brew install maven" >&2
exit 1