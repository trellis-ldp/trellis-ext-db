#!/bin/bash

IMAGE=$1

if [ -z "$1" ]; then
    echo "Please include an image name parameter (e.g. trellisldp/trellis-database)"
    exit 1
else
    echo "Building docker image $IMAGE"
fi

VERSION=$(./gradlew -q getVersion)
BRANCH=$(git branch 2>/dev/null | sed -n -e 's/^\* \(.*\)/\1/p')


TAG=latest
# Use the develop tag for snapshots
if [[ $VERSION == *SNAPSHOT* ]]; then
    TAG=develop
fi

if [[ $IMAGE == "trellisldp/trellis-ext-db" ]]; then
    # Dropwizard-based image
    cd platform/dropwizard

    # Don't use latest/develop tags for maintenance branches
    if [[ $BRANCH == *.x ]]; then
        docker build -f src/main/docker/Dockerfile -t "$IMAGE:$VERSION" .
    else
        docker build -f src/main/docker/Dockerfile -t "$IMAGE:$TAG" -t "$IMAGE:$VERSION" .
    fi

    docker push "$IMAGE"

else
    # Quarkus-based images
    cd platform/quarkus

    if [[ -f "build/trellis-quarkus-${VERSION}-runner.jar" && -d "build/lib" ]]
    then
        # Don't use latest/develop tags for maintenance branches
        if [[ $BRANCH == *.x ]]; then
            docker build -f src/main/docker/Dockerfile.jvm -t "$IMAGE:$VERSION" .
        else
            docker build -f src/main/docker/Dockerfile.jvm -t "$IMAGE:$TAG" -t "$IMAGE:$VERSION" .
        fi

        docker push "$IMAGE"
    else
        echo "Build artifacts not present. Please run 'gradle assemble' first"
        exit 1
    fi
fi


