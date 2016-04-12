package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

//import com.melnykov.fab.FloatingActionButton;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    private CharSequence mTitle;
    private Intent mServiceIntent;
    private ItemTouchHelper mItemTouchHelper;
    private static final int CURSOR_LOADER_ID = 0;
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;
    private Cursor mCursor;

    FloatingActionButton fab;
    CoordinatorLayout parentLayout;

    private static final int PREFS_VALUE_ALREADY_READ = 0;

    public static final String STOCK_SYMBOL = "STOCK_SYMBOL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

//        setContentView(R.layout.activity_my_stocks);
        setContentView(R.layout.activity_my_stocks_new);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(mTitle);

        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        mServiceIntent = new Intent(this, StockIntentService.class);

        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra("tag", "init");

            if (Utils.isNetworkAvailable(this)) {
                startService(mServiceIntent);
            } else {
                Utils.setStockStatus(this, StockTaskService.NETWORK_NOT_AVAILABLE, true);
//                networkToast();
            }
        }

        parentLayout = (CoordinatorLayout) findViewById(R.id.parentLayout);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        // Set a layout for empty view.
        View emptyView = findViewById(R.id.recyclerview_stock_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mCursorAdapter = new QuoteCursorAdapter(this, null, emptyView);

        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {

                        String stockSymbol = ((TextView) v.findViewById(R.id.stock_symbol)).getText().toString();

                        Intent chartIntent = new Intent(mContext, ChartActivity.class) ;
                        chartIntent.putExtra(ChartFragment.STOCK_SYMBOL, stockSymbol);

                        // Show a graph using a library.
                        startActivity(chartIntent);

                    }
                }));

        recyclerView.setAdapter(mCursorAdapter);


        fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.attachToRecyclerView(recyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.isNetworkAvailable(mContext)) {
                    new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                            .content(R.string.content_test)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {

                                    // Check if no data is entered.
                                    if(input != null && input.toString().trim().length() == 0) {
                                        showSnack(parentLayout, getString(R.string.enter_valid_symbol));
                                        return;
                                    }

                                    // On FAB click, receive user input. Make sure the stock doesn't already exist
                                    // in the DB and proceed accordingly
                                    Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                            new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                            new String[]{input.toString().toUpperCase()}, null);

                                    Log.d("MyStocksActivity", "ADD Cursor count: " + (c != null ? c.getCount() : null));

                                    if (c != null && c.getCount() != 0) {

                                        showSnack(parentLayout, getString(R.string.stock_already_saved));

                                        return;
                                    } else {
                                        // Add the stock to DB
                                        mServiceIntent.putExtra("tag", "add");
                                        mServiceIntent.putExtra("symbol", input.toString());
                                        startService(mServiceIntent);
                                    }

                                    if (c != null && !c.isClosed()) {
                                        c.close();
                                    }

                                }
                            })

                            .show();
                } else {
                    networkToast();
                }

            }
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        mTitle = getTitle();
        if (Utils.isNetworkAvailable(this)) {
            long period = 3600L;
            long flex = 10L;
            String periodicTag = "periodic";

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }
    }


    @Override
    public void onResume() {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);

        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    @Override
    protected void onPause() {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.unregisterOnSharedPreferenceChangeListener(this);

        super.onPause();
    }

    public void networkToast() {
//        Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
        showSnack(parentLayout, getString(R.string.network_toast));
    }

