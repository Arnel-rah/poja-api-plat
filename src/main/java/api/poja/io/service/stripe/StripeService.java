package api.poja.io.service.stripe;

import static api.poja.io.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static api.poja.io.model.money.Currency.CENTS_EUR;
import static api.poja.io.repository.model.enums.InvoiceStatus.PAID;
import static com.stripe.param.CouponCreateParams.Duration.ONCE;
import static com.stripe.param.CustomerUpdateParams.Tax.ValidateLocation.IMMEDIATELY;
import static com.stripe.param.InvoiceItemCreateParams.TaxBehavior.EXCLUSIVE;

import api.poja.io.endpoint.rest.model.Address;
import api.poja.io.model.exception.ApiException;
import api.poja.io.model.exception.InternalServerErrorException;
import api.poja.io.model.money.Money;
import api.poja.io.repository.model.enums.InvoiceStatus;
import com.stripe.exception.StripeException;
import com.stripe.model.Coupon;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceItem;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.net.RequestOptions;
import com.stripe.param.CouponCreateParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.InvoiceCreateParams;
import com.stripe.param.InvoiceFinalizeInvoiceParams;
import com.stripe.param.InvoiceItemCreateParams;
import com.stripe.param.InvoicePayParams;
import com.stripe.param.InvoiceUpdateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.PaymentMethodDetachParams;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class StripeService {
  public static final String EUR = "eur";
  private final StripeConf stripeConf;

  public Customer createCustomer(String name, String email) throws StripeException {
    CustomerCreateParams params =
        CustomerCreateParams.builder().setName(name).setEmail(email).build();
    return Customer.create(params, getRequestOption());
  }

  public List<PaymentMethod> getPaymentMethods(String customerId) {
    try {
      Customer customer = Customer.retrieve(customerId);
      PaymentMethodCollection pmCollection = customer.listPaymentMethods();
      return pmCollection.getData();
    } catch (StripeException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  public PaymentMethod setDefaultPaymentMethod(String customerId, String paymentMethodId) {
    try {
      Customer customer = Customer.retrieve(customerId);
      CustomerUpdateParams.InvoiceSettings invoiceSettingParams =
          CustomerUpdateParams.InvoiceSettings.builder()
              .setDefaultPaymentMethod(paymentMethodId)
              .build();
      CustomerUpdateParams params =
          CustomerUpdateParams.builder().setInvoiceSettings(invoiceSettingParams).build();
      customer.update(params);
      return PaymentMethod.retrieve(paymentMethodId);
    } catch (StripeException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  public PaymentMethod attachPaymentMethod(String customerId, String paymentMethodId) {
    try {
      PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);

      PaymentMethodAttachParams params =
          PaymentMethodAttachParams.builder().setCustomer(customerId).build();
      return paymentMethod.attach(params);
    } catch (StripeException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  public PaymentMethod detachPaymentMethod(String paymentMethodId) {
    try {
      PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);

      PaymentMethodDetachParams params = PaymentMethodDetachParams.builder().build();
      return paymentMethod.detach(params);
    } catch (StripeException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  public PaymentMethod retrievePaymentMethod(String paymentMethodId) {
    try {
      return PaymentMethod.retrieve(paymentMethodId);
    } catch (StripeException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  // Customer
  public Customer retrieveCustomer(String customerId) {
    try {
      return Customer.retrieve(customerId);
    } catch (StripeException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  public Customer updateCustomer(
      String id, String name, String email, String phone, Address address) throws StripeException {
    var resource = Customer.retrieve(id);
    var params =
        CustomerUpdateParams.builder()
            .setName(name)
            .setEmail(email)
            .setPhone(phone)
            .setTax(CustomerUpdateParams.Tax.builder().setValidateLocation(IMMEDIATELY).build())
            .setAddress(
                CustomerUpdateParams.Address.builder()
                    .setCountry(address.getCountry())
                    .setCity(address.getCity())
                    .setPostalCode(address.getPostalCode())
                    .setState(address.getState())
                    .setLine1(address.getLine1())
                    .setLine2(address.getLine2())
                    .build())
            .build();
    return resource.update(params);
  }

  public Customer updateCustomer(String id, String name, String email, String phone) {
    try {
      Customer resource = Customer.retrieve(id);
      CustomerUpdateParams params =
          CustomerUpdateParams.builder().setName(name).setEmail(email).setPhone(phone).build();
      return resource.update(params);
    } catch (StripeException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  public Invoice retrieveInvoice(String invoiceId) {
    try {
      return Invoice.retrieve(invoiceId);
    } catch (StripeException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public Invoice createInvoice(String customerId, boolean enableAutomaticTaxes) {
    try {
      var params =
          InvoiceCreateParams.builder()
              .setAutomaticTax(
                  InvoiceCreateParams.AutomaticTax.builder()
                      .setEnabled(enableAutomaticTaxes)
                      .build())
              .setCustomer(customerId)
              .setCurrency(EUR)
              .build();
      return Invoice.create(params);
    } catch (StripeException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  public InvoiceItem createInvoiceItem(
      String invoiceId, String customerId, Money amount, String invoiceItemDescription) {
    try {
      InvoiceItemCreateParams params =
          InvoiceItemCreateParams.builder()
              .setInvoice(invoiceId)
              .setAmount(amount.convertCurrency(CENTS_EUR).amount().longValue())
              .setDescription(invoiceItemDescription)
              .setCustomer(customerId)
              .build();
      return InvoiceItem.create(params);
    } catch (StripeException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  public InvoiceItem createInvoiceItem(
      String invoiceId,
      String customerId,
      Money unitPrice,
      Long quantity,
      String invoiceItemDescription) {
    try {
      var params =
          InvoiceItemCreateParams.builder()
              .setInvoice(invoiceId)
              .setUnitAmountDecimal(unitPrice.convertCurrency(CENTS_EUR).amount())
              .setTaxBehavior(EXCLUSIVE)
              .setQuantity(quantity)
              .setDescription(invoiceItemDescription)
              .setCustomer(customerId)
              .build();
      return InvoiceItem.create(params);
    } catch (StripeException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  public Invoice finalizeInvoice(String invoiceId) {
    try {
      Invoice resource = Invoice.retrieve(invoiceId);
      var params = InvoiceFinalizeInvoiceParams.builder().build();
      return resource.finalizeInvoice(params);
    } catch (StripeException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  public Invoice payInvoice(String invoiceId) {
    try {
      Invoice resource = Invoice.retrieve(invoiceId);
      var params = InvoicePayParams.builder().build();
      return resource.pay(params);
    } catch (StripeException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  public Invoice payInvoice(String invoiceId, String paymentMethodId) {
    try {
      Invoice resource = Invoice.retrieve(invoiceId);
      var params = InvoicePayParams.builder().setPaymentMethod(paymentMethodId).build();
      return resource.pay(params);
    } catch (StripeException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  public Invoice applyDiscountToInvoice(
      String invoiceId, String discountName, Long discountAmount) {
    try {
      var invoice = retrieveInvoice(invoiceId);
      var couponCreateParams =
          CouponCreateParams.builder()
              .setDuration(ONCE)
              .setName(discountName)
              .setAmountOff(discountAmount)
              .setCurrency(EUR)
              .build();
      var discountCoupon = Coupon.create(couponCreateParams);

      var params =
          InvoiceUpdateParams.builder()
              .addDiscount(
                  InvoiceUpdateParams.Discount.builder().setCoupon(discountCoupon.getId()).build())
              .build();

      return invoice.update(params);
    } catch (StripeException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  public PaymentIntent retrievePaymentIntent(String paymentIntentId) {
    try {
      return PaymentIntent.retrieve(paymentIntentId);
    } catch (StripeException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  public InvoiceStatus getPaymentStatus(Invoice invoice) {
    String status = invoice.getStatus();
    if (!Objects.equals(status, "paid")) {
      var paymentIntentStatus = retrievePaymentIntent(invoice.getPaymentIntent()).getStatus();
      return Objects.equals(paymentIntentStatus, "succeeded")
          ? PAID
          : InvoiceStatus.fromValue(paymentIntentStatus);
    }
    return PAID;
  }

  private RequestOptions getRequestOption() {
    return RequestOptions.builder().setApiKey(stripeConf.getApiKey()).build();
  }

  public Invoice voidInvoice(String invoiceId) {
    try {
      return retrieveInvoice(invoiceId).voidInvoice();
    } catch (StripeException e) {
      throw new InternalServerErrorException(e);
    }
  }
}
