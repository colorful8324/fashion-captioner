#!/bin/sh
set -e

sleep 10

mc alias set minio http://minio:9000 minioadmin minioadmin
mc mb minio/fashion-captioner

mc policy set download minio/fashion-captioner
