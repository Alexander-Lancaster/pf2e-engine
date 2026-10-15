#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p target/classes target/test-classes
find src/main/java -name '*.java' > target/main-sources.txt
find src/test/java -name '*.java' > target/test-sources.txt
javac --release 21 -Xlint:all -Werror -d target/classes @target/main-sources.txt
javac --release 21 -Xlint:all -Werror -cp target/classes -d target/test-classes @target/test-sources.txt
java -cp target/classes:target/test-classes dev.ttrpg.pf2e.ModelTest
java -cp target/classes:target/test-classes dev.ttrpg.pf2e.EncounterTest
