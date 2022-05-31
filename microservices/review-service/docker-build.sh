#!/bin/bash
docker build -t ms-demo-05-reactive-review-service .
docker images | grep review-service
