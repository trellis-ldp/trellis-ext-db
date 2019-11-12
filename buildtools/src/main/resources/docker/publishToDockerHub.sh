#!/bin/sh

IMAGE=trellisldp/trellis-database

VERSION=$(./gradlew -q getVersion)
BRANCH=$(git branch 2>/dev/null | sed -n -e 's/^\* \(.*\)/\1/p')

./gradlew assemble
cd platform/quarkus

echo $VERSION
TAG=latest
# Use the develop tag for snapshots
if [[ $VERSION == *SNAPSHOT ]]
then
    TAG=develop
fi

# Don't use latest/develop tags for maintenance branches
if [[ $BRANCH == *.x ]]
then
    docker build -f src/main/docker/Dockerfile.jvm -t $IMAGE:$VERSION .
else
    docker build -f src/main/docker/Dockerfile.jvm -t $IMAGE:$TAG -t $IMAGE:$VERSION .
fi

docker push $IMAGE