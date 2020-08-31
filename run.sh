#!/bin/bash

CLASSPATH="target/bcs-1.0.jar:lib/*"

#java -verbose:gc -Xms1000m -Xmx1500m -Dtx.useLog=false -cp $CLASSPATH org.fit.psc.Main $@
java -Xms1200m -Xmx1700m -Dtx.useLog=false -cp $CLASSPATH org.fit.pis.Main $@
