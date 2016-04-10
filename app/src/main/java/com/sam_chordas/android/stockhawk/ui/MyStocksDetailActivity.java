package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.LineChartView;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MyStocksDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<String>> {

    private static String mQuoteSymbol;
    private static final String TAG = "StockDetailActivity";
    private static final int CURSOR_LOADER_ID = 1;
    private static OkHttpClient client = new OkHttpClient();
    private LineSet mLineSet;
    private LineChartView mLineChartView;
    static HashMap<String, Float> resultsMap;
    int maxValue;
    int minValue;
    static int numberOfDays = 30;
    private static TextView dateRangeTextView;
    private static TextView highBidTextView;
    private static TextView lowBidTextView;
    private static double trueMax;
    private static double trueMin;
    private static Resources resources;
    private String unknownStockSymbol;


    @Override
    public Loader<List<String>> onCreateLoader(int id, Bundle args) {
        return new SampleLoader(this);
    }


    @Override
    public void onLoadFinished(Loader<List<String>> loader, List<String> list) {

        if (list != null) {


            generateRealData(resultsMap);

            //Calculate Step Size for axis
            double stockPriceRange = trueMax - trueMin;

            int desiredStep;

            if (stockPriceRange > 0 && stockPriceRange < 20) {
                desiredStep = 1;
            } else if (stockPriceRange > 0 && stockPriceRange < 50) {
                desiredStep = 5;
            } else if (stockPriceRange >= 50 && stockPriceRange < 100) {
                desiredStep = 10;
            } else if (stockPriceRange >= 100 && stockPriceRange < 1000) {
                desiredStep = 50;
            } else if (stockPriceRange >= 1000) {
                desiredStep = 100;
            } else {
                desiredStep = -1;
            }

            double tempMinValue = trueMin;
            double tempMaxValue = trueMax;

            tempMinValue = Math.floor(tempMinValue / desiredStep) * desiredStep;
            tempMaxValue = Math.ceil(tempMaxValue / desiredStep) * desiredStep;

            mLineSet.setColor(getResources().getColor(R.color.material_blue_500));
            mLineChartView.addData(mLineSet);
            if (desiredStep != -1) {
                mLineChartView.setAxisBorderValues((int) tempMinValue, (int) tempMaxValue, desiredStep);
            } else {
                mLineChartView.setAxisBorderValues(minValue, maxValue);
            }
            mLineChartView.setXAxis(false);
            mLineChartView.setXLabels(AxisController.LabelPosition.NONE);
            mLineChartView.setLabelsColor(Color.WHITE);

            mLineChartView.show();
        } else {
            Toast.makeText(getApplicationContext(), R.string.details_page_network_error, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLoaderReset(Loader<List<String>> loader) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_line_graph);
        dateRangeTextView = (TextView) findViewById(R.id.date_range_textview);
        highBidTextView = (TextView) findViewById(R.id.high_bid_textview);
        lowBidTextView = (TextView) findViewById(R.id.low_bid_textview);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String prefNumberOfDays = sharedPref.getString(MySettingsActivity.HISTORIC_DATA_KEY, "");

        resources = getResources();

        if (!prefNumberOfDays.isEmpty()) {
            numberOfDays = Integer.parseInt(prefNumberOfDays);
        }

        long mQuoteId = getIntent().getLongExtra("ID", -1);

        unknownStockSymbol = resources.getString(R.string.details_page_unknown_stock_symbol_error);

        if (mQuoteId == -1) {
            mQuoteSymbol = unknownStockSymbol;
        } else {
            Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{QuoteColumns.SYMBOL}, QuoteColumns._ID + "= ?",
                    new String[]{String.valueOf(mQuoteId)}, null);
            if (c != null && c.getCount() != 0) {
                if (c.moveToFirst()) {
                    mQuoteSymbol = c.getString(c.getColumnIndex(QuoteColumns.SYMBOL));
                }
            } else {
                mQuoteSymbol = unknownStockSymbol;
            }
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }

        if (mQuoteSymbol != null) {
            // Update activity title with stock symbol and number of days
            String pageTitle = resources.getString(R.string.details_page_title, mQuoteSymbol, String.valueOf(numberOfDays));
            setTitle(pageTitle);
        }

        mLineSet = new LineSet();
        mLineChartView = (LineChartView) findViewById(R.id.linechart);

        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this).forceLoad();
    }


    private void generateRealData(HashMap<String, Float> hashMap) {
        for (Map.Entry<String, Float> entry : hashMap.entrySet()) {
            mLineSet.addPoint(entry.getKey(), entry.getValue());
        }

        trueMax = Collections.max(hashMap.values());
        trueMin = Collections.min(hashMap.values());

        maxValue = Math.round((((int) trueMax) / 100) * 105);
        minValue = Math.round((((int) trueMin) / 100) * 95);

        highBidTextView.append(" " + String.format("%.2f", trueMax));
        lowBidTextView.append(" " + String.format("%.2f", trueMin));
    }


    private static class SampleLoader extends AsyncTaskLoader<List<String>> {

        public SampleLoader(Context context) {
            super(context);
        }

        @Override
        public List<String> loadInBackground() {

            String finalJSONString = null;

            Calendar endCalendar = Calendar.getInstance();

            int day = endCalendar.get(Calendar.DAY_OF_MONTH);
            int month = endCalendar.get(Calendar.MONTH) + 1;
            int year = endCalendar.get(Calendar.YEAR);

            endCalendar.add(Calendar.DAY_OF_YEAR, -numberOfDays);

            int day1 = endCalendar.get(Calendar.DAY_OF_MONTH);
            int month1 = endCalendar.get(Calendar.MONTH) + 1;
            int year1 = endCalendar.get(Calendar.YEAR);


            String startDate = year1 + "-" + String.format("%02d", month1) + "-" + String.format("%02d", day1);
            String endDate = year + "-" + String.format("%02d", month) + "-" + String.format("%02d", day);

            dateRangeTextView.setText(resources.getString(R.string.details_page_date_range, startDate, endDate));

            String queryParams = "\"" + mQuoteSymbol + "\" and startDate = \"" + startDate + "\" and endDate = \"" + endDate + "\"";

            String urlStub = "select * from yahoo.finance.historicaldata where symbol = ";

            StringBuilder urlStringBuilder = new StringBuilder();
            try {
                // Base URL for the Yahoo query
                urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
                urlStringBuilder.append(URLEncoder.encode(urlStub + queryParams, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                    + "org%2Falltableswithkeys&callback=");

            String urlString;
            String getResponse = null;
            int result = GcmNetworkManager.RESULT_FAILURE;
            JSONObject serverJSONResponse = null;

            if (urlStringBuilder != null) {
                urlString = urlStringBuilder.toString();

                try {
                    getResponse = fetchData(urlString);
                    result = GcmNetworkManager.RESULT_SUCCESS;
                    Log.d(TAG, getResponse);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            if (result == GcmNetworkManager.RESULT_SUCCESS) {
                try {
                    serverJSONResponse = new JSONObject(getResponse);
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                if (serverJSONResponse != null) {
                    try {
                        JSONArray quoteJSONObject = serverJSONResponse.getJSONObject("query").getJSONObject("results").getJSONArray("quote");

                        if (quoteJSONObject.length() > 0) {

                            resultsMap = new HashMap<>();
                            for (int i = 0; i < quoteJSONObject.length(); i++) {
                                String quoteDate = quoteJSONObject.getJSONObject(i).getString("Date");
                                float quoteClose = Float.valueOf(quoteJSONObject.getJSONObject(i).getString("Close"));
                                resultsMap.put(quoteDate, quoteClose);
                            }

                            finalJSONString = resultsMap.toString();

                            Log.d(TAG, resultsMap.toString());
                        }
                    } catch (JSONException e) {
                        finalJSONString = "JSON Error";
                        e.printStackTrace();
                    }
                }

                final ArrayList<String> finalResults = new ArrayList<>();
                finalResults.add(finalJSONString);

                return finalResults;

            } else {
                return null;
            }
        }


        String fetchData(String url) throws IOException {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();
        }


    }
}
