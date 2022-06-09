#!/bin/bash
docker build -t ms-demo-05-reactive-recommendation-service .
docker images | grep recommendation-service
