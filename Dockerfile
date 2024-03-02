FROM ghcr.io/graalvm/native-image-community:21 as build
WORKDIR /app
COPY . /app
RUN ./mvnw -Pnative native:compile

FROM alpine
COPY --from=build /app/target/rinha2024q1 /backend
RUN apk add libc6-compat
EXPOSE 8080
CMD ["./backend"]