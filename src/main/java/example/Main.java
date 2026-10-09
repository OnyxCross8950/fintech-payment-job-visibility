package example;

public final class Main {
  public static void main(String[] args) throws Exception {
    InfraiClient client = new InfraiClient();
    client.createCron("https://example.test/payments/poll");
    String result = client.consume();
    System.out.println("payment poll response: " + result);
  }
}
