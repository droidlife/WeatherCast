package main.weathercast;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.Loader;
import android.support.v4.app.LoaderManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.net.URL;
import java.util.Arrays;

import main.weathercast.data.WeatherPreference;
import main.weathercast.utilities.NetworkUtility;
import main.weathercast.utilities.OpenWeatherJsonUtil;

public class MainActivity extends AppCompatActivity implements
        ForecastAdapter.ForecastAdapterOnClickHandler, LoaderManager.LoaderCallbacks<String[]>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private final static String TAG = MainActivity.class.getSimpleName();
    private final static int FORECAST_LOADER_ID = 0;
    private static boolean PREFERENCES_HAVE_BEEN_UPDATED = false;
    private RecyclerView mRecyclerView;
    private ForecastAdapter mForecastAdapter;
    private TextView mErrorMessageDisplay;
    private ProgressBar mLoadingIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = findViewById(R.id.recycler_view_forecast);
        mErrorMessageDisplay = findViewById(R.id.tv_error_message_display);
        mLoadingIndicator = findViewById(R.id.pb_loading_indicator);

        LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mForecastAdapter = new ForecastAdapter(this);
        mRecyclerView.setAdapter(mForecastAdapter);

        Bundle bundleForLoader = null;
        getSupportLoaderManager().initLoader(FORECAST_LOADER_ID, bundleForLoader, this);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (PREFERENCES_HAVE_BEEN_UPDATED) {
            Log.d(TAG, "onStart: preferences were updated");
            getSupportLoaderManager().restartLoader(FORECAST_LOADER_ID, null, this);
            PREFERENCES_HAVE_BEEN_UPDATED = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /* Unregister MainActivity as an OnPreferenceChangedListener to avoid any memory leaks. */
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    private void showWeatherDataView() {
        mRecyclerView.setVisibility(View.VISIBLE);
        mErrorMessageDisplay.setVisibility(View.INVISIBLE);
    }


    private void showErrorMessageView() {
        mRecyclerView.setVisibility(View.INVISIBLE);
        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.forecast, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.mi_action_refresh) {
            mForecastAdapter.setWeatherData(null);
            getSupportLoaderManager().restartLoader(FORECAST_LOADER_ID, null, this);
            return true;
        }

        if (id == R.id.action_map) {
            openLocationInMap();
            return true;
        }

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void openLocationInMap() {
        String addressString = WeatherPreference.getPreferredWeatherLocation(this);
        Uri geoLocation = Uri.parse("geo:0,0?q=" + addressString);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!");
        }
    }

    @Override
    public void onClick(String weatherForDay) {
        Context context = this;
        Class destinationClass = DetailActivity.class;
        Intent intent = new Intent(context, destinationClass);
        intent.putExtra(Intent.EXTRA_TEXT, weatherForDay);
        startActivity(intent);
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public Loader<String[]> onCreateLoader(int id, Bundle args) {
        return new AsyncTaskLoader<String[]>(this) {
            String[] mWeatherData = null;

            @Override
            protected void onStartLoading() {
                Log.v(TAG, "START LOADING");
                Log.v(TAG, "weather data: " + Arrays.toString(mWeatherData));
                if (mWeatherData != null) {
                    deliverResult(mWeatherData);
                } else {
                    mLoadingIndicator.setVisibility(View.VISIBLE);
                    forceLoad();
                }
            }

            @Override
            public String[] loadInBackground() {
                Log.v(TAG, "BACKGROUND LOADING");
                String location = WeatherPreference.getPreferredWeatherLocation(MainActivity.this);
                URL weatherRequestUrl = NetworkUtility.buildUrl(location);

                try {
                    String jsonWeatherResponse = NetworkUtility.getHttpResponse(weatherRequestUrl);

                    String[] simpleJsonWeatherData = OpenWeatherJsonUtil
                            .getSimpleWeatherStringsFromJson(MainActivity.this, jsonWeatherResponse);

                    return simpleJsonWeatherData;

                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public void deliverResult(String[] data) {
                Log.v(TAG, "DELIVER RESULT");
                mWeatherData = data;
                super.deliverResult(data);
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<String[]> loader, String[] weatherData) {
        Log.v(TAG, "load finished");
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        if (weatherData != null) {
            showWeatherDataView();
            mForecastAdapter.setWeatherData(weatherData);
        } else {
            showErrorMessageView();
        }
    }

    @Override
    public void onLoaderReset(Loader<String[]> loader) {

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        PREFERENCES_HAVE_BEEN_UPDATED = true;
    }
}
