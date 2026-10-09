package example;

public final class PaymentWorkerTest {
  public static void main(String[] args) {
    PaymentWorker worker = new PaymentWorker();
    if (worker.decide(150000, "US") != PaymentWorker.Decision.HOLD_FOR_REVIEW)
      throw new AssertionError("high-value payment must be held");
    if (worker.decide(5000, "US") != PaymentWorker.Decision.ACK)
      throw new AssertionError("ordinary domestic payment must be acknowledged");
    System.out.println("payment decisions verified");
  }
}
