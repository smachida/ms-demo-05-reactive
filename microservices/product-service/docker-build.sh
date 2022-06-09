#!/bin/bash
docker build -t ms-demo-05-reactive-product-service --platform linux/amd64 .
docker images | grep product-service
