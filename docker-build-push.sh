#!/bin/bash

# Docker Hub credentials - use environment variables or set them here
# For security, use environment variables: export DOCKER_USERNAME, DOCKER_PASSWORD, DOCKER_EMAIL
DOCKER_USERNAME="${DOCKER_USERNAME:-asadbekabdinazarov}"
DOCKER_PASSWORD="${DOCKER_PASSWORD}"
DOCKER_EMAIL="${DOCKER_EMAIL:-a.abdinazarov@student.pdp.university}"
IMAGE_NAME="devops-assignment"
IMAGE_TAG="latest"
FULL_IMAGE_NAME="${DOCKER_USERNAME}/${IMAGE_NAME}:${IMAGE_TAG}"

# Check if password is set
if [ -z "$DOCKER_PASSWORD" ]; then
    echo "Error: DOCKER_PASSWORD environment variable is not set!"
    echo "Please set it using: export DOCKER_PASSWORD=your_password"
    exit 1
fi

echo "Building Docker image: ${FULL_IMAGE_NAME}"

# Build the Docker image
docker build -t ${FULL_IMAGE_NAME} .

if [ $? -ne 0 ]; then
    echo "Docker build failed!"
    exit 1
fi

echo "Logging in to Docker Hub..."
echo ${DOCKER_PASSWORD} | docker login -u ${DOCKER_USERNAME} --password-stdin

if [ $? -ne 0 ]; then
    echo "Docker login failed!"
    exit 1
fi

echo "Pushing image to Docker Hub: ${FULL_IMAGE_NAME}"
docker push ${FULL_IMAGE_NAME}

if [ $? -ne 0 ]; then
    echo "Docker push failed!"
    exit 1
fi

echo "Successfully built and pushed ${FULL_IMAGE_NAME}"
