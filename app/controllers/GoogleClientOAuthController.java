package controllers;


import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.GenericUrl;
import com.google.api.services.mirror.model.MenuItem;
import com.google.api.services.mirror.model.NotificationConfig;
import com.google.api.services.mirror.model.Subscription;
import com.google.api.services.mirror.model.TimelineItem;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import play.Play;

import services.GlassAuthUtil;
import services.MirrorServiceUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class GoogleClientOAuthController extends Controller{


    /**
     * THe OAuth2 callback endpoint
     *
     * @return
     * @throws IOException
     */
    public static Result oauth2Callback() throws IOException {

        String redirectUri =
             controllers.routes.GoogleClientOAuthController.oauth2Callback().absoluteURL(request());

        String code = request().getQueryString("code");

        //Proceed with oauth flow if code was sent
        if (code != null) {
            Logger.info("Got a code. Attempting to exchange for access token");

            AuthorizationCodeFlow flow = GlassAuthUtil.newAuthorizationCodeFlow();

            TokenResponse tokenResponse = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();

            //Fetch user id from the ID token in the auth response
            String userId = GoogleIdToken.parse(tokenResponse.getFactory(), (String) tokenResponse.get("id_token"))
                    .getPayload().getSubject();

            Logger.info("Code exchange worked. User " + userId + " logged in.");

            //Set the user id in the session (cookie)
            session("userId", userId);
            flow.createAndStoreCredential(tokenResponse, userId);

            //TODO: Perform New User Bootstrap
            bootstrapNewUser(userId);

            //Redirect to the root of the application
            return redirect(controllers.routes.Application.index().absoluteURL(request()));
        }

        // Else, we have a new flow. Initiate a new flow.
        Logger.info("No auth context found. Kicking off a new auth flow.");

        AuthorizationCodeFlow flow = GlassAuthUtil.newAuthorizationCodeFlow();
        GenericUrl url =
                flow.newAuthorizationUrl().setRedirectUri(redirectUri);
        url.set("approval_prompt", "force");
        url.set("access_type", "offline");

        //rerun the action to continue the auth flow
        return redirect(url.build());
    }


    /**
     * Bootstraps the user by subscribing to updates from the user and inserting a
     * welcome timeline.
     *
     * @param userId
     * @throws IOException
     */
    private static void bootstrapNewUser(String userId) throws IOException {
        Credential credential  = GlassAuthUtil.newAuthorizationCodeFlow().loadCredential(userId);

        //////////
        //Subscribe to timeline updates
        try {
            String redirectUri =
                    controllers.routes.GoogleGlassNotifierController.notification().absoluteURL(request());

            // Subscribe to timeline updates
            Subscription subscription =
                    MirrorServiceUtil.insertSubscription(credential, redirectUri, userId, "timeline");

            Logger.info("Bootstrapper inserted subscription " + subscription
                    .getId() + " for user " + userId);
        } catch (GoogleJsonResponseException e) {
            //TODO: Implement Subscription Proxy???
            Logger.warn("Failed to create timeline subscription. Might be related to running on "
                    + "localhost. Details:" + e.getDetails().toPrettyString());
        }

        /////////
        // Send welcome timeline item
        TimelineItem timelineItem = new TimelineItem();
        timelineItem.setText("Welcome to \n" + Play.application().configuration().getString("applicationName") + "!");
        timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

        List<MenuItem> menuItemList = new ArrayList<MenuItem>();
        MenuItem deleteMenuItem = new MenuItem();
        deleteMenuItem.setAction("DELETE");
        menuItemList.add(deleteMenuItem);

        timelineItem.setMenuItems(menuItemList);

        TimelineItem insertedItem = MirrorServiceUtil.insertTimelineItem(credential, timelineItem);

        Logger.info("Bootstrapper inserted welcome message " + insertedItem.getId() + " for user "
                + userId);
    }
}
