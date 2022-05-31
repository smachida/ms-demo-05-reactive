#!/bin/bash
curl -s localhost:8080/actuator/health | jq 
