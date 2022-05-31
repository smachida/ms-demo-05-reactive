#!/bin/bash
curl http://localhost:8080/product-composite/$1 -s | jq
