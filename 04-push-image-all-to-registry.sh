#!/bin/bash

HARBOR_HOST=172.16.140.11

echo "pushing the images to the registry: $HARBOR_HOST"
docker login $HARBOR_HOST

docker tag ms-demo-05-reactive-product-service $HARBOR_HOST/ms-demo/ms-demo-05-reactive-product-service
docker push $HARBOR_HOST/ms-demo/ms-demo-05-reactive-product-service
docker tag ms-demo-05-reactive-recommendation-service $HARBOR_HOST/ms-demo/ms-demo-05-reactive-recommendation-service
docker push $HARBOR_HOST/ms-demo/ms-demo-05-reactive-recommendation-service
docker tag ms-demo-05-reactive-review-service $HARBOR_HOST/ms-demo/ms-demo-05-reactive-review-service
docker push $HARBOR_HOST/ms-demo/ms-demo-05-reactive-review-service
docker tag ms-demo-05-reactive-product-composite-service $HARBOR_HOST/ms-demo/ms-demo-05-reactive-product-composite-service
docker push $HARBOR_HOST/ms-demo/ms-demo-05-reactive-product-composite-service
