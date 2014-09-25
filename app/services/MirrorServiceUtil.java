package services;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.mirror.Mirror;
import com.google.api.services.mirror.model.*;
import com.google.common.io.ByteStreams;
import play.Logger;
import play.Play;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MirrorServiceUtil {

    /**
     * Gets access to the Mirror service class
     *
     * @param credential
     * @return
     */
    public static Mirror getMirror(Credential credential) {
        return new Mirror.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
                .setApplicationName(Play.application().configuration().getString("applicationName")).build();
    }


    /**
     * Inserts a contact representing the current application. Will allow registered Glass wearer
     * to share content with our glassware from other glassware
     *
     * @param credential
     * @param contact
     * @return
     * @throws IOException
     */
    public static Contact insertContact(Credential credential, Contact contact) throws IOException {
        Mirror.Contacts contacts = getMirror(credential).contacts();
        return contacts.insert(contact).execute();
    }


    /**
     * List the contacts
     *
     * @param credential
     * @return
     * @throws IOException
     */
    public static ContactsListResponse listContacts(Credential credential) throws IOException {
        Mirror.Contacts contacts = getMirror(credential).contacts();
        return contacts.list().execute();
    }


    /**
     * Get the contact identified as ID
     * @param credential
     * @param id
     * @return
     * @throws IOException
     */
    public static Contact getContact(Credential credential, String id) throws IOException {
        try {
            Mirror.Contacts contacts = getMirror(credential).contacts();
            return contacts.get(id).execute();
        } catch (GoogleJsonResponseException e) {
            Logger.warn("Could not find contact with ID " + id);
            return null;
        }
    }


    /**
     * Inserts a simple timeline item.
     *
     * @param credential the user's credential
     * @param item       the item to insert
     */
    public static TimelineItem insertTimelineItem(Credential credential, TimelineItem item)
            throws IOException {
        return getMirror(credential).timeline().insert(item).execute();
    }

    /**
     * Inserts an item with an attachment provided as a byte array.
     *
     * @param credential            the user's credential
     * @param item                  the item to insert
     * @param attachmentContentType the MIME type of the attachment (or null if
     *                              none)
     * @param attachmentData        data for the attachment (or null if none)
     */
    public static void insertTimelineItem(Credential credential, TimelineItem item,
                                          String attachmentContentType, byte[] attachmentData) throws IOException {
        Mirror.Timeline timeline = getMirror(credential).timeline();
        timeline.insert(item, new ByteArrayContent(attachmentContentType, attachmentData)).execute();

    }

    /**
     * Inserts an item with an attachment provided as an input stream.
     *
     * @param credential            the user's credential
     * @param item                  the item to insert
     * @param attachmentContentType the MIME type of the attachment (or null if
     *                              none)
     * @param attachmentInputStream input stream for the attachment (or null if
     *                              none)
     */
    public static void insertTimelineItem(Credential credential, TimelineItem item,
                                          String attachmentContentType, InputStream attachmentInputStream) throws IOException {
        insertTimelineItem(credential, item, attachmentContentType,
                ByteStreams.toByteArray(attachmentInputStream));
    }


    /**
     * Gets a list of timeline items
     *
     * @param credential            the user's credential
     * @param count                 the max number of timeline items to return
     * @return  the list of timeline items
     * @throws IOException
     */
    public static List<TimelineItem>  listTimelineItems(Credential credential, long count) throws IOException {
        Mirror.Timeline timeline = getMirror(credential).timeline();

        Mirror.Timeline.List timelineList = timeline.list();
        timelineList.setMaxResults(count);

        return timelineList.execute().getItems();
    }


    public static InputStream getAttachmentInputStream(Credential credential, String timelineItemId,
                                                       String attachmentId) throws IOException {
        Mirror mirrorService = getMirror(credential);
        Mirror.Timeline.Attachments attachments = mirrorService.timeline().attachments();
        Attachment attachmentMetadata = attachments.get(timelineItemId, attachmentId).execute();
        HttpResponse resp =
                mirrorService.getRequestFactory()
                        .buildGetRequest(new GenericUrl(attachmentMetadata.getContentUrl())).execute();
        return resp.getContent();
    }

    public static String getAttachmentContentType(Credential credential, String timelineItemId,
                                                  String attachmentId) throws IOException {
        Mirror.Timeline.Attachments attachments = getMirror(credential).timeline().attachments();
        Attachment attachmentMetadata = attachments.get(timelineItemId, attachmentId).execute();
        return attachmentMetadata.getContentType();
    }

    public static void deleteTimelineItem(Credential credential, String timelineItemId) throws IOException {
        getMirror(credential).timeline().delete(timelineItemId).execute();
    }


    /**
     * Subscribes to notifications on the user's timeline.
     */
    public static Subscription insertSubscription(Credential credential, String callbackUrl,
                                                  String userId, String collection) throws IOException {
        Logger.info("Attempting to subscribe verify_token " + userId + " with callback " + callbackUrl);

        Subscription subscription = new Subscription();

        // Alternatively, subscribe to "locations"
        subscription.setCollection(collection);
        subscription.setCallbackUrl(callbackUrl);
        subscription.setUserToken(userId);

        return getMirror(credential).subscriptions().insert(subscription).execute();
    }

    /**
     * Subscribes to notifications on the user's timeline.
     */
    public static void deleteSubscription(Credential credential, String id) throws IOException {
        getMirror(credential).subscriptions().delete(id).execute();
    }

}
