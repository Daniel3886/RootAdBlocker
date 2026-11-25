#!/data/data/com.termux/files/usr/bin/bash
# Starts the sinkhole on a rooted Android device with the android profile.
# Run it from anywhere; paths resolve against the repository root.
set -euo pipefail

cd "$(dirname "$(readlink -f "$0")")"

JAR=target/dns-sinkhole.jar
if [ ! -f "$JAR" ]; then
    echo "$JAR not found. Build it first: mvn clean package -DskipTests" >&2
    exit 1
fi

# su starts a clean environment, so pass the profile and java path explicitly.
exec su -c "cd '$PWD' && '$(command -v java)' -jar '$JAR' --spring.profiles.active=android $*"
