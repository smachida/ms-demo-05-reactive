#!/bin/bash
docker build -t ms-demo-05-reactive-product-service .
docker images | grep product-service
