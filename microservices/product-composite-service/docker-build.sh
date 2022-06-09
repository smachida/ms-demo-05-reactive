#!/bin/bash
docker build -t ms-demo-05-reactive-product-composite-service --platform linux/amd64 .
docker images | grep product-composite-service
