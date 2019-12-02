#!/bin/bash
if [ -z "$DOCKER_USERNAME" ]
then
	>&2 echo "DOCKER_USERNAME not provided"
	exit 22
fi

if [ -z "$DOCKER_PASSWORD" ]
then
	>&2 echo "DOCKER_PASSWORD not provided"
	exit 22
fi

if [ -z "$LOCAL_IMAGE" ]
then
	>&2 echo "LOCAL_IMAGE not specified"
	exit 22
fi

if [ -z "$DOCKER_SLUG" ]
then
	>&2 echo "Docker slug not given, example: DOCKER_SLUG=dm874/webserver"
	exit 22
fi

if [ -z "$TRAVIS_COMMIT" ]
then
	>&2 echo "TRAVIS_COMMIT not set, did you run outside of Travis CI?"
	exit 22
fi

echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
docker tag $LOCAL_IMAGE $DOCKER_SLUG:$TRAVIS_COMMIT
docker push $DOCKER_SLUG:$TRAVIS_COMMIT
