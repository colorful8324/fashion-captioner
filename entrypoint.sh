#!/bin/bash

echo "Waiting for ngrok to expose public URL..."
until curl --silent --fail http://ngrok:4040/api/tunnels; do
  echo "Waiting for ngrok to be ready..."
  sleep 2
done

NGROK_URL=""
while [ -z "$NGROK_URL" ]; do
  NGROK_URL=$(curl -s http://ngrok:4040/api/tunnels | jq -r '.tunnels[] | select(.name=="command_line") | .public_url')
  [ -z "$NGROK_URL" ] && echo "ngrok tunnel not ready yet, retrying..." && sleep 2
done

# Set MinIO environment variables
export MINIO_SERVER_URL="$NGROK_URL"
export MINIO_BROWSER_REDIRECT_URL="$NGROK_URL"
export MINIO_ENDPOINT="$NGROK_URL"

echo "Starting Spring Boot app with MINIO_ENDPOINT=$MINIO_ENDPOINT"
java -jar /app/fashion-captioner.jar
