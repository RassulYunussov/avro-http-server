package org.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.avro.User;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.io.IOException;

public class App {
    private static final AvroSerializer<User> avroSerializer = new AvroSerializer<>(User.getClassSchema());
    private static final ObjectMapper jsonSerializer = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static void main(String[] args) {
        DisposableServer server =
                HttpServer.create()
                        .route(routes ->
                                routes.post("/avro", (request, response) -> request.receive().asByteBuffer().flatMap(byteBuffer -> {
                                            byte[] requestData = new byte[byteBuffer.remaining()];
                                            byteBuffer.get(requestData);
                                            try {
                                                final var requestUser = avroSerializer.deserialize(requestData);
                                                requestUser.setName("updated " + requestUser.getName());
                                                final var responseData = avroSerializer.serialize(requestUser);
                                                return response.sendByteArray(Mono.just(responseData));
                                            } catch (IOException e) {
                                                throw new IllegalArgumentException(e);
                                            }
                                        }))
                                        .post("/json", (request, response) -> request.receive().asByteBuffer().flatMap(byteBuffer -> {
                                            byte[] requestData = new byte[byteBuffer.remaining()];
                                            byteBuffer.get(requestData);
                                            try {
                                                final var requestUser = jsonSerializer.readValue(requestData, org.example.json.User.class);
                                                requestUser.setName("updated " + requestUser.getName());
                                                final var responseData = jsonSerializer.writeValueAsBytes(requestUser);
                                                return response.sendByteArray(Mono.just(responseData));
                                            } catch (IOException e) {
                                                throw new IllegalArgumentException(e);
                                            }
                                        }))
                        )
                        .port(8080)
                        .host("localhost")
                        .bindNow();

        server.onDispose().block();
    }
}
