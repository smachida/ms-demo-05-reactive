#!/bin/bash
docker build -t ms-demo-05-reactive-review-service --platform linux/amd64 .
docker images | grep review-service
