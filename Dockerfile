FROM cgr.dev/chainguard/jre

COPY build/libs/*.jar /app/

CMD ["-jar", "/app/app-yolo.jar"]
