package com.brentpanther.cryptowidget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.widget.RemoteViews;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;

/**
 * Created by Panther on 2/8/2017.
 */

public class BackgroundService extends Service {

    private Context context;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        this.context = this;
    }

    private final class MyRunnable implements Runnable {

        private final int appWidgetId;
        private boolean manualRefresh;

        MyRunnable(int appWidgetId, boolean manualRefresh) {
            this.appWidgetId = appWidgetId;
            this.manualRefresh = manualRefresh;
        }

        public void run() {
            Ids ids = WidgetApplication.getInstance().getIds();
            Prefs prefs = WidgetApplication.getInstance().getPrefs(appWidgetId);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int layout = prefs.getThemeLayout();
            RemoteViews views = new RemoteViews(context.getPackageName(), layout);
            if (manualRefresh) WidgetViews.setLoading(views, ids, appWidgetId);
            Intent i = new Intent(context, PriceBroadcastReceiver.class);
            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent pi = PendingIntent.getBroadcast(context, appWidgetId, i, 0);
            views.setOnClickPendingIntent(ids.parent(), pi);
            appWidgetManager.updateAppWidget(appWidgetId, views);
            Exchange provider = prefs.getExchange();
            try {
                String json = provider.getValue();
                L.l("JSON FROM BACKGROUND SERVICE: "+json);
                DCRJson dcr = new DCRJson(json);
                int type = prefs.getType();
                String text;
                switch (type){
                    case 0:
                        text = dcr.getBtcPrice();
                        break;
                    case 1:
                        text = "$"+dcr.getUsdPrice();
                        break;
                    case 2:
                        text = dcr.getTicketPrice();
                        break;
                    case 3:
                        text = dcr.getEstNextPrice();
                        break;
                    case 4:
                        text = new ChangeTime(dcr.getPriceChangeInSeconds()).format();
                        break;
                    case 5:
                        text = dcr.getDifficulty();
                        break;
                    case 6:
                        double networkHash = dcr.getNetworkHash();
                        text = new HashRate(networkHash).format();
                        break;
                    default:
                        text = "--";
                }
                WidgetViews.setText(context, views, text, appWidgetId);
                prefs.setLastUpdate();
            } catch (Exception e) {
                L.l("Widget Type: EXCEPTION: "+e.getMessage());
                long lastUpdate = prefs.getLastUpdate();
                int interval = prefs.getInterval();
                //if its been "a while" since the last successful update, gray out the icon.
                boolean isOld = ((System.currentTimeMillis() - lastUpdate) > 1000 * 90 * interval);
                boolean hideIcon = prefs.getIcon();
                WidgetViews.setOld(views, isOld, ids, hideIcon);
                WidgetViews.setLastText(context, views, appWidgetId);
            }
            Intent priceUpdate = new Intent(context, PriceBroadcastReceiver.class);
            priceUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            priceUpdate.putExtra("manualRefresh", true);
            PendingIntent pendingPriceUpdate = PendingIntent.getBroadcast(context, appWidgetId, priceUpdate, FLAG_CANCEL_CURRENT);
            views.setOnClickPendingIntent(ids.parent(), pendingPriceUpdate);
            appWidgetManager.updateAppWidget(appWidgetId, views);
            stopSelf();
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        int appWidgetId = intent.getIntExtra("appWidgetId", 0);
        boolean manualRefresh = intent.getBooleanExtra("manualRefresh", false);
        new Thread(new MyRunnable(appWidgetId, manualRefresh)).start();
        return START_REDELIVER_INTENT;
    }
}