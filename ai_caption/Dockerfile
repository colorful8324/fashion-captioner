# Use an official Python runtime as a base image
FROM python:3.9-slim

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Install PyTorch first (CPU version to avoid CUDA issues)
RUN pip install --no-cache-dir torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cpu

# Copy requirements file
COPY requirements.txt .

# Remove torch from requirements.txt if it exists
RUN sed -i '/torch/d' requirements.txt

# Install remaining requirements
RUN pip install --no-cache-dir -r requirements.txt

# Copy the application files
COPY . .

# Expose the port
EXPOSE 8000

# Command to run the application
CMD ["uvicorn", "gen_cap:app", "--host", "0.0.0.0", "--port", "8000"]
