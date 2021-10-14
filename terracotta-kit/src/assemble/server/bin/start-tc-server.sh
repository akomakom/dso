#!/bin/sh

# 
# The contents of this file are subject to the Terracotta Public License Version
# 2.0 (the "License"); You may not use this file except in compliance with the
# License. You may obtain a copy of the License at 
# 
#      http://terracotta.org/legal/terracotta-public-license.
# 
# Software distributed under the License is distributed on an "AS IS" basis,
# WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
# the specific language governing rights and limitations under the License.
# 
# The Covered Software is Terracotta Platform.
# 
# The Initial Developer of the Covered Software is 
#     Terracotta, Inc., a Software AG company
#

case "$1" in
  "--help"|"-h"|"-?")
    echo "Syntax: $0 [-f /path/to/tc-config.xml] [-n server_name]"
    echo
    echo "-f : start the server with your own Terracotta configuration instead of the default one"
    echo "-n : specify which server you want to start when you have more than one servers configured"
    exit
    ;;
esac

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

THIS_DIR=`dirname $0`
TC_INSTALL_DIR=`cd $THIS_DIR;pwd`/../..
JAVA_MEMORY_OPTS=${JAVA_MEMORY_OPTS:-"-Xms2g -Xmx2g"}

if [ -r "$TC_INSTALL_DIR"/server/bin/setenv.sh ] ; then
  . "$TC_INSTALL_DIR"/server/bin/setenv.sh
fi

if test \! -d "${JAVA_HOME}"; then
  echo "$0: the JAVA_HOME environment variable is not defined correctly"
  exit 2
fi

# For Cygwin, convert paths to Windows before invoking java
if $cygwin; then
  [ -n "$TC_INSTALL_DIR" ] && TC_INSTALL_DIR=`cygpath -m "$TC_INSTALL_DIR"`
fi

for JAVA_COMMAND in \
"\"${JAVA_HOME}/bin/java\" -d64 -server -XX:MaxDirectMemorySize=9223372036854775807" \
"\"${JAVA_HOME}/bin/java\" -server -XX:MaxDirectMemorySize=9223372036854775807" \
"\"${JAVA_HOME}/bin/java\" -d64 -client  -XX:MaxDirectMemorySize=9223372036854775807" \
"\"${JAVA_HOME}/bin/java\" -client -XX:MaxDirectMemorySize=9223372036854775807" \
"\"${JAVA_HOME}/bin/java\" -XX:MaxDirectMemorySize=9223372036854775807"
do
  eval ${JAVA_COMMAND} -version > /dev/null 2>&1
  if test "$?" = "0" ; then break; fi
done

#rmi.dgc.server.gcInterval is set an year to avoid system gc in case authentication is enabled
#users may change it accordingly
args="$@"
start=true
while "$start"
do
eval ${JAVA_COMMAND} ${JAVA_MEMORY_OPTS} -XX:+HeapDumpOnOutOfMemoryError \
   -Dcom.sun.management.jmxremote \
   -Dtc.install-root="${TC_INSTALL_DIR}" \
   -Dsun.rmi.dgc.server.gcInterval=31536000000\
   ${JAVA_OPTS} \
   -cp "${TC_INSTALL_DIR}/server/lib/tc.jar" \
   com.tc.server.TCServerMain $args
 exitValue=$?
 start=false;

 if test "$exitValue" = "11"; then
   start=true;
   mod_args=''
   # The --active flag needs to be removed from the startup options when a server gets auto-restarted.
   # When a server node is auto-restarted, the intention is to make it join the cluster as a passive.
   # So in that case it doesn't make sense to start the server with the active flag.
   for var in $args; do
     if [ '--active' != "$var" ] && [ '-a' != "$var" ]; then
       mod_args="$mod_args $var";
     fi
   done
   args="$mod_args"
   echo "start-tc-server: Restarting the server..."
 else
   exit $exitValue
 fi
done
