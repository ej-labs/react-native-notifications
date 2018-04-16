package com.wix.reactnativenotifications.core;

import android.content.Intent;
import android.content.BroadcastReceiver;
// import android.util.Log;
import android.app.NotificationManager;
import android.content.Context;


public class NotificationScheduler extends BroadcastReceiver {

  public static String NOTIFICATION_ID = "notification_id";
  public static String NOTIFICATION = "notification";

  @Override
  public void onReceive(final Context context, Intent intent) {

      final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

      Notification notification = intent.getParcelableExtra(NOTIFICATION);
      int notificationId = intent.getIntExtra(NOTIFICATION_ID, 0);
      notificationManager.notify(notificationId, notification);
  }
}
