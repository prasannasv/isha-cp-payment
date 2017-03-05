package org.ishausa.registration.cp.payment;

import com.paypal.api.payments.CreditCard;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by Prasanna Venkat on 3/4/2017.
 */
public class PaymentProcessorTest {
    private PaymentProcessor paymentProcessor;

    @Before
    public void setUp() {
        paymentProcessor = new PaymentProcessor();
    }

    @Test
    @Ignore
    public void validCard() {
        final PaymentInfo amount = new PaymentInfo(1, "Children's Program Jul 2017", "Children's Program Jul 2017");
        final CreditCard card = new CreditCard("4147008081913313", "Visa", 1, 2040);
        final CardOwnerInfo ownerInfo = new CardOwnerInfo.Builder()
                .withFirstName("Prasanna")
                .withLastName("Venkat")
                .withAddressLine1("123 2nd Ave N")
                .withCity("Atlanta")
                .withState("GA")
                .withZip("30301")
                .withEmail("random@gmail.com").build();

        paymentProcessor.chargeCreditCard(amount, card, ownerInfo);
    }

    @Test
    public void invalidCard() {
        final PaymentInfo amount = new PaymentInfo(1, "Children's Program Jul 2017", "Children's Program Jul 2017");
        final CreditCard card = new CreditCard("4147008081913313", "Visa", 1, 2010);
        final CardOwnerInfo ownerInfo = new CardOwnerInfo.Builder()
                .withFirstName("Prasanna")
                .withLastName("Venkat")
                .withAddressLine1("123 2nd Ave N")
                .withCity("Atlanta")
                .withState("GA")
                .withZip("30301")
                .withEmail("random@gmail.com").build();

        paymentProcessor.chargeCreditCard(amount, card, ownerInfo);
    }
}
