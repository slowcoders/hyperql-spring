#!/usr/bin/env bash

db=${1:-"postgres"}
docker-compose -f ./db/docker-compose.yml up -d $db

#pushd ./tutorial+test
#npm install
#npm run build
#popd

pushd sample-app
./gradlew bootRun --console=plain --args="--spring.profiles.active=$db"
popd

