package org.ishausa.registration.cp;

import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.Error;
import com.sforce.soap.enterprise.QueryResult;
import com.sforce.soap.enterprise.SaveResult;
import com.sforce.soap.enterprise.sobject.Child__c;
import com.sforce.soap.enterprise.sobject.Payment_Information__c;
import com.sforce.soap.enterprise.sobject.SObject;
import com.sforce.ws.ConnectionException;
import org.ishausa.registration.cp.payment.PaymentProcessor;
import org.ishausa.registration.cp.payment.TransactionStatus;

import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Prasanna Venkat on 3/4/2017.
 */
public class RegistrationApp {
    private static final Logger log = Logger.getLogger(RegistrationApp.class.getName());

    public static void main(final String[] args) throws ConnectionException {
        final SalesforceAuthenticator authenticator = new SalesforceAuthenticator();
        final EnterpriseConnection connection = authenticator.login();
        final PaymentProcessor paymentProcessor = new PaymentProcessor();

        queryParticipant(connection);

        transferAndLog(paymentProcessor, connection);
    }

    private static void transferAndLog(final PaymentProcessor processor, final EnterpriseConnection connection) {
        createPaymentInfoChildrenProgram(connection, "a2s0G00000BNS3N",
                //todo: get this from payment processor
                TransactionStatus.fromResponse(""));
    }

    private static void queryParticipant(final EnterpriseConnection connection) {
        try {
            final QueryResult queryResult = connection.query(
                    "select id, name, Full_Name__c, " +
                            "Parent_or_gaurdian__r.FirstName, Parent_or_gaurdian__r.LastName, " +
                            "Parent_or_gaurdian__r.email " +
                            "from Child__c " +
                            "where id='a2s0G00000BNS3N'");

            if (queryResult.getRecords().length > 0) {
                for (final SObject object : queryResult.getRecords()) {
                    final Child__c child = (Child__c) object;
                    log.info(String.format(
                            "Id: %s, Name: %s, Full Name: %s, Parent.First: %s, Parent.Last: %s, Parent.Email: %s",
                            child.getId(), child.getName(), child.getFull_Name__c(),
                            child.getParent_or_gaurdian__r().getFirstName(),
                            child.getParent_or_gaurdian__r().getLastName(),
                            child.getParent_or_gaurdian__r().getEmail()
                    ));
                }
            }
        } catch (final ConnectionException e) {
            log.log(Level.SEVERE, "Exception querying Child__c", e);
        }
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
