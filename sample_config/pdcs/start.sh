#!/bin/sh

###################
# to configure 
APP_PATH=$HOME/workspace/hazelcast-embedded-springboot/target/hazelcast-embedded-springboot-0.1.jar
#CLASSPATH="<path-to-dependencies>"
#input license here or configure in environment variables 
HZ_LICENSEKEY="${HZ_LICENSEKEY}"
HZ_CONFIG_FILE="$HOME/workspace/hazelcast-embedded-springboot/sample_config/pdcs/hazelcast.yaml"
LOG_LOCATION="$HOME/log/hazelcast"
LOG_JVM="$LOG_LOCATION/jvm"
LOG_DIAGNOSTIC="$LOG_LOCATION/diagnostic"
MIN_HEAP_SIZE=512M 
MAX_HEAP_SIZE=512M
###################
JMX_OPTS="-Xms${MIN_HEAP_SIZE} -Xmx${MAX_HEAP_SIZE} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOG_JVM} -XX:ErrorFile=${LOG_JVM}/hs_err_pid%p.log -Xlog:gc*,gc+age=trace,safepoint:file=${LOG_JVM}/gc.log:utctime,pid,tags:filecount=32,filesize=64m"
JAVA_OPTS="${JMX_OPTS} -Dhazelcast.diagnostics.enabled=true -Dhazelcast.diagnostics.directory=${LOG_DIAGNOSTIC} -Dhazelcast.enterprise.license.key=${HZ_LICENSEKEY}"
JAVA_OPTS="$JAVA_OPTS -Dhazelcast.config=${HZ_CONFIG_FILE}"

echo "########################################"
#echo "# JAVA=$JAVA"
echo "# JAVA_OPTS=${JAVA_OPTS}"
echo "# CLASSPATH=$CLASSPATH"
echo "########################################"


java $JAVA_OPTS -jar $APP_PATH

