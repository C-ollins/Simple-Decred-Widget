package com.brentpanther.cryptowidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class WidgetProvider extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Ids ids = WidgetApplication.getInstance().getIds();
        for (int widgetId : appWidgetIds) {
            Prefs prefs = WidgetApplication.getInstance().getPrefs(widgetId);
            int layout = prefs.getThemeLayout();
            RemoteViews views = new RemoteViews(context.getPackageName(), layout);
            views.setViewVisibility(ids.bottomText(), GONE);
            views.setViewVisibility(ids.topText(), GONE);
            views.setViewVisibility(ids.price(), GONE);
            views.setViewVisibility(ids.image(), GONE);
            views.setViewVisibility(ids.imageSmall(), GONE);
            views.setViewVisibility(ids.loading(), VISIBLE);
            Intent i = new Intent(context, PriceBroadcastReceiver.class);
            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            PendingIntent pi = PendingIntent.getBroadcast(context, widgetId, i, 0);
            views.setOnClickPendingIntent(ids.parent(), pi);
            appWidgetManager.updateAppWidget(widgetId, views);
            L.l("Setting Alarm 1");
            setAlarm(context, widgetId);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        Prefs prefs = WidgetApplication.getInstance().getPrefs(appWidgetId);
        Ids ids = WidgetApplication.getInstance().getIds();
        int layout = prefs.getThemeLayout();
        RemoteViews views = new RemoteViews(context.getPackageName(), layout);
        WidgetViews.setLastText(context, views, appWidgetId);
        Intent i = new Intent(context, PriceBroadcastReceiver.class);
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pi = PendingIntent.getBroadcast(context, appWidgetId, i, 0);
        views.setOnClickPendingIntent(ids.parent(), pi);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		for (int widgetId : appWidgetIds) {
        	Prefs prefs = WidgetApplication.getInstance().getPrefs(widgetId);
			prefs.delete();
			Intent i = new Intent(context, PriceBroadcastReceiver.class);
            i.setAction("FALSE_CLICK");
			PendingIntent pi = PendingIntent.getBroadcast(context, widgetId + 1000, i, 0);
			AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			alarm.cancel(pi);
		}
	}
	
	private void setAlarm(Context context, int widgetId) {
        Prefs prefs = WidgetApplication.getInstance().getPrefs(widgetId);
		AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, PriceBroadcastReceiver.class);
        i.setAction("FALSE_CLICK");
		i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		PendingIntent pi = PendingIntent.getBroadcast(context,widgetId + 1000, i, 0);
		int update = prefs.getInterval();
        if(update > 0 && alarm != null) {
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, update * 60000, update * 60000, pi);
        }
		//alarm.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis()+1000, update * 60000, pi);
	}

}
