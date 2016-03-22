#!/bin/bash

CLASSPATH="target/pis-1.0.jar:lib/*"

java -Xms1200m -Xmx1700m -Dtx.useLog=false -cp $CLASSPATH org.fit.pis.tools.GroupSelector $@
