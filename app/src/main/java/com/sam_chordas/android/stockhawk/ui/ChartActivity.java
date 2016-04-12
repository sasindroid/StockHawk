package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.sam_chordas.android.stockhawk.R;

public class ChartActivity extends AppCompatActivity {

    ChartFragment fragment_chart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        fragment_chart = (ChartFragment) getSupportFragmentManager().findFragmentByTag(getResources().getString(R.string.tag_fragment_chart));

        if (savedInstanceState == null) {

            if(fragment_chart != null) {
                fragment_chart.setChartArguments(getIntent().getStringExtra(MyStocksActivity.STOCK_SYMBOL));
            }
        }
    }
}
