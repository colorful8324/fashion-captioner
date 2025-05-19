#!/bin/sh
set -e

# Wait for MinIO to be ready
sleep 10

# Create the bucket 'fashion-captioner'
mc alias set minio http://minio:9000 minioadmin minioadmin
mc mb minio/fashion-captioner

# Set bucket policy to allow public read access
mc policy set download minio/fashion-captioner
