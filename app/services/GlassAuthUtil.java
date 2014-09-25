package services;


import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import play.Play;
import play.mvc.Http;

import java.io.IOException;
import java.util.Collections;

/**
 * Contains utility functions related to authentication; specifically using Google as the
 * OAuth2 provider.
 *
 */
public class GlassAuthUtil {

    public static final String GLASS_SCOPE = "https://www.googleapis.com/auth/glass.timeline "
            + "https://www.googleapis.com/auth/glass.location "
            + "https://www.googleapis.com/auth/userinfo.profile";

    /**
     * Creates and returns a new {@link AuthorizationCodeFlow} for this app.
     */
    public static AuthorizationCodeFlow newAuthorizationCodeFlow() throws IOException {
        //Prep auth properties
        String clientId = Play.application().configuration().getString("clientId");
        String clientSecret = Play.application().configuration().getString("clientSecret");
        String tokenServerUrl = Play.application().configuration().getString("tokenServerUrl");
        String authorizationServerUrl = Play.application().configuration().getString("authorizationServerUrl");

        // Get the Auth builder
        AuthorizationCodeFlow.Builder authorizationCodeFlowBuilder =
            new AuthorizationCodeFlow.Builder(BearerToken.authorizationHeaderAccessMethod(),
                new NetHttpTransport(), new JacksonFactory(), new GenericUrl(tokenServerUrl),
                new ClientParametersAuthentication(clientId, clientSecret), clientId,
                authorizationServerUrl);

        //Initialize additional properties
        authorizationCodeFlowBuilder.setScopes(Collections.singleton(GLASS_SCOPE));
        authorizationCodeFlowBuilder.setDataStoreFactory(MemoryDataStoreFactory.getDefaultInstance());

        return authorizationCodeFlowBuilder.build();
    }


    /**
     * Obtains a Credential object needed for authentication
     *
     * @param userId the current user's id
     *
     * @return the resulting Credential
     * @throws IOException
     */
    public static Credential getCredential(String userId) throws IOException {
        if (userId == null) {
            return null;
        }
        else {
            return newAuthorizationCodeFlow().loadCredential(userId);
        }
    }


    /**
     * Invoked to retrieve the userId from the session cookie
     *
     * @param session
     * @return
     */
    public static String getUserId(Http.Session session) {
        return session.get("userId");
    }
}
