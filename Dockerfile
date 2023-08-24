FROM gcr.io/distroless/static-debian11

COPY --chmod=0744 build/bin/native/releaseExecutable/secretsnotifier.kexe /app/secretsnotifier

CMD ["/app/secretsnotifier"]
