package org.ishausa.registration.cp;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.paypal.api.payments.CreditCard;
import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.Error;
import com.sforce.soap.enterprise.QueryResult;
import com.sforce.soap.enterprise.SaveResult;
import com.sforce.soap.enterprise.sobject.Child__c;
import com.sforce.soap.enterprise.sobject.Contact;
import com.sforce.soap.enterprise.sobject.Payment_Information__c;
import com.sforce.ws.ConnectionException;
import org.ishausa.registration.cp.http.NameValuePairs;
import org.ishausa.registration.cp.payment.CardOwnerInfo;
import org.ishausa.registration.cp.payment.CardType;
import org.ishausa.registration.cp.payment.PaymentInfo;
import org.ishausa.registration.cp.payment.PaymentProcessor;
import org.ishausa.registration.cp.payment.TransactionStatus;
import org.ishausa.registration.cp.renderer.SoyRenderer;
import org.ishausa.registration.cp.security.HttpsEnforcer;
import spark.Request;
import spark.Response;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFiles;

/**
 * Created by Prasanna Venkat on 3/4/2017.
 */
public class RegistrationApp {
    private static final Logger log = Logger.getLogger(RegistrationApp.class.getName());
    private static final int CHILDRENS_PROGRAM_AMOUNT = 1;
    private static final String CHILDRENS_PROGRAM_DESC = "Children's Program Jul 2017";

    private static final String CARD_NUMBER_PARAM = "cardNumber";
    private static final String CARD_EXPIRY_MONTH_PARAM = "expiryMonth";
    private static final String CARD_EXPIRY_YEAR_PARAM = "expiryYear";
    private static final String CARD_CVV2_PARAM = "cvv2";
    private static final String OWNER_FIRST_NAME_PARAM = "cardOwnerFirstName";
    private static final String OWNER_LAST_NAME_PARAM = "cardOwnerLastName";
    private static final String OWNER_STREET_ADDRESS_PARAM = "streetAddress";
    private static final String OWNER_CITY_PARAM = "city";
    private static final String OWNER_STATE_PARAM = "state";
    private static final String OWNER_EMAIL_PARAM = "email";
    private static final String OWNER_ZIPCODE_PARAM = "zipcode";
    private static final String CHILD_ID_PARAM = "childId";

    private final SalesforceAuthenticator authenticator;
    private final EnterpriseConnection connection;
    private final PaymentProcessor paymentProcessor;

    private RegistrationApp() throws ConnectionException {
        authenticator = new SalesforceAuthenticator();
        connection = authenticator.login();
        paymentProcessor = new PaymentProcessor();
    }

    public static void main(final String[] args) throws ConnectionException {
        final RegistrationApp app = new RegistrationApp();

        port(Integer.parseInt(System.getenv("PORT")));
        staticFiles.location("/static");

        get("/charge", app::handleGet);

        post("/", app::handlePost);

        exception(Exception.class, ((exception, request, response) -> {
            log.info("Exception: " + exception + " stack: " + Throwables.getStackTraceAsString(exception));
            response.status(500);
            response.body("Exception: " + exception + " stack: " + Throwables.getStackTraceAsString(exception));
        }));

        app.initFilters();
    }

    private void initFilters() {
        before(new HttpsEnforcer());
    }

    private String handleGet(final Request request, final Response response) {
        final String childId = request.queryParams("id");

        if (!Strings.isNullOrEmpty(childId)) {
            final Child__c child = queryParticipant(connection, childId);
            //TODO: Check if participant has already paid. If so, don't show this page.
            if (child != null) {
                final Map<String, Object> soyData = new HashMap<>();

                soyData.put("childId", childId);
                soyData.put("childFullName", child.getFull_Name__c());
                final Contact parent = child.getParent_or_gaurdian__r();
                soyData.put("parentsFullName", parent.getFirstName() + " " + parent.getLastName());
                soyData.put("parentsEmail", parent.getEmail());
                soyData.put("amount", CHILDRENS_PROGRAM_AMOUNT);

                return SoyRenderer.INSTANCE.render(SoyRenderer.RegistrationAppTemplate.INDEX, soyData);
            }
        }

        return SoyRenderer.INSTANCE.render(SoyRenderer.RegistrationAppTemplate.NON_EXISTENT_ID,
                    ImmutableMap.of("id", Strings.nullToEmpty(childId)));
    }

