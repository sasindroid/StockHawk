package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.LineChartView;
import com.db.chart.view.animation.Animation;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class ChartFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "ChartFragment";

    public static final String STOCK_SYMBOL = "STOCK_SYMBOL";

    LineChartView linechart;
    TextView tvChartHeader;
    Animation anim;

    private static final int CHART_LOADER = 1;

    private final String DATE_TIME_FORMAT = "MMM d, yyyy hh:mm:ss";
    // Get the 24 hr time format.
    private final String TIME_FORMAT = "HH:mm";

    private int minBidPrice, maxBidPrice;
    private String stockSymbol;


    public ChartFragment() {
        // Required empty public constructor
    }

    public static ChartFragment newInstance() {
        return new ChartFragment();
    }

    public void setChartArguments(String stockSymbol) {
        this.stockSymbol = stockSymbol;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chart, container, false);

        linechart = (LineChartView) view.findViewById(R.id.linechart);
        tvChartHeader = (TextView) view.findViewById(R.id.tvChartHeader);

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        // Get the saved value from screen rotation.
        if (savedInstanceState != null) {

            stockSymbol = savedInstanceState.getString(STOCK_SYMBOL, null);

            Log.d(TAG, "onActivityCreated savedInstanceState not null : " + stockSymbol);

            getLoaderManager().restartLoader(CHART_LOADER, null, this);
        } else {

            if (stockSymbol != null) {
                getLoaderManager().initLoader(CHART_LOADER, null, this);
            }
        }

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.d(TAG, "onSaveInstanceState called");

        outState.putString(STOCK_SYMBOL, stockSymbol);
    }

    private void drawMyChart(String[] labels, float[] values) {

        linechart.setYLabels(AxisController.LabelPosition.OUTSIDE);
        linechart.setXLabels(AxisController.LabelPosition.OUTSIDE);

        LineSet dataset = new LineSet(labels, values);
//        dataset.addPoint("sas", 750.0f);

        // Dots
        dataset.setDotsColor(ContextCompat.getColor(getActivity(), R.color.material_green_700));
        dataset.setDotsRadius(6.0f);

        // Line
        dataset.setThickness(3.5f);
        dataset.setColor(ContextCompat.getColor(getActivity(), R.color.red_900));

        setBorderValues(linechart);

        linechart.setXAxis(true);
        linechart.addData(dataset);

//        anim = new Animation();
//        anim.setDuration(500);
//        anim.setEasing(new CubicEase());
//        anim.setAlpha(3);
//        linechart.show(anim);

        linechart.show();
    }

    private void setBorderValues(LineChartView linechart) {

        int diff = maxBidPrice - minBidPrice;
        int step = 1;

        linechart.setLabelsFormat(new DecimalFormat());

        // Set step according to the diff.
//        if (diff >= 8 && diff < 15) {
//            step = 2;
//        } else if (diff >= 15 && diff < 22) {
//            step = 3;
//        } else {
//            step = 1;
//        }
//
//        if (diff == 0) {
//            linechart.setAxisBorderValues(minBidPrice - 1, maxBidPrice + 1);
//        } else {
//            try {
//                linechart.setAxisBorderValues(minBidPrice - 1, maxBidPrice + 1, step);
//            } catch (Exception e) {
//                e.printStackTrace();
//                linechart.setAxisBorderValues(minBidPrice - 1, maxBidPrice + 1, step + 1);
//            }
//        }

        linechart.setAxisBorderValues(minBidPrice - 1, maxBidPrice + 1);
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "Invoked onCreateLoader!");

        Uri uri = QuoteProvider.Quotes.CONTENT_URI;
        String[] projection = new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.CREATED, QuoteColumns.ISUP};
        String selection = QuoteColumns.SYMBOL + " = ?";
        String[] selectionArgs = new String[]{stockSymbol};
        String sortOrder = QuoteColumns._ID + " DESC LIMIT " + getResources().getInteger(R.integer.stock_query_limit);

        return new CursorLoader(getContext(), uri, projection, selection, selectionArgs, sortOrder);
    }

    /**
     * Called when a previously created loader has finished its load.  Note
     * that normally an application is <em>not</em> allowed to commit fragment
     * transactions while in this call, since it can happen after an
     * activity's state is saved.  See {@link --FragmentManager#beginTransaction()
     * FragmentManager.openTransaction()} for further discussion on this.
     * <p>
     * <p>This function is guaranteed to be called prior to the release of
     * the last data that was supplied for this Loader.  At this point
     * you should remove all use of the old data (since it will be released
     * soon), but should not do your own release of the data since its Loader
     * owns it and will take care of that.  The Loader will take care of
     * management of its data so you don't have to.  In particular:
     * <p>
     * <ul>
     * <li> <p>The Loader will monitor for changes to the data, and report
     * them to you through new calls here.  You should not monitor the
     * data yourself.  For example, if the data is a {@link Cursor}
     * and you place it in a {@link CursorAdapter}, use
     * the {@link CursorAdapter#CursorAdapter(Context,
     * Cursor, int)} constructor <em>without</em> passing
     * in either {@link CursorAdapter#FLAG_AUTO_REQUERY}
     * or {@link CursorAdapter#FLAG_REGISTER_CONTENT_OBSERVER}
     * (that is, use 0 for the flags argument).  This prevents the CursorAdapter
     * from doing its own observing of the Cursor, which is not needed since
     * when a change happens you will get a new Cursor throw another call
     * here.
     * <li> The Loader will release the data once it knows the application
     * is no longer using it.  For example, if the data is
     * a {@link Cursor} from a {@link CursorLoader},
     * you should not call close() on it yourself.  If the Cursor is being placed in a
     * {@link CursorAdapter}, you should use the
     * {@link CursorAdapter#swapCursor(Cursor)}
     * method so that the old Cursor is not closed.
     * </ul>
     *
     * @param loader The Loader that has finished.
     * @param data   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        tvChartHeader.setText(String.format(getResources().getString(R.string.chart_header),
                stockSymbol, getResources().getInteger(R.integer.stock_query_limit)));

        ArrayList<Float> bidPriceAL = new ArrayList<>();
        ArrayList<String> createdAL = new ArrayList<>();

        if (data != null && data.getCount() > 0) {

            // Now we have the last n number of rows (differs for portrait & landscape)
            // Now we have to iterate from last to show properly on graph.
            for (int i = data.getCount() - 1; i >= 0; i--) {
                data.moveToPosition(i);

                bidPriceAL.add(Float.valueOf(data.getString(data.getColumnIndex(QuoteColumns.BIDPRICE))));
                createdAL.add(data.getString(data.getColumnIndex(QuoteColumns.CREATED)));
            }

        }

        if (bidPriceAL.size() > 0) {

            float[] bidPriceArr = new float[bidPriceAL.size()];
            String[] createdArr = new String[bidPriceAL.size()];

            int i = 0;

            for (Float bidPrice : bidPriceAL) {

                Log.d(TAG, String.valueOf(bidPrice));

                bidPriceArr[i++] = bidPrice;
            }

            int j = 0;

            for (String created : createdAL) {

                String dateTime = getFormattedDateTime(Long.valueOf(created), TIME_FORMAT);

                Log.d(TAG, dateTime);

                createdArr[j++] = dateTime;
            }


            minBidPrice = Math.round(Collections.min(bidPriceAL));
            maxBidPrice = Math.round(Collections.max(bidPriceAL));

            Log.d(TAG, "MIN-MAX: " + minBidPrice + "-" + maxBidPrice);

            drawMyChart(createdArr, bidPriceArr);

            linechart.setContentDescription(String.format(getResources().getString(R.string.content_line_chart), stockSymbol));
        }
    }

    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public static String getFormattedDateTime(long timeMillis, String format) {
        Date date = new Date();
        date.setTime(timeMillis);
        return new SimpleDateFormat(format).format(date);
    }
}
