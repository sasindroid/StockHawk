package com.sam_chordas.android.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.StackView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.ui.ChartActivity;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

public class StockListWidgetProvider extends AppWidgetProvider {

    public final String TAG = StockListWidgetProvider.class.getSimpleName();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.stock_list_widget);

            // Create an Intent to launch MainActivity
            Intent intent = new Intent(context, MyStocksActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            // Set the Remote adapter.
            views.setRemoteAdapter(R.id.widget_list, new Intent(context, StockRemoveViewService.class));

            PendingIntent clickPendingIntentTemplate = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(new Intent(context, ChartActivity.class))
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            /**
             * When using collections (eg. {@link ListView}, {@link StackView} etc.) in widgets, it is very
             * costly to set PendingIntents on the individual items, and is hence not permitted. Instead
             * this method should be used to set a single PendingIntent template on the collection, and
             * individual items can differentiate their on-click behavior using
             * {@link RemoteViews#setOnClickFillInIntent(int, Intent)}.
             */
            views.setPendingIntentTemplate(R.id.widget_list, clickPendingIntentTemplate);
            views.setEmptyView(R.id.widget_list, R.id.widget_empty);


            views.setEmptyView(R.id.widget_list, R.id.widget_empty);

            // Tell the appwidgetmanager to perform an update on the current app widget.
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Log.d(TAG, "onReceive called");

        if (StockTaskService.ACTION_DATA_UPDATED.equals(intent.getAction())) {

            Log.d(TAG, "onReceive: " + StockTaskService.ACTION_DATA_UPDATED);

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, getClass()));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);
        }
    }
}

