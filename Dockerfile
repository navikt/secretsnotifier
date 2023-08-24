FROM gcr.io/distroless/static-debian11:nonroot

COPY --chmod=0755 build/bin/native/releaseExecutable/secretsnotifier.kexe /app/secretsnotifier

CMD ["/app/secretsnotifier"]
