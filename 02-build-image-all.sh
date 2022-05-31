#!/bin/bash

cd microservices

echo "Building product-composite-service image ..."
cd product-composite-service
./docker-build.sh

echo "Building product-service image ..."
cd ../product-service
./docker-build.sh

echo "Building recommendation-service image ..."
cd ../recommendation-service
./docker-build.sh

echo "Building review-service image ..."
cd ../review-service
./docker-build.sh

echo " ---- image build done. --- "
docker images | grep ms-demo-05-reactive
