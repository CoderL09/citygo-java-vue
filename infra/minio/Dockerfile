# Local development only. Source: official minio/minio, AGPL-3.0.
# RELEASE.2025-10-15T17-29-55Z resolves to this fixed source commit.
FROM golang:1.24.8-bookworm@sha256:4ed690d6649d63c312b99a6120025ec79ce3b542968a37da53d6236c7c61a848 AS build
ENV CGO_ENABLED=0 GOMAXPROCS=2
RUN --mount=type=cache,target=/go/pkg/mod --mount=type=cache,target=/root/.cache/go-build     go install -p=2 -ldflags="-s -w" github.com/minio/minio@9e49d5e7a648f00e26f2246f4dc28e6b07f8c84a
RUN go version -m /go/bin/minio
FROM alpine:3.22@sha256:5291449c3df73caf6ed85e649dec1b9e818b39a5d8c871e97afc13e9cd5e8fa8
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt
COPY --from=build /go/bin/minio /usr/local/bin/minio
LABEL org.opencontainers.image.source="https://github.com/minio/minio"       org.opencontainers.image.revision="9e49d5e7a648f00e26f2246f4dc28e6b07f8c84a"       org.opencontainers.image.licenses="AGPL-3.0"
EXPOSE 9000 9001
ENTRYPOINT ["/usr/local/bin/minio"]
