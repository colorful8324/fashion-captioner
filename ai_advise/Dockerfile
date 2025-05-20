FROM python:3.9-slim

WORKDIR /app

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

COPY requirements.txt /app/
RUN pip install --no-cache-dir -r requirements.txt

COPY . /app/

EXPOSE 5000

CMD ["uvicorn", "advise:app", "--host", "0.0.0.0", "--port", "5000"]
