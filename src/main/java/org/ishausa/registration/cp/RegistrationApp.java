package org.ishausa.registration.cp;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.Error;
import com.sforce.soap.enterprise.QueryResult;
import com.sforce.soap.enterprise.SaveResult;
import com.sforce.soap.enterprise.sobject.Child__c;
import com.sforce.soap.enterprise.sobject.Contact;
import com.sforce.soap.enterprise.sobject.Payment_Information__c;
import com.sforce.ws.ConnectionException;
import org.ishausa.registration.cp.payment.PaymentProcessor;
import org.ishausa.registration.cp.payment.TransactionStatus;
import org.ishausa.registration.cp.renderer.SoyRenderer;
import org.ishausa.registration.cp.security.HttpsEnforcer;
import spark.Request;
import spark.Response;

import java.util.Calendar;
import java.util.HashMap;
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
    private static final int CHILDRENS_PROGRAM_AMOUNT = 925;

    private final SalesforceAuthenticator authenticator;
    private final EnterpriseConnection connection;
    private final PaymentProcessor paymentProcessor;

    public RegistrationApp() throws ConnectionException {
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
            if (child != null) {
                final Map<String, Object> soyData = new HashMap<>();

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
        return "Yet to implement";
    }

    private static void transferAndLog(final PaymentProcessor processor, final EnterpriseConnection connection) {
        createPaymentInfoChildrenProgram(connection, "a2s0G00000BNS3N",
                //todo: get this from payment processor
                TransactionStatus.fromResponse(""));
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

    private static boolean createPaymentInfoChildrenProgram(final EnterpriseConnection connection,
                                                            final String childContactId,
                                                            final TransactionStatus status) {
        try {
            final Payment_Information__c pI = new Payment_Information__c();
            final Calendar sCal = Calendar.getInstance();
            pI.setDate_of_Deposit_or_Date_CC_was_Charged__c(sCal);
            pI.setPayment_Amount__c(925.00);
            pI.setVendor_Confirmation_Number__c(status.getTransactionId());
            pI.setChildrens_Program_Payment__c(childContactId);

            //not mandatory
            pI.setCredit_card_type__c("VISA");
            //usually store the last four digit... not mandatory
            pI.setCredit_card_Number__c("XXXXXX1234");

            //set the payment type
            pI.setMode_of_Payment__c("Paypal");
            pI.setVolunteer_handled_the_payment__c("isha-cp-reg heroku");

            //Now we can create the payment object
            Payment_Information__c[] payments = new Payment_Information__c[1];
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
