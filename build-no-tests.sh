#!/bin/sh
cd src
mvn clean install -Pwebeoc,release -Dmaven.test.skip=true
