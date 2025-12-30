#!/bin/env bash

CONFIG_NAME="prometheus_config"
CONFIG_FILE="services/monitor-service/prometheus.yml"

# Remove old config if it exists
if docker config inspect "$CONFIG_NAME" >/dev/null 2>&1; then
  docker config rm "$CONFIG_NAME"
fi

# Create new config from repo file
docker config create "$CONFIG_NAME" "$CONFIG_FILE"