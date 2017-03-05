package org.ishausa.registration.cp;

import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.QueryResult;
import com.sforce.soap.enterprise.sobject.Child__c;
import com.sforce.soap.enterprise.sobject.SObject;
import com.sforce.ws.ConnectionException;

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

        queryParticipant(connection);
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
}
