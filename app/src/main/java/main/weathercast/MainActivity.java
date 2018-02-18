package main.weathercast;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.net.URL;

import main.weathercast.data.WeatherPreference;
import main.weathercast.utilities.NetworkUtility;
import main.weathercast.utilities.OpenWeatherJsonUtil;

public class MainActivity extends AppCompatActivity {
    private TextView mWeatherTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWeatherTextView = findViewById(R.id.tv_weather_data);
        loadWeatherData();
    }

    public void loadWeatherData() {
        String location = WeatherPreference.getPreferredWeatherLocation(this);
        Log.v("loadWeatherData", "Location : " + location);
        new FetchWeatherTask().execute(location);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {
        @Override
        protected String[] doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }

            String location = params[0];
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
        protected void onPostExecute(String[] weatherData) {
            if (weatherData != null) {
                for (String weatherString : weatherData) {
                    mWeatherTextView.append((weatherString) + "\n\n\n");
                }
            }
        }
    }
}
