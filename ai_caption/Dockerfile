# Use an official Python runtime as a base image
FROM python:3.9-slim

# Set working directory
WORKDIR /app

# Install dependencies
COPY requirements.txt /app/
RUN pip install --no-cache-dir -r requirements.txt

# Copy the application files into the container
COPY . /app/

# Expose the port the app runs on
EXPOSE 8000

# Command to run the application
CMD ["uvicorn", "gen_cap:app", "--host", "0.0.0.0", "--port", "8000"]
