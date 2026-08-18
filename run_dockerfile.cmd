docker build -t orbytum-api .
docker run --env-file .env -p 8080:8080 orbytum-api