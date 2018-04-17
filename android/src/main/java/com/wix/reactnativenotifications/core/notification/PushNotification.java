package com.wix.reactnativenotifications.core.notification;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.bridge.ReactContext;
import com.wix.reactnativenotifications.Defs;
import com.wix.reactnativenotifications.core.AppLaunchHelper;
import com.wix.reactnativenotifications.core.AppLifecycleFacade;
import com.wix.reactnativenotifications.core.AppLifecycleFacade.AppVisibilityListener;
import com.wix.reactnativenotifications.core.AppLifecycleFacadeHolder;
import com.wix.reactnativenotifications.core.InitialNotificationHolder;
import com.wix.reactnativenotifications.core.JsIOHelper;
import com.wix.reactnativenotifications.core.NotificationIntentAdapter;
import com.wix.reactnativenotifications.core.NotificationScheduler;
import com.wix.reactnativenotifications.core.ProxyService;

import static com.wix.reactnativenotifications.Defs.NOTIFICATION_OPENED_EVENT_NAME;
import static com.wix.reactnativenotifications.Defs.NOTIFICATION_RECEIVED_EVENT_NAME;

public class PushNotification implements IPushNotification {

    final protected Context mContext;
    final protected AppLifecycleFacade mAppLifecycleFacade;
    final protected AppLaunchHelper mAppLaunchHelper;
    final protected JsIOHelper mJsIOHelper;
    final protected PushNotificationProps mNotificationProps;
    final protected AppVisibilityListener mAppVisibilityListener = new AppVisibilityListener() {
        @Override
        public void onAppVisible() {
            mAppLifecycleFacade.removeVisibilityListener(this);
            dispatchImmediately();
        }

        @Override
        public void onAppNotVisible() {
        }
    };

    public static IPushNotification get(Context context, Bundle bundle) {
        Context appContext = context.getApplicationContext();
        if (appContext instanceof INotificationsApplication) {
            return ((INotificationsApplication) appContext).getPushNotification(context, bundle, AppLifecycleFacadeHolder.get(), new AppLaunchHelper());
        }
        return new PushNotification(context, bundle, AppLifecycleFacadeHolder.get(), new AppLaunchHelper(), new JsIOHelper());
    }

    protected PushNotification(Context context, Bundle bundle, AppLifecycleFacade appLifecycleFacade, AppLaunchHelper appLaunchHelper, JsIOHelper JsIOHelper) {
        mContext = context;
        mAppLifecycleFacade = appLifecycleFacade;
        mAppLaunchHelper = appLaunchHelper;
        mJsIOHelper = JsIOHelper;
        mNotificationProps = createProps(bundle);
    }

    @Override
    public void onReceived() throws InvalidNotificationException {
        postNotification(null);
        notifyReceivedToJS();
    }

    @Override
    public void onOpened() {
        digestNotification();
        clearAllNotifications();
    }

    @Override
    public int onPostRequest(Integer notificationId) {
        return postNotification(notificationId);
    }

    @Override
    public int onProjectSchedulerRequest(Integer notificationId) {
        return postNotificationSchedule(notificationId);
    }

    @Override
    public PushNotificationProps asProps() {
        return mNotificationProps.copy();
    }

    protected int postNotification(Integer notificationId) {
        final PendingIntent pendingIntent = getCTAPendingIntent();
        final Notification notification = buildNotification(pendingIntent);
        return postNotification(notification, notificationId);
    }

    protected int postNotificationSchedule(Integer notificationId) {
        final PendingIntent pendingIntent = getCTAPendingIntent();
        final Notification notification = buildNotification(pendingIntent);
        return postNotificationSchedule(notification, notificationId);
    }

    protected void digestNotification() {
        if (!mAppLifecycleFacade.isReactInitialized()) {
            setAsInitialNotification();
            launchOrResumeApp();
            return;
        }

        final ReactContext reactContext = mAppLifecycleFacade.getRunningReactContext();
        if (reactContext.getCurrentActivity() == null) {
            setAsInitialNotification();
        }

        if (mAppLifecycleFacade.isAppVisible()) {
            dispatchImmediately();
        } else {
            dispatchUponVisibility();
        }
    }

    protected PushNotificationProps createProps(Bundle bundle) {
        return new PushNotificationProps(bundle);
    }

    protected void setAsInitialNotification() {
        InitialNotificationHolder.getInstance().set(mNotificationProps);
    }

    protected void dispatchImmediately() {
        notifyOpenedToJS();
    }

    protected void dispatchUponVisibility() {
        mAppLifecycleFacade.addVisibilityListener(getIntermediateAppVisibilityListener());

        // Make the app visible so that we'll dispatch the notification opening when visibility changes to 'true' (see
        // above listener registration).
        launchOrResumeApp();
    }

    protected AppVisibilityListener getIntermediateAppVisibilityListener() {
        return mAppVisibilityListener;
    }

    protected PendingIntent getCTAPendingIntent() {
        final Intent cta = new Intent(mContext, ProxyService.class);
        return NotificationIntentAdapter.createPendingNotificationIntent(mContext, cta, mNotificationProps);
    }

    protected Notification buildNotification(PendingIntent intent) {
        return getNotificationBuilder(intent).build();
    }

