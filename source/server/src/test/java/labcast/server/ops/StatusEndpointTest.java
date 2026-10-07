package labcast.server.ops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StatusEndpointTest {

    @Test
    void versionTraVeDungCommitDangChay() throws Exception {
        BuildInfo info = new BuildInfo("0.1.0", "abc123", Instant.parse("2026-10-06T00:00:00Z"));

        try (StatusEndpoint status = StatusEndpoint.start(0, info)) {
            HttpResponse<String> res = HttpClient.newHttpClient()
                    .send(
                            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + status.port() + "/version"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

            assertEquals(200, res.statusCode());
            assertTrue(res.body().contains("\"commit\":\"abc123\""), res.body());
        }
    }
}
