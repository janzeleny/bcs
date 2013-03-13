#!/bin/bash

CLASSPATH="target/srvs-0.1.jar:lib/*:lib2/*"

#java -verbose:gc -Xms1000m -Xmx1500m -Dtx.useLog=false -cp $CLASSPATH org.fit.psc.Main $@
java -Xms1200m -Xmx1700m -Dtx.useLog=false -cp $CLASSPATH org.fit.psc.Main $@
