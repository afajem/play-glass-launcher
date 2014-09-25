package controllers;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.mirror.model.MenuItem;
import com.google.api.services.mirror.model.NotificationConfig;
import com.google.api.services.mirror.model.TimelineItem;
import play.Configuration;
import play.Logger;
import play.Play;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.ws.*;
import play.mvc.Controller;
import play.mvc.Result;
import services.GlassAuthUtil;
import services.MirrorServiceUtil;
import views.html.index;
import views.html.launch;
import views.html.timelineitems;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class Application extends Controller {


    /**
     * Default endpoint
     *
     * @return
     */
    @GlassSecurity.GlassAuthenticated
    public static Result index() {
        return ok(index.render());
    }


    /**
     * Relogin endpoint
     *
     * @return
     */
    public static Result relogin() {
        session().clear();
        return redirect(controllers.routes.GoogleClientOAuthController.oauth2Callback().absoluteURL(request()));
    }


    /**
     * Endpoint to display page where timeline card inserts can occur
     *
     * @return
     */
    @GlassSecurity.GlassAuthenticated
    public static Result launch() {
        return ok(launch.render());
    }


     /**
     * Endpoint to retrieve timeline items for the current user
     *
     * @return
     * @throws IOException
     */
    @GlassSecurity.GlassAuthenticated
    public static Result timelineItems() throws IOException{
        List<TimelineItem> timelineItemList =
            MirrorServiceUtil.listTimelineItems(
                GlassAuthUtil.getCredential(GlassAuthUtil.getUserId(session())),
                Play.application().configuration().getLong("max.timelines"));

        return ok(timelineitems.render(timelineItemList));
    }


    /**
     * Proxy the attachment from an external source for download purposes.
     *
     * @return
     * @throws IOException
     */
    public static Result fetchAttachment() throws IOException{
        String attachmentId = request().getQueryString("attachment");
        String timelineItemId = request().getQueryString("timelineItem");

        if (attachmentId == null || timelineItemId == null) {
            Logger.warn("attempted to load image attachment with missing IDs");
            return badRequest();
        }
        // identify the viewing user
        Credential credential = GlassAuthUtil.getCredential(GlassAuthUtil.getUserId(session()));

        // Get the content type
        String contentType =
            MirrorServiceUtil.getAttachmentContentType(credential, timelineItemId, attachmentId);

        // Get the attachment bytes
        InputStream attachmentInputStream =
            MirrorServiceUtil.getAttachmentInputStream(credential, timelineItemId, attachmentId);

        // Write it out
        return ok(attachmentInputStream);

    }



    /***
     * Inserts a Timeline card based on the input command
     *
     * @return
     */
    @GlassSecurity.GlassAuthenticated
    public static Promise<Result> insertTimelineItem() {

        Promise<Result> result = null;

        String[] commandParams = request().body().asFormUrlEncoded().get("command");

        String invalidCommandMsg = "Invalid command sent. Please see the usage for more help.";

        if (commandParams != null && commandParams.length > 0 ) {
            String command = commandParams[0];
            Logger.debug("inside insertimelineitem: command=" + command);

            //Command expects to be 3 parts
            String[] commandParts = command.split("\\s+");

            if (commandParts.length == 3) {
                try {
                    String ticker = commandParts[0].trim();
                    String investmentType = commandParts[1].trim().toUpperCase();
                    String investmentQuant = commandParts[2].trim().toLowerCase();

                    Configuration config = Play.application().configuration();
                    List<String> investmentTypes = config.getStringList("investment.types");
                    List<String> investmentQuants = config.getStringList("investment.quantifiers");

                    if (investmentTypes.contains(investmentType) &&
                            investmentQuants.contains(investmentQuant)) {

                        if (config.getString("investment.type.equity").equals(investmentType)) {
                            result = Promise.pure(generateEquityTimelineItem(ticker, investmentQuant));
                        }
                        else if (config.getString("investment.type.dividend").equals(investmentType)) {
                            result = generateDividendTimelineItem(ticker, investmentQuant);
                        }
                    }


                } catch (IOException ioe) {
                    Logger.error("Exception occurred while attempting to create a timeline", ioe);

                    result = Promise.pure((Result)badRequest("An unexpected error occurred while creating the timeline."));
                }
            }
            else {
                result =  Promise.pure((Result) badRequest(invalidCommandMsg));
            }
        }
        else {
            result =   Promise.pure((Result) badRequest(invalidCommandMsg));
        }

        return result;
    }


    /**
     * Removes a timeline item from the current user's timeline.
     *
     * @return
     * @throws IOException
     */
    @GlassSecurity.GlassAuthenticated
    public static Result deleteTimelineItem() throws IOException {

        MirrorServiceUtil.deleteTimelineItem(
            GlassAuthUtil.getCredential(GlassAuthUtil.getUserId(session())),
            request().body().asFormUrlEncoded().get("itemId")[0]);

        return redirect(controllers.routes.Application.timelineItems());
    }


    /**
     *  Invoked to generate a Timeline item for an equity command
     * @param ticker
     * @param investmentQuant
     *
     * @return
     * @throws IOException
     */
    private static Result generateEquityTimelineItem(String ticker, String investmentQuant) throws IOException {

        Configuration config = Play.application().configuration();

        URL url = new URL(
                MessageFormat.format(config.getString("chart.url.pattern"), ticker,
                        investmentQuant.equals("gip") ? "1d" : investmentQuant));

        TimelineItem timelineItem = new TimelineItem();
        timelineItem.setText("");
        timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

        //Define HTML
        StringBuilder builder = new StringBuilder();
        builder.append("<article>")

                .append("<img src=\"").append(url).append("\" width=\"100%\" height=\"100%\">")
                .append("<div class=\"overlay-gradient-tall-dark\"/>")
                .append("<footer class=\"green\">")
                .append("Symbol: ")
                .append(ticker.toUpperCase())
                .append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")
                .append("Chart: ")
                .append(investmentQuant.toUpperCase())
                .append("</footer>")
                .append("</article>");
        timelineItem.setHtml(builder.toString());

        List<MenuItem> menuItemList = new ArrayList<>();
        MenuItem deleteMenuItem = new MenuItem();
        deleteMenuItem.setAction("DELETE");
        menuItemList.add(deleteMenuItem);

        timelineItem.setMenuItems(menuItemList);

        MirrorServiceUtil.insertTimelineItem(
                GlassAuthUtil.getCredential(GlassAuthUtil.getUserId(session())), timelineItem, "image/png", url.openStream());

        return ok();
    }


    /**
     *  Invoked to generate a Timeline item for a dividend command
     *
     * @param ticker
     * @param investmentQuant
     * @return
     * @throws IOException
     */
    private static Promise<Result> generateDividendTimelineItem(final String ticker, final String investmentQuant) throws IOException {
        final Configuration config = Play.application().configuration();

        Logger.debug("ticker " + ticker + " invq " + investmentQuant);

        //Determine the Url to use
        StringBuilder quoteFields =  new StringBuilder();

        //Determine URL to invoke
        if (investmentQuant.equals(config.getString("investment.dividend.yield"))) {
            quoteFields.append("yr1");
        }
        else if (investmentQuant.equals(config.getString("investment.dividend.per.share"))) {
            quoteFields.append("dr1");
        }

        //Add the ticker company name attribute
        quoteFields.append("n");

        final Promise<Result> resultPromise = WS.url(config.getString("quotes.url"))
                .setQueryParameter("s", ticker)
                .setQueryParameter("f", quoteFields.toString())
                .get().map(response -> {

                    TimelineItem timelineItem = new TimelineItem();

                    List<MenuItem> menuItemList = new ArrayList<>();

                    String responseText = response.getBody();

                    Logger.debug("Response Text: " + responseText);

                    //split to extract results
                    String[] responseParts = responseText.split(",");

                    String htmlPattern = "";
                    if (investmentQuant.equals(config.getString("investment.dividend.yield"))) {
                        StringBuilder builder = new StringBuilder("");
                        builder.append("<article>")
                                .append("<section>")
                                .append("<div class=\"text-auto-size\">")
                                .append("<p class=\"align-center\">Dividend Yield</p>")
                                .append("</div>")
                                .append("<div class=\"layout-two-column\">")
                                .append("<div class=\"align-center\">")
                                .append("<p class=\"text-x-large\">{0}</p>")
                                .append("</div>")
                                .append("<div class=\"align-center\">")
                                .append("<p class=\"text-x-large green\">{1}%</p>")
                                .append("</div>")
                                .append("</div>")
                                .append("</section>")
                                .append("<footer>")
                                .append("<div>Payed on {2}</div>")
                                .append("</footer>")
                                .append("</article>");
                        htmlPattern = builder.toString();

                        timelineItem.setSpeakableText("The current dividend yield for " + responseParts[2] + " is " + responseParts[0] + " percent. " +
                            "The dividend was last payed on " + responseParts[1]);

                    }
                    else if (investmentQuant.equals(config.getString("investment.dividend.per.share"))) {
                        StringBuilder builder = new StringBuilder("");
                        builder.append("<article>")
                                .append("<section>")
                                .append("<div class=\"text-auto-size\">")
                                .append("<p class=\"align-center\">Dividend per Share</p>")
                                .append("</div>")
                                .append("<div class=\"layout-two-column\">")
                                .append("<div class=\"align-center\">")
                                .append("<p class=\"text-x-large\">{0}</p>")
                                .append("</div>")
                                .append("<div class=\"align-center\">")
                                .append("<p class=\"text-x-large green\">${1}</p>")
                                .append("</div>")
                                .append("</div>")
                                .append("</section>")
                                .append("<footer>")
                                .append("<div>Payed on {2}</div>")
                                .append("</footer>")
                                .append("</article>");
                        htmlPattern = builder.toString();

                        //slight number format adjustment to the dividend per shar amount
                        String amount = "";
                        try {
                            MessageFormat speakableTextFormat = new MessageFormat("The dividend per share for {0} is ${1,number,currency}. " +
                                    "This amount was last payed on {2}");
                            Object[] args = {responseParts[2], new Double(responseParts[0]), responseParts[1]};

                            timelineItem.setSpeakableText(speakableTextFormat.format(args));

                        } catch (Exception e) {
                            Logger.error("Error occured while parsing the response parts:" + responseParts);
                            throw e;
                        }

                    }

                    timelineItem.setHtml(MessageFormat.format(htmlPattern,
                            ticker.toUpperCase(), responseParts[0], responseParts[1]));
                    timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

                    MenuItem readAloudMenuItem = new MenuItem();
                    readAloudMenuItem .setAction("READ_ALOUD");
                    menuItemList.add(readAloudMenuItem);

                    MenuItem deleteMenuItem = new MenuItem();
                    deleteMenuItem.setAction("DELETE");
                    menuItemList.add(deleteMenuItem);

                    timelineItem.setMenuItems(menuItemList);

                    MirrorServiceUtil.insertTimelineItem(
                            GlassAuthUtil.getCredential(GlassAuthUtil.getUserId(session())), timelineItem);

                    return ok();
            }
        );

        return resultPromise;
    }



}
