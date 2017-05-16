package org.geospaces.schas.UtilityObjectClasses;

/**
 * Created by Erik on 4/18/2017.
 */

public class FBNotification {
    private String notificationBody;
    private String notificationTitle;
    private String notificationTag;
    private long notificationSentTimeMillis;

    public FBNotification(String notificationBody, String notificationTitle, String notificationTag, long notificationSentTimeMillis){
        this.notificationBody = notificationBody;
        this.notificationTitle = notificationTitle;
        this.notificationTag = notificationTag;
        this.notificationSentTimeMillis = notificationSentTimeMillis;
    }

    public String getNotificationBody() {
        return notificationBody;
    }

    public String getNotificationTitle() {
        return notificationTitle;
    }

    public String getNotificationTag() {
        return notificationTag;
    }

    public long getNotificationSentTimeMillis() {
        return notificationSentTimeMillis;
    }
}
