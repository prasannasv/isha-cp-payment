package org.ishausa.registration.cp;

import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.QueryResult;
import com.sforce.soap.enterprise.SaveResult;
import com.sforce.soap.enterprise.sobject.Child__c;
import com.sforce.soap.enterprise.sobject.Payment_Information__c;
import com.sforce.soap.enterprise.sobject.SObject;
import com.sforce.ws.ConnectionException;
import org.ishausa.registration.cp.payment.PaymentProcessor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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

        performTransaction(paymentProcessor);
    }

    private static void performTransaction(final PaymentProcessor processor) {
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

    private boolean createPaymentInfoChildrenProgram(final EnterpriseConnection connection,
                                                     final String childContactId) {
        try {
            final Payment_Information__c pI = new Payment_Information__c();
            final Calendar sCal = Calendar.getInstance();
            final Date trans_date = new Date();
            if (trans_date != null) {
                sCal.setTime(trans_date);
                pI.setDate_of_Deposit_or_Date_CC_was_Charged__c(sCal);
            } else {
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Calendar cal11 = Calendar.getInstance();
                pI.setDate_of_Deposit_or_Date_CC_was_Charged__c(cal11);
            }
            pI.setPayment_Amount__c(1000.00);
            pI.setVendor_Confirmation_Number__c("PAYPALXXXXX1");
            pI.setChildrens_Program_Payment__c(childContactId);

            //not mandatory
            pI.setCredit_card_type__c("VISA");
            //usually store the last four digit... not mandatory
            pI.setCredit_card_Number__c("XXXXXX1234");

            //set the payment type
            pI.setMode_of_Payment__c("Paypal");
            pI.setVolunteer_handled_the_payment__c("Event Espresso");

            //Now we can create the payment object
            Payment_Information__c[] payments = new Payment_Information__c[1];
            payments[0] = pI;

            SaveResult createResult = connection.create(payments)[0];

            if (createResult.isSuccess()) {
                System.out.println("Successfully created the Payment for Visitor Info record - Id: " + createResult.getId());
            } else {
                com.sforce.soap.enterprise.Error[] errors = createResult.getErrors();
                for (int j = 0; j < errors.length; j++) {
                    System.out.println("ERROR updating record: " + errors[j].getMessage());
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
