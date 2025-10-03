package ru.nsu.abramenko;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Spider {
    private HttpClient client;
    private String baseUrl;
    private ObjectMapper mapper;

    public Spider(String address, int port) {
        client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.baseUrl = String.format("http://%s:%d", address, port);
        mapper = new ObjectMapper();
    }

    @SneakyThrows
    public Response get(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        var uri = new URI(baseUrl + path);
        var request = HttpRequest
                .newBuilder(uri)
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return getBody(response);
    }

    @SneakyThrows
    private Response getBody(HttpResponse<String> httpResponse) {
        if (httpResponse.statusCode() != HttpStatus.SC_OK) {
            return null;
        }
        return mapper.readValue(httpResponse.body(), Response.class);
    }
}
