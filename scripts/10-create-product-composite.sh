#!/bin/bash
export body='{"productId": 100, "name": "product name A", "weight": 300, "recommendations": [{"recommendationId":1, "author": "author 1", "rate": 1, "content": "content 1"}], "reviews": [{"reviewId": 1, "author": "author 1", "subject": "subject 1", "content": "content 1"}]}'
curl -X POST localhost:8080/product-composite -H "Content-Type: application/json" --data "$body"
