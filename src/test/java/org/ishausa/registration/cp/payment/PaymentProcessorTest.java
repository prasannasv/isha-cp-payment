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
        final CreditCard card = new CreditCard("4147099565229612", "Visa", 8, 2020);
        final CardOwnerInfo ownerInfo = new CardOwnerInfo.Builder()
                .withFirstName("Prasanna")
                .withLastName("Venkat")
                .withAddressLine1("208 Orcas PL SE")
                .withCity("Renton")
                .withState("WA")
                .withZip("98059")
                .withEmail("to.srini@gmail.com").build();

        paymentProcessor.chargeCreditCard(amount, card, ownerInfo);
    }

    @Test
    public void invalidCard() {
        final PaymentInfo amount = new PaymentInfo(1, "Children's Program Jul 2017", "Children's Program Jul 2017");
        final CreditCard card = new CreditCard("4147099565223112", "Visa", 1, 2010);
        final CardOwnerInfo ownerInfo = new CardOwnerInfo.Builder()
                .withFirstName("Prasanna")
                .withLastName("Venkat")
                .withAddressLine1("208 Orcas PL SE")
                .withCity("Renton")
                .withState("WA")
                .withZip("98059")
                .withEmail("to.srini@gmail.com").build();

        paymentProcessor.chargeCreditCard(amount, card, ownerInfo);
    }
}
