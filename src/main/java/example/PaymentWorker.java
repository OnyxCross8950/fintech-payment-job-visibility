package example;

public final class PaymentWorker {
  public enum Decision { ACK, HOLD_FOR_REVIEW }

  public Decision decide(long amountCents, String country) {
    return amountCents > 100000 || !("US".equals(country) || "CA".equals(country))
        ? Decision.HOLD_FOR_REVIEW : Decision.ACK;
  }

  public void process(InfraiClient client, String messageId, long amountCents, String country) throws Exception {
    if (decide(amountCents, country) == Decision.HOLD_FOR_REVIEW) {
      client.capture("{\"exception\":\"payment held for compliance review\",\"message_id\":\"" + messageId + "\"}");
      client.nack(messageId);
    } else client.ack(messageId);
  }
}
