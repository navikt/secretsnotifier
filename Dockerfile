FROM gcr.io/distroless/static-debian11

COPY build/bin/native/macReleaseExecutable/mac.kexe /secretsnotifier

ENTRYPOINT ["/secretsnotifier"]
