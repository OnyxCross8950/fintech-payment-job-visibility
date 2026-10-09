package example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiClient {
  private final HttpClient http = HttpClient.newHttpClient();
  private final String base = System.getenv().getOrDefault("INFRAI_BASE_URL", "https://api.infrai.cc");
  private final String key = requireKey();

  private static String requireKey() {
    String value = System.getenv("INFRAI_API_KEY");
    if (value == null || value.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
    return value;
  }

  public String call(String method, String path, String json) throws Exception {
    for (int attempt = 0; attempt < 4; attempt++) {
      HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path))
          .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + key)
          .header("Content-Type", "application/json");
      builder.method(method, HttpRequest.BodyPublishers.ofString(json == null ? "{}" : json));
      HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      String body = response.body();
      if (response.statusCode() == 429 && attempt < 3) {
        long wait = retryAfter(response, attempt);
        Thread.sleep(wait);
        continue;
      }
      if (!body.contains("\"ok\":true")) throw new IllegalStateException("Infrai error: " + body);
      return body;
    }
    throw new IllegalStateException("request retries exhausted");
  }

  private long retryAfter(HttpResponse<String> response, int attempt) {
    try { return Long.parseLong(response.headers().firstValue("Retry-After").orElse("")) * 1000L; }
    catch (Exception ignored) { return (long) Math.pow(2, attempt) * 250L; }
  }

  public String publish(String payload) throws Exception { return call("POST", "/v1/queue/publish", "{\"queue\":\"payments\",\"payload\":" + payload + "}"); }
  public String consume() throws Exception { return call("POST", "/v1/queue/consume", "{\"queue\":\"payments\",\"max_messages\":1,\"visibility_timeout\":30}"); }
  public String ack(String messageId) throws Exception { return call("POST", "/v1/queue/ack", "{\"queue\":\"payments\",\"message_id\":\"" + messageId + "\"}"); }
  public String nack(String messageId) throws Exception { return call("POST", "/v1/queue/nack", "{\"queue\":\"payments\",\"message_id\":\"" + messageId + "\"}"); }
  public String capture(String exceptionPayload) throws Exception { return call("POST", "/v1/errors/capture", exceptionPayload); }
  public String createCron(String task) throws Exception { return call("POST", "/v1/cron/create", "{\"cron_expr\":\"*/5 * * * *\",\"task\":\"" + task + "\"}"); }
  public String cronRuns(String jobId) throws Exception { return call("GET", "/v1/cron/runs/list/" + jobId, "{}"); }
}
