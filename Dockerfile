FROM gcr.io/distroless/static-debian11

COPY build/bin/native/releaseExecutable/secretsnotifier.kexe /secretsnotifier

ENTRYPOINT ["/secretsnotifier"]
