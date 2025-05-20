# Use an official Python runtime as a base image
FROM python:3.9-slim

# Set working directory
WORKDIR /app

# Install system dependencies with retry mechanism
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    default-libmysqlclient-dev \
    build-essential \
    && rm -rf /var/lib/apt/lists/* \
    || (apt-get update && \
        apt-get install -y --no-install-recommends \
        default-libmysqlclient-dev \
        build-essential \
        && rm -rf /var/lib/apt/lists/*)

# Install Python dependencies
COPY requirements.txt /app/
RUN pip install --no-cache-dir -r requirements.txt

# Copy the application files into the container
COPY . /app/

# Expose the port the app runs on
EXPOSE 5000

# Command to run the application
CMD ["uvicorn", "advise:app", "--host", "0.0.0.0", "--port", "5000"]
