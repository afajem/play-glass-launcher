package controllers;

import controllers.*;
import play.libs.F;
import play.mvc.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


public class GlassSecurity {

    /**
     * Wraps the annotated action in an <code>GlassAuthenticatedAction</code>.
     */
    @With(GlassAuthenticatedAction.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface GlassAuthenticated {
    }



    /**
     * Wraps another action, allowing only authenticated HTTP requests.
     * <p>
     * The user id is retrieved from the session cookie, and added to the HTTP request's
     * <code>userId</code> attribute.
     */
    public static class GlassAuthenticatedAction extends Action<GlassAuthenticated> {

        public F.Promise<Result> call(Http.Context ctx) throws Throwable {
            try {
                //Fetch current user Id if it exists
                String userId = ctx.session().get("userId");

                //Redirect to authflow as the user is not yet registered
                if(userId == null) {
                    return F.Promise.pure(redirect(controllers.routes.GoogleClientOAuthController.oauth2Callback().url()));
                }
            } catch(RuntimeException e) {
                throw e;
            } catch(Throwable t) {
                throw new RuntimeException(t);
            }

            return delegate.call(ctx);
        }

    }


}