//    public void restoreActionBar() {
//        ActionBar actionBar = getSupportActionBar();
//        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
//        actionBar.setDisplayShowTitleEnabled(true);
//        actionBar.setTitle(mTitle);
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
//        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            Utils.showPercent = !Utils.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
            return true;
        }

        if (id == R.id.action_refresh) {

            actionOnNotConnected();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void actionOnNotConnected() {

        if (Utils.isNetworkAvailable(this)) {
            mServiceIntent = new Intent(this, StockIntentService.class);
            mServiceIntent.putExtra("tag", "periodic");
            startService(mServiceIntent);
        } else {
            ContentValues contentValues = new ContentValues();
            // update ISCURRENT to 0 (false) so new data is current

            contentValues.put(QuoteColumns.ISCURRENT, 0);
            mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                    null, null);

            Utils.setStockStatus(this, StockTaskService.NETWORK_NOT_AVAILABLE, true);

            getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        Log.d("MyStocksActivity", "Invoked onCreateLoader!");

        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        Log.d("MyStocksActivity", "Invoked onLoadFinished! data count: " + data.getCount());

        mCursorAdapter.swapCursor(data);

        // Show/hide the empty view based on the flag in Prefs.

        updateEmptyView(data != null ? data.getCount() : 0);

        mCursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }


    /*
    Updates the empty list view with contextually relevant information that the user can
    use to determine why they aren't seeing stock details.
 */
    private void updateEmptyView(int cnt) {

        if(cnt > 0) {
            return;
        }

        TextView tv = (TextView) findViewById(R.id.recyclerview_stock_empty);
        if (null != tv) {

            int message = R.string.empty_stock_list;

            int stock = Utils.getStockStatus(mContext, true);

            switch (stock) {
                case StockTaskService.STOCK_STATUS_SERVER_DOWN:
                    message = R.string.empty_stock_list_server_down;
                    break;
                case StockTaskService.STOCK_STATUS_SERVER_INVALID:
                    message = R.string.empty_stock_list_server_error;
                    break;
                case StockTaskService.STOCK_STATUS_INVALID:
                    message = R.string.empty_stock_list_invalid_symbol;
                    break;
                case StockTaskService.NETWORK_NOT_AVAILABLE:
                    message = R.string.empty_stock_list_no_network;
                    break;
                default:
                    if (!Utils.isNetworkAvailable(mContext)) {
                        message = R.string.empty_stock_list_no_network;
                    }
            }
            tv.setText(message);
        }
    }

    private void showSnack(View view, String str) {
        Snackbar snackbar = Snackbar.make(view, str, Snackbar.LENGTH_LONG);
        View snackView = snackbar.getView();

        // Set the Accessibility (talkback) text here.
        view.announceForAccessibility(str);

        snackView.setBackgroundColor(ContextCompat.getColor(this, R.color.red_300));

        snackbar.show();
    }

    private void showSnackWithAction(View view, String str) {
        final Snackbar snackbar = Snackbar.make(view, str, Snackbar.LENGTH_LONG).setDuration(Snackbar.LENGTH_LONG);

        View snackView = snackbar.getView();

        // Set the Accessibility (talkback) text here.
        view.announceForAccessibility(str);

        snackView.setBackgroundColor(ContextCompat.getColor(this, R.color.red_300));


//        snackbar.setAction("OK", new View.OnClickListener() {
//
//            /**
//             * Called when a view has been clicked.
//             *
//             * @param v The view that was clicked.
//             */
//            @Override
//            public void onClick(View v) {
//                snackbar.dismiss();
//            }
//        });

        snackbar.show();
    }

    /**
     * Called when a shared preference is changed, added, or removed. This
     * may be called even if a preference is set to its existing value.
     * <p/>
     * <p>This callback will be run on your main thread.
     *
     * @param sharedPreferences The {@link SharedPreferences} that received
     *                          the change.
     * @param key               The key of the preference that was changed, added, or
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        // Any changes to add/update stock will reflect here.
        if (key.equals(getString(R.string.pref_stock_add_status_key))) {

            int stock = Utils.getStockStatus(mContext, false);

            if (stock == PREFS_VALUE_ALREADY_READ) {
                return;
            }

            // Clear the prefs.
            Utils.setStockStatus(this, PREFS_VALUE_ALREADY_READ, false);


            int message = R.string.empty_stock_list;

            switch (stock) {
                case StockTaskService.STOCK_STATUS_SERVER_DOWN:
                    message = R.string.empty_add_stock_list_server_down;
                    break;
                case StockTaskService.STOCK_STATUS_SERVER_INVALID:
                    message = R.string.empty_add_stock_list_server_error;
                    break;
                case StockTaskService.STOCK_STATUS_INVALID:
                    message = R.string.empty_add_stock_list_invalid_symbol;
                    break;
                default:
                    if (!Utils.isNetworkAvailable(mContext)) {
                        message = R.string.empty_add_stock_list_no_network;
                    }
            }

            showSnackWithAction(parentLayout, getResources().getString(message));
        }
    }

}
