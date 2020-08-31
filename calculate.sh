#!/bin/bash

BASENAME=`dirname $0`
CLASSPATH="$BASENAME/target/bcs-1.0.jar:$BASENAME/lib/*"

java -Xms1200m -Xmx1700m -Dtx.useLog=false -cp $CLASSPATH org.fit.pis.tools.PrecisionCounter $@
