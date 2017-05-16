package org.geospaces.schas.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.geospaces.schas.R;
import org.geospaces.schas.UtilityObjectClasses.FBNotification;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Erik on 4/23/2017.
 */

public class NotificationListAdapter extends ArrayAdapter<FBNotification> {
    private Context context;
    private List<FBNotification> notifications;

    public NotificationListAdapter(Context context, List<FBNotification> notifications) {
        super(context, -1, notifications);
        this.context = context;
        this.notifications = notifications;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.notification_log_list_item, parent, false);

        TextView titleText = (TextView) rowView.findViewById(R.id.notificationTitle);
        titleText.setText(notifications.get(position).getNotificationTitle());

        TextView timeText = (TextView) rowView.findViewById(R.id.notificationSentTime);
        timeText.setText(convertTimeInMillisToDateString(notifications.get(position).getNotificationSentTimeMillis()));

        TextView tagText = (TextView) rowView.findViewById(R.id.notificationTagText);
        tagText.setText(notifications.get(position).getNotificationTag());

        TextView bodyText = (TextView)  rowView.findViewById(R.id.notificationBodyText);
        bodyText.setText(notifications.get(position).getNotificationBody());

        return rowView;
    }

    public String convertTimeInMillisToDateString(long millis){
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm dd/MM/yyyy");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return timeFormat.format(calendar.getTime());
    }
}
