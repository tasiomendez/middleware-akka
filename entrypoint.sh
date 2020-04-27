#! /bin/bash

if [ -z "${JAVA_JAR_FILENAME}" ]; then
  echo Looking for project metadata...
  JAVA_PROJECT_NAME=$(mvn exec:exec -Dexec.executable=echo -Dexec.args='${project.name}' -q)
  JAVA_PROJECT_VERSION=$(mvn exec:exec -Dexec.executable=echo -Dexec.args='${project.version}' -q)
  export JAVA_JAR_FILENAME=$JAVA_PROJECT_NAME-$JAVA_PROJECT_VERSION.jar
  unset JAVA_PROJECT_NAME JAVA_PROJECT_VERSION
fi

# Run java jarfile
echo '>>' Jar file: $JAVA_JAR_FILENAME
java -jar target/$JAVA_JAR_FILENAME