    private String handlePost(final Request request, final Response response) {
        final String content = request.body();
        final Map<String, List<String>> params = NameValuePairs.splitParams(content);
        log.info("content: " + content + ", params: " + params);
        // Validate input
        final String childId = NameValuePairs.nullSafeGetFirst(params, CHILD_ID_PARAM);
        if (Strings.isNullOrEmpty(childId)) {
            return SoyRenderer.INSTANCE.render(SoyRenderer.RegistrationAppTemplate.NON_EXISTENT_ID,
                    ImmutableMap.of("id", Strings.nullToEmpty(childId)));
        }
        final Child__c child = queryParticipant(connection, childId);
        if (child == null) {
            return SoyRenderer.INSTANCE.render(SoyRenderer.RegistrationAppTemplate.NON_EXISTENT_ID,
                    ImmutableMap.of("id", Strings.nullToEmpty(childId)));
        }

        // Create PaymentInfo, CreditCard (set cvv2), CardOwnerInfo
        final PaymentInfo paymentInfo =
                new PaymentInfo(CHILDRENS_PROGRAM_AMOUNT, CHILDRENS_PROGRAM_DESC, CHILDRENS_PROGRAM_DESC);

        final String cardNumber = NameValuePairs.nullSafeGetFirst(params, CARD_NUMBER_PARAM);
        final CardType cardType = CardType.detect(cardNumber);
        final String expiryMonthStr = NameValuePairs.nullSafeGetFirst(params, CARD_EXPIRY_MONTH_PARAM);
        final String expiryYearStr = NameValuePairs.nullSafeGetFirst(params, CARD_EXPIRY_YEAR_PARAM);
        final String cvv2 = NameValuePairs.nullSafeGetFirst(params, CARD_CVV2_PARAM);

        final CreditCard card = new CreditCard(cardNumber, cardType.getTypeName(), Integer.parseInt(expiryMonthStr),
                Integer.parseInt(expiryYearStr));
        card.setCvv2(cvv2);

        final CardOwnerInfo cardOwnerInfo = new CardOwnerInfo.Builder()
                .withFirstName(NameValuePairs.nullSafeGetFirst(params, OWNER_FIRST_NAME_PARAM))
                .withLastName(NameValuePairs.nullSafeGetFirst(params, OWNER_LAST_NAME_PARAM))
                .withAddressLine1(NameValuePairs.nullSafeGetFirst(params, OWNER_STREET_ADDRESS_PARAM))
                .withCity(NameValuePairs.nullSafeGetFirst(params, OWNER_CITY_PARAM))
                .withState(NameValuePairs.nullSafeGetFirst(params, OWNER_STATE_PARAM))
                .withEmail(NameValuePairs.nullSafeGetFirst(params, OWNER_EMAIL_PARAM))
                .withZip(NameValuePairs.nullSafeGetFirst(params, OWNER_ZIPCODE_PARAM))
                .build();

        // Use PaymentProcessor to do the payment
        final TransactionStatus status = paymentProcessor.chargeCreditCard(paymentInfo, card, cardOwnerInfo);
        // Log that info in Salesforce
        createPaymentInfoChildrenProgram(connection, childId, paymentInfo, card, status);
        // Respond with payment success page with transactionId
        if (status.getAcknowledgment().startsWith("Success")) {
            return SoyRenderer.INSTANCE.render(SoyRenderer.RegistrationAppTemplate.PAYMENT_SUCCESS,
                    ImmutableMap.of("childFullName", child.getFull_Name__c(),
                            "transactionId", status.getTransactionId()));
        }
        // If payment failed, send the payment failure page with error message.
        return SoyRenderer.INSTANCE.render(SoyRenderer.RegistrationAppTemplate.PAYMENT_FAILURE,
                ImmutableMap.of("childFullName", child.getFull_Name__c(),
                        "longMessage", status.getLongMessage()));
    }

    private Child__c queryParticipant(final EnterpriseConnection connection, final String childId) {
        try {
            final QueryResult queryResult = connection.query(
                    String.format("select id, name, Full_Name__c, " +
                            "Parent_or_gaurdian__r.FirstName, Parent_or_gaurdian__r.LastName, " +
                            "Parent_or_gaurdian__r.email " +
                            "from Child__c " +
                            "where id='%s'", childId));

            if (queryResult.getRecords().length > 0) {
                final Child__c child = (Child__c) queryResult.getRecords()[0];
                log.info(String.format(
                        "Id: %s, Name: %s, Full Name: %s, Parent.First: %s, Parent.Last: %s, Parent.Email: %s",
                        child.getId(), child.getName(), child.getFull_Name__c(),
                        child.getParent_or_gaurdian__r().getFirstName(),
                        child.getParent_or_gaurdian__r().getLastName(),
                        child.getParent_or_gaurdian__r().getEmail()
                ));
                return child;
            }
        } catch (final ConnectionException e) {
            log.log(Level.SEVERE, "Exception querying Child__c", e);
        }
        return null;
    }

    private boolean createPaymentInfoChildrenProgram(final EnterpriseConnection connection,
                                                     final String childContactId,
                                                     final PaymentInfo paymentInfo,
                                                     final CreditCard card,
                                                     final TransactionStatus status) {
        try {
            final Payment_Information__c pI = new Payment_Information__c();
            final Calendar sCal = Calendar.getInstance();
            pI.setDate_of_Deposit_or_Date_CC_was_Charged__c(sCal);
            pI.setPayment_Amount__c((double) paymentInfo.getAmount());
            pI.setVendor_Confirmation_Number__c(status.getTransactionId());
            pI.setChildrens_Program_Payment__c(childContactId);

            //not mandatory
            pI.setCredit_card_type__c(card.getType());
            //usually store the last four digit... not mandatory
            pI.setCredit_card_Number__c(card.getNumber().substring(card.getNumber().length() - 4));

            //set the payment type
            pI.setMode_of_Payment__c("Paypal");
            pI.setVolunteer_handled_the_payment__c("cp-payment heroku"); //max length 20 chars

            //Now we can create the payment object
            final Payment_Information__c[] payments = new Payment_Information__c[1];
            payments[0] = pI;

            final SaveResult createResult = connection.create(payments)[0];

            if (createResult.isSuccess()) {
                log.info("Successfully created the Payment Info record - Id: " + createResult.getId());
            } else {
                for (final Error error : createResult.getErrors()) {
                    log.severe("ERROR updating record: " + error.getMessage());
                }
            }
        } catch (final Exception e) {
            log.log(Level.SEVERE, "Unknown failure updating payment info", e);
            return false;
        }
        return true;
    }
}