    protected Notification.Builder getNotificationBuilder(PendingIntent intent) {
        String ticker = mNotificationProps.getTicker();
        String largeIcon = mNotificationProps.getLargeIcon();
        String smallIcon = mNotificationProps.getSmallIcon();
        String subText = mNotificationProps.getSubText();
        String group = mNotificationProps.getGroup();
        Resources resources = mContext.getResources();
        String packageName = mContext.getPackageName();


        Notification.Builder notiBuilder = new Notification.Builder(mContext)
                .setContentTitle(mNotificationProps.getTitle())
                .setContentText(mNotificationProps.getBody())
                .setContentIntent(intent)
                .setNumber(mNotificationProps.getNumber())
                .setAutoCancel(mNotificationProps.getAutoCancel());

                // set small icon and large icon for notification.
                int smallIconResId;
                int largeIconResId;
                if (smallIcon != null) {
                    smallIconResId = resources.getIdentifier(smallIcon, "mipmap", packageName);
                } else {
                    smallIconResId = resources.getIdentifier("ic_notification", "mipmap", packageName);
                }

                if (smallIconResId == 0) {
                    smallIconResId = resources.getIdentifier("ic_launcher", "mipmap", packageName);
    
                    if (smallIconResId == 0) {
                        smallIconResId = android.R.drawable.ic_dialog_info;
                    }
                }
                notiBuilder.setSmallIcon(smallIconResId);

                if (largeIcon != null) {
                    largeIconResId = resources.getIdentifier(largeIcon, "mipmap", packageName);
                } else {
                    largeIconResId = resources.getIdentifier("ic_launcher", "mipmap", packageName);
                }

                Bitmap largeIconBitmap = BitmapFactory.decodeResource(resources, largeIconResId);

                if (largeIconResId != 0 && (largeIcon != null || Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {
                    notiBuilder.setLargeIcon(largeIconBitmap);
                }

                if (ticker != null) {
                    notiBuilder.setTicker(ticker);
                }

                if (group != null) {
                    notiBuilder.setGroup(group);
                }

                if (subText != null) {
                    notiBuilder.setSubText(subText);
                }

                if (mNotificationProps.getVibrate() == true) {
                    notiBuilder.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 });
                }

                if (mNotificationProps.getEnableLights() == true) {
                    notiBuilder.setLights(android.graphics.Color.BLUE, 3000, 3000);
                }

                if (mNotificationProps.getSilent() == false) {
                    Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    String soundName = mNotificationProps.getSoundName();
                    if (soundName != null) {
                        if (!"default".equalsIgnoreCase(soundName)) {
    
                            // sound name can be full filename, or just the resource name.
                            // So the strings 'my_sound.mp3' AND 'my_sound' are accepted
                            // The reason is to make the iOS and android javascript interfaces compatible
    
                            int soundId = resources.getIdentifier(soundName, "raw", packageName);
                            if (soundId == 0) {
                                soundName = soundName.substring(0, soundName.lastIndexOf('.'));
                                soundId = resources.getIdentifier(soundName, "raw", packageName);
                            } 
                            soundUri = soundId == 0 ? soundUri : Uri.parse("android.resource://" + packageName + "/" + soundId);
                        }
                    }
                    notiBuilder.setSound(soundUri);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    notiBuilder.setCategory(Notification.CATEGORY_CALL);
    
                    String color = mNotificationProps.getColor();
                    if (color != null) {
                        notiBuilder.setColor(Color.parseColor(color));
                    }
                }

        return notiBuilder;
    }

    protected int postNotification(Notification notification, Integer notificationId) {
        int id = notificationId != null ? notificationId : createNotificationId(notification);
        postNotification(id, notification);
        return id;
    }

    protected void postNotification(int id, Notification notification) {
        final NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
    }

    public int postNotificationSchedule(Notification notification,  Integer notificationId) {

        long fireDate = mNotificationProps.getFireDate();
        long interval = mNotificationProps.getRepeatInterval();
        
        if (fireDate == 0) {
            Log.e(Defs.LOGTAG, "No date specified for the scheduled notification");
            return notificationId;
        }
        
        Intent notificationIntent = new Intent(mContext, NotificationScheduler.class);
        notificationIntent.putExtra(NotificationScheduler.NOTIFICATION_ID, notificationId);
        notificationIntent.putExtra(NotificationScheduler.NOTIFICATION, notification);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            mContext, 
            notificationId, 
            notificationIntent, 
            PendingIntent.FLAG_CANCEL_CURRENT
            );

        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        if (interval != -1) {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    fireDate,
                    interval,
                    pendingIntent
            ); 
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                fireDate,
                pendingIntent
            );
        }

        return notificationId;
    }



    protected void clearAllNotifications() {
        final NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    protected int createNotificationId(Notification notification) {
        return (int) System.nanoTime();
    }

    private void notifyReceivedToJS() {
        mJsIOHelper.sendEventToJS(NOTIFICATION_RECEIVED_EVENT_NAME, mNotificationProps.asBundle(), mAppLifecycleFacade.getRunningReactContext());
    }

    private void notifyOpenedToJS() {
        mJsIOHelper.sendEventToJS(NOTIFICATION_OPENED_EVENT_NAME, mNotificationProps.asBundle(), mAppLifecycleFacade.getRunningReactContext());
    }

    protected void launchOrResumeApp() {
        final Intent intent = mAppLaunchHelper.getLaunchIntent(mContext);
        mContext.startActivity(intent);
    }
}
