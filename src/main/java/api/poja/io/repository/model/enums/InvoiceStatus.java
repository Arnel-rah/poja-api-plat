package api.poja.io.repository.model.enums;

public enum InvoiceStatus {
  DRAFT("draft"),
  OPEN("open"),
  PAID("paid") {
    @Override
    public boolean canBePaid() {
      return false;
    }
  },
  VOID("void") {
    @Override
    public boolean canBePaid() {
      return false;
    }
  },
  UNCOLLECTIBLE("uncollectible") {
    @Override
    public boolean canBePaid() {
      return false;
    }
  },
  CANCELED("canceled") {
    @Override
    public boolean canBePaid() {
      return false;
    }
  },
  PROCESSING("processing") {
    @Override
    public boolean canBePaid() {
      return false;
    }
  },
  REQUIRES_ACTION("requires_action"),
  REQUIRES_CAPTURE("requires_capture"),
  REQUIRES_CONFIRMATION("requires_confirmation"),
  REQUIRES_PAYMENT_METHOD("requires_payment_method"),
  UNKNOWN("unknown") {
    @Override
    public boolean canBePaid() {
      return false;
    }
  };
  private final String value;

  InvoiceStatus(String value) {
    this.value = value;
  }

  public static InvoiceStatus fromValue(String value) {
    for (InvoiceStatus b : InvoiceStatus.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

  public boolean canBePaid() {
    return true;
  }
}
