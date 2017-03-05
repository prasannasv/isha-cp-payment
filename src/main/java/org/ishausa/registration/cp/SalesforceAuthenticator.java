package org.ishausa.registration.cp;

import com.sforce.soap.enterprise.Connector;
import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

import java.util.logging.Logger;

/**
 * Created by Prasanna Venkat on 3/4/2017.
 */
public class SalesforceAuthenticator {
    private static final Logger log = Logger.getLogger(SalesforceAuthenticator.class.getName());

    public EnterpriseConnection login() throws ConnectionException {
        final String sfUsername = System.getenv("sf_username");
        final String sfPasswordAndKey = System.getenv("sf_password_and_key");

        final ConnectorConfig config = new ConnectorConfig();
        config.setUsername(sfUsername);
        config.setPassword(sfPasswordAndKey);
        final EnterpriseConnection connection = Connector.newConnection(config);

        log.fine("Auth EndPoint: " + config.getAuthEndpoint());
        log.fine("Service EndPoint: " + config.getServiceEndpoint());
        log.fine("Username: " + config.getUsername());
        log.fine("SessionId: " + config.getSessionId());

        return connection;
    }
}
