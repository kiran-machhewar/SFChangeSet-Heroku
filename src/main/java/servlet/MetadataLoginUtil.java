package servlet;

import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

/**
 * Login utility.
 */
public class MetadataLoginUtil {

    public static MetadataConnection loginToSourceOrg(String username,String password,String orgType) throws ConnectionException {
        // This is only a sample. Hard coding passwords in source files is a bad practice.
        final String URL = "https://"+((orgType.equalsIgnoreCase("Production"))?"login":"test")+".salesforce.com/services/Soap/u/35.0";
        final LoginResult loginResult = loginToSalesforce(username, password, URL);
        return createMetadataConnection(loginResult);
    }

    public static MetadataConnection loginToTargetOrg(String username,String password,String orgType) throws ConnectionException {
         // This is only a sample. Hard coding passwords in source files is a bad practice.
    	 final String URL = "https://"+((orgType.equalsIgnoreCase("Production"))?"login":"test")+".salesforce.com/services/Soap/u/35.0";
         final LoginResult loginResult = loginToSalesforce(username, password, URL);
         return createMetadataConnection(loginResult);
    }
    private static MetadataConnection createMetadataConnection(
            final LoginResult loginResult) throws ConnectionException {
        final ConnectorConfig config = new ConnectorConfig();
        config.setServiceEndpoint(loginResult.getMetadataServerUrl());
        config.setSessionId(loginResult.getSessionId());
        return new MetadataConnection(config);
    }

    private static LoginResult loginToSalesforce(
            final String username,
            final String password,
            final String loginUrl) throws ConnectionException {
        final ConnectorConfig config = new ConnectorConfig();
        config.setAuthEndpoint(loginUrl);
        config.setServiceEndpoint(loginUrl);
        config.setManualLogin(true);
        return (new PartnerConnection(config)).login(username, password);
    }
}