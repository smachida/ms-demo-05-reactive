#!/bin/bash
docker build -t ms-demo-05-reactive-recommendation-service --platform linux/amd64 .
docker images | grep recommendation-service
