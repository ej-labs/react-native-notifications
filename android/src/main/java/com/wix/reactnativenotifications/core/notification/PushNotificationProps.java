package com.wix.reactnativenotifications.core.notification;

import com.wix.reactnativenotifications.Defs;

import android.os.Bundle;
import android.util.Log;

public class PushNotificationProps {

    protected Bundle mBundle;

    public PushNotificationProps() {
        mBundle = new Bundle();
    }

    public PushNotificationProps(
            String title,
            String body,
            String fireDate,
            String repeatInterval
    ) {
        mBundle = new Bundle();
        mBundle.putString("title", title);
        mBundle.putString("body", body);
        mBundle.putString("fireDate", fireDate);
        mBundle.putString("repeatInterval", repeatInterval);
    }

    public PushNotificationProps(Bundle bundle) {
        mBundle = bundle;
    }

    private static long buildRepeatTimeMilis(long interval) {
        if (interval < 10000l) {
            Log.e(Defs.LOGTAG, "notifications interval lowest value is 10s, your interval option will be set to 10s");
            return 10000l;
        }
        return interval;
    }

    private static long buildRepeatTimeMilis(String interval) {

        if (interval == null) {
            Log.e(Defs.LOGTAG, "repeatInterval not exist");
            return -1l;
        }

        switch (interval) {
            case "minute": 
                return 60000l;

            case "hour": 
                return 3600000l;
        
            case "day":
                return 86400000l;

            case "week":
                return 604800000l;

            default:
            try {
                return buildRepeatTimeMilis(Long.parseLong(interval));
            } catch (NumberFormatException e) {
                Log.e(Defs.LOGTAG, "parse interval error");
                return -1l;
            } catch (Exception e) {
                Log.e(Defs.LOGTAG, "undeclared interval for scheduler notifications");
                return -1l;
            }
        }
    }

    public String getTitle() {
        return mBundle.getString("title");
    }

    public String getBody() {
        return mBundle.getString("body");
    }

    public boolean getAutoCancel(){
        Boolean autoCancel = mBundle.getBoolean("autoCancel");
        return autoCancel != null ? autoCancel : true;
    }

    public int getNumber() {
        Double number = mBundle.getDouble("number");
        int _intNumber = number != null ? number.intValue() : 0;
        return _intNumber > 1 ? _intNumber : 1;
    }

    public String getLargeIcon() {
        return mBundle.getString("largeIcon");
    }

    public String getSmallIcon() {
        return mBundle.getString("smallIcon");
    }

    public Boolean getEnableLights() {
        return mBundle.getBoolean("enableLights");
    }

    public String getSubText() {
        return mBundle.getString("subText");
    }

    public Boolean getVibrate() {
        return mBundle.getBoolean("vibrate");
    }

    // public String getTag() {
    //     return mBundle.getString("tag");
    // }

    public String getGroup() {
        return mBundle.getString("group");
    }

    public String getTicker() {
        return mBundle.getString("ticker");
    }

    public String getSoundName() {
        return mBundle.getString("soundName");
    }

    public String getCategory() {
        return mBundle.getString("category");
    }

    public String getColor() {
        return mBundle.getString("color");
    }

    public Boolean getSilent() {
        Boolean silent =  mBundle.getBoolean("silent");
        return silent != null ? silent : false;
    }


    public long getFireDate() {
        try {
            double fireDate =  mBundle.getDouble("fireDate");
            return Double.valueOf(fireDate).longValue();
        } catch (NumberFormatException e) {
            Log.e(Defs.LOGTAG, "parse fire date error");
            return 0l;
        }
    }

    public long getRepeatInterval() {
        String interval =  mBundle.getString("repeatInterval");
        return buildRepeatTimeMilis(interval);
    }

    public Bundle asBundle() {
        return (Bundle) mBundle.clone();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(1024);
        for (String key : mBundle.keySet()) {
            sb.append(key).append("=").append(mBundle.get(key)).append(", ");
        }
        return sb.toString();
    }

    protected PushNotificationProps copy() {
        return new PushNotificationProps((Bundle) mBundle.clone());
    }
}
