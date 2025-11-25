#!/bin/sh

# Rust legacy system interface on port 8080
/app/legacy-system &

# Java service
exec java -jar /app/app.jar