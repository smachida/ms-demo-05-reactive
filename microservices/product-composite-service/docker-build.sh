#!/bin/bash
docker build -t ms-demo-05-reactive-product-composite-service .
docker images | grep product-composite-service
