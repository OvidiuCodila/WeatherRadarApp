package com.example.weatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    // UI elements variables
    private RelativeLayout homeLayout;
    private RecyclerView infoCardsView;
    private ProgressBar loadingBar;
    private TextView cityNameTV, temperatureTV, conditionTV;
    private ImageView background, weatherIcon;
    private LinearLayout searchLayout;
    private TextInputEditText searchInput;
    private ImageView searchButton;

    // Location variables
    private LocationManager locationManager;
    private Location currentLocation;
    private final int PERMISSION_CODE = 1;
    private String cityName, latitude, longitude;

    // Request variable
    private RequestQueue requestQueue;

    // Keyboard access variable
    private InputMethodManager inputManager;

    // Info cards access variables
    private ArrayList<InfoCard> infoCardArrayList;
    private InfoCardAdapter infoCardAdapter;

    // Api keys
    private final String airQualityApiKey = "c32bfadc11834a83b9812f357f0f286e";
    private final String weatherInfoApiKey = "9f6a9af49b8147048da182253210612";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_main);

        // We get the elements from the layout
        homeLayout = findViewById(R.id.idLayoutHome);
        loadingBar = findViewById(R.id.idLoadingBar);
        cityNameTV = findViewById(R.id.idCityName);
        temperatureTV = findViewById(R.id.idTemperature);
        conditionTV = findViewById(R.id.idWeatherCondition);
        background = findViewById(R.id.idBackground);
        weatherIcon = findViewById(R.id.idWeatherIcon);

        searchLayout = findViewById(R.id.idSearchLayout);
        searchInput = findViewById(R.id.idSearchInput);
        searchButton = findViewById(R.id.idSearchButton);

        // We initialize the recycler view, the cards list and the adapter
        infoCardsView = findViewById(R.id.idInfoCardsListView);
        infoCardArrayList = new ArrayList<>();
        infoCardAdapter = new InfoCardAdapter(this, infoCardArrayList);
        infoCardsView.setAdapter(infoCardAdapter);

        // We initialize the longitude and latitude variables
        latitude = "";
        longitude = "";

        // Initialize the input manager so we can access the keyboard later
        inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // Initialize a request queue where we add the requests we make online
        requestQueue = Volley.newRequestQueue(MainActivity.this);

        // We check the permissions and request them if we don't have them
        if(this.checkPermissions()) {
            this.requestPermissions();
        }
        else
        {
            // Then we get the necessary data for the current location
            if (this.getCurrentLocation())
                getDataForLocation();
            else {
                // Or fill the fields with error values and messages
                fillWeatherErrorDataForLocation();
                fillAirQualityErrorDataForLocation();
            }
        }

        // Defining the action to be done when the search button on the screen (not the one in the action bar) is pressed
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // We get the text in the input field
                String cityInput = searchInput.getText().toString();
                if(cityInput.isEmpty())
                    Toast.makeText(MainActivity.this, "Please enter a city..", Toast.LENGTH_SHORT).show();
                else
                {
                    // If the text is not empty
                    // We format the city name to start with an upper case letter and the rest of it to be lowercase letters
                    cityInput = formatInputCityName(cityInput);
                    // We make the keyboard disappear
                    inputManager.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
                    // We request the information for the respective city
                    requestWeatherInformation(cityInput);
                    // Ee empty the input text box
                    searchInput.setText("");
                    // And make the search layout invisible
                    searchLayout.setVisibility(View.GONE);
                }
            }
        });
    }



    /* ---------- Activity bar menu ---------- */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // We initialize the menu (action bar) with its specific layout
        getMenuInflater().inflate(R.menu.menu_buttons, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // This method gets called then the user interacts with the menu buttons
        switch (item.getItemId()) {
            case R.id.actionSearch: {
                // If the search button is pressed we make the search fields visible
                searchLayout.setVisibility(View.VISIBLE);
                return true;
            }

            case R.id.actionCurrentLocation: {
                // If the current location button is pressed we check the permissions again
                if(this.checkPermissions()) {
                    //Ask for them if needed
                    this.requestPermissions();
                }
                else {
                    //If they are not needed we get the current location and the data for it
                    if (this.getCurrentLocation())
                        getDataForLocation();
                    else {
                        // Or fill the fields with error values and messages
                        fillWeatherErrorDataForLocation();
                        fillAirQualityErrorDataForLocation();
                    }
                }
                // Then we make the search fields invisible again
                searchLayout.setVisibility(View.GONE);
                // We close the keyboard again just in case it was open when this button was pressed
                inputManager.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
                return true;
            }

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }



    /* ---------- Permissions ---------- */

    // Method used for checking if the permissions for location were given or not
    private boolean checkPermissions()
    {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED);
    }

    // Method used for requesting permissions for location
    private void requestPermissions()
    {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Overriding the method with the permissions result after we requested them
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_CODE)
        {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // If permission was given
                Toast.makeText(this, "Permission granted..", Toast.LENGTH_SHORT).show();
                // We get the current location of the phone and the data for that location
                if(this.getCurrentLocation())
                    getDataForLocation();
                else
                {
                    // Or fill the fields with error values and messages
                    fillWeatherErrorDataForLocation();
                    fillAirQualityErrorDataForLocation();
                }
            }
            else
            {
                // If permissions were not given, we show the error message and fill the error values and messages
                Toast.makeText(this, "Please provide permission..", Toast.LENGTH_SHORT).show();
                fillWeatherErrorDataForLocation();
                fillAirQualityErrorDataForLocation();
            }
            // We replace the loading screen with the interactive UI
            loadingBar.setVisibility(View.GONE);
            homeLayout.setVisibility(View.VISIBLE);
        }
    }



    /* ---------- Information processing ---------- */

    private void getDataForLocation()
    {
        // We get the city name for the coordinates where the phone was found
        cityName = getCityFromLocation(currentLocation.getLongitude(), currentLocation.getLatitude());
        // Then we request weather information for that city
        this.requestWeatherInformation(cityName);
    }

    private String getCityFromLocation(double longitude, double latitude)
    {
        String cityNameFound = "City Not Found";
        // Locale.getDefault() gets the default locale for this current instance of JVM
        // Geocoder codes and decodes addresses into coordinates
        // Initializing a geocoder whose responses will be localized for the default locale
        Geocoder geo = new Geocoder(getBaseContext(), Locale.getDefault());
        try
        {
            // we get a list of potential addresses
            List<Address> addresses = geo.getFromLocation(latitude,longitude,10);
            for(Address adr: addresses)
                if(adr != null)
                {
                    // If we find one address that is not null
                    // We get the city at that address
                    String city = adr.getLocality();
                    if(city != null && !city.equals(""))
                    {
                        // If the city is valid, meaning not null or empty we save the city name
                        cityNameFound = city;
                    }
                }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        // We return the city name found
        return cityNameFound;
    }

    private String formatInputCityName(String name)
    {
        // Get the first letter as string
        String firstLetter = name.substring(0,1);
        // Get the rest of the string
        String remainingLetters = name.substring(1);

        // Concatenate the first letter as upper case with the remaining letters as lower case
        return firstLetter.toUpperCase() + remainingLetters.toLowerCase();
    }



    /* ---------- Information requests ---------- */

    // Get the current location of the phone
    private boolean getCurrentLocation()
    {
        // Initialize the location manager receiving a location manager that controls the location updates
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            // Get the last known location
            currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (currentLocation != null) {
                // If we got a location we return true
                return true;
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        // If we don't have a location, we return false
        return false;
    }

    // Request the information about the weather from the weather api
    @SuppressLint("NewApi")
    private synchronized void requestWeatherInformation(String cityName)
    {
        String url = "http://api.weatherapi.com/v1/current.json?key=" + weatherInfoApiKey + "&q=" + cityName + "&aqi=no";

        // Set the name of the city
        cityNameTV.setText(cityName);

        //
        loadingBar.setVisibility(View.GONE);
        homeLayout.setVisibility(View.VISIBLE);

        // Clear the list with the information cards
        infoCardArrayList.clear();

        // Creating a request object, with the GET method and the above URL
        // And then we specify what to do on success and on error
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try
                {
                    // We parse the resulted JSON object
                    String temperature = response.getJSONObject("current").getString("temp_c") + "°c";
                    temperatureTV.setText(temperature);
                    int isDay = response.getJSONObject("current").getInt("is_day");
                    if(isDay == 1)
                    {
                        background.setImageResource(R.drawable.background_day);
                        // We set the background color of the info cards to the day value
                        InfoCard.setBackgroundColor(Color.parseColor("#CC0080ff"));
                        //Picasso.get().load("https://images.unsplash.com/photo-1513002749550-c59d786b8e6c?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxleHBsb3JlLWZlZWR8MXx8fGVufDB8fHx8&w=1000&q=80").into(background);
                    }
                    else
                    {
                        background.setImageResource(R.drawable.background_night);
                        // We set the background color of the info cards to the night value
                        InfoCard.setBackgroundColor(Color.parseColor("#CC292D36"));
                        //Picasso.get().load("https://images.unsplash.com/photo-1537911836262-959646ba0ff3?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1yZWxhdGVkfDE0fHx8ZW58MHx8fHw%3D&w=1000&q=80").into(background);
                    }
                    String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                    conditionTV.setText(condition);
                    String icon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                    Picasso.get().load("https:".concat(icon)).into(weatherIcon);

                    // We get the necessary data, create an InfoCard object and then add that object to the list of info cards
                    double windSpeed = response.getJSONObject("current").getDouble("wind_kph");
                    String windDirection = response.getJSONObject("current").getString("wind_dir");
                    infoCardArrayList.add(new InfoCard("Wind", String.valueOf(windSpeed), windDirection, R.drawable.ic_wind));

                    double humidity = response.getJSONObject("current").getDouble("humidity");
                    infoCardArrayList.add(new InfoCard("Humidity", String.valueOf(humidity), "", R.drawable.ic_humidity));

                    double rainfall = response.getJSONObject("current").getDouble("precip_mm");
                    infoCardArrayList.add(new InfoCard("Rainfall", rainfall + " mm", "", R.drawable.ic_rainfall));

                    double pressure = response.getJSONObject("current").getDouble("pressure_mb");
                    double pressureInAtms = Math.floor(pressure * 0.000986923267 * 1000) / 1000;
                    infoCardArrayList.add(new InfoCard("Pressure", pressure + " mB", pressureInAtms + " atm", R.drawable.ic_pressure));

                    // We need to parse the date to get the time and date separately and on the correct format
                    SimpleDateFormat inputDate = new SimpleDateFormat("yyyy-MM-dd hh:mm");
                    SimpleDateFormat outputTime = new SimpleDateFormat("hh:mm aa");
                    SimpleDateFormat outputDate = new SimpleDateFormat("dd.MM.yyyy");

                    String date = response.getJSONObject("location").getString("localtime");
                    try {
                        Date d = inputDate.parse(date);
                        infoCardArrayList.add(new InfoCard("Time", outputTime.format(d), outputDate.format(d), R.drawable.ic_time));
                    }
                    catch (ParseException e)
                    {
                        e.printStackTrace();
                        // In case of a parse error we fill the error values
                        infoCardArrayList.add(new InfoCard("Time", "--", "--", R.drawable.ic_time));
                    }

                    String region = response.getJSONObject("location").getString("tz_id");
                    String[] regionParts = region.split("/");
                    infoCardArrayList.add(new InfoCard("Location", regionParts[0], "", R.drawable.ic_region));

                    // We get the latitude and longitude to add as an info card but to also pass to the other API search on breezometer
                    longitude = String.valueOf(response.getJSONObject("location").getDouble("lon"));
                    latitude = String.valueOf(response.getJSONObject("location").getDouble("lat"));
                    infoCardArrayList.add(new InfoCard("Position", latitude, longitude, R.drawable.ic_coordinates));

                    // We request the air quality info only after the weather info request has been processed
                    requestAirQualityInformation(longitude, latitude);

                    // We notify the info cards container that the content has been updated
                    infoCardAdapter.notifyDataSetChanged();
                }
                catch(JSONException e)
                {
                    e.printStackTrace();
                    // If there is an error, we fill the data on the screen with error values and messages
                    fillWeatherErrorDataForLocation();
                    // If the weather info gives an error then the air quality will too, so we fill these fields with error messages and values as well
                    fillAirQualityErrorDataForLocation();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // If there is an error, we fill the data on the screen with error values and messages
                Toast.makeText(MainActivity.this, "Could not get weather data..", Toast.LENGTH_SHORT).show();
                fillWeatherErrorDataForLocation();
                // If the weather info gives an error then the air quality will too, so we fill these fields with error messages and values as well
                fillAirQualityErrorDataForLocation();
            }
        });

        // We add the request to a queue
        requestQueue.add(jsonObjectRequest);
    }

    // Request the information about the quality of air from breezometer
    private synchronized void requestAirQualityInformation(String longitude, String latitude)
    {
        String url = "https://api.breezometer.com/air-quality/v2/current-conditions?lat="+latitude+"&lon="+longitude+"&key=" + airQualityApiKey;

        // Creating a request object, with the GET method and the above URL
        // We specify what to do on success and on error
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try
                {
                    // We parse the resulted JSON object
                    String aqi = response.getJSONObject("data").getJSONObject("indexes").getJSONObject("baqi").getString("aqi");
                    String quality = response.getJSONObject("data").getJSONObject("indexes").getJSONObject("baqi").getString("category");
                    String[] qualityTextSplit = quality.split("\\s+");

                    // We create a card for this information and add it to the info cards list
                    infoCardArrayList.add(new InfoCard("Air Quality", aqi, qualityTextSplit[0], R.drawable.ic_air_quality));

                    // We notify the info cards container that the content has been updated
                    infoCardAdapter.notifyDataSetChanged();
                }
                catch(JSONException e)
                {
                    e.printStackTrace();
                    // If there is an error, we fill the respective fields with error messages and values
                    fillAirQualityErrorDataForLocation();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // If there is an error, we fill the respective fields with error messages and values
                Toast.makeText(MainActivity.this, "Could not get air quality data..", Toast.LENGTH_SHORT).show();
                fillAirQualityErrorDataForLocation();
            }
        });
        // We add the request to a queue
        requestQueue.add(jsonObjectRequest);
    }



    /* ---------- Error handling for the information fields ---------- */

    private void fillWeatherErrorDataForLocation()
    {
        // We fill the fields with NaN/--/error values and messages
        // We also change the icons and backgrounds
        cityNameTV.setText("Location Not Found");
        temperatureTV.setText("--°c");
        background.setImageResource(R.drawable.background_error);
        conditionTV.setText("");
        weatherIcon.setImageResource(R.drawable.ic_no_location);

        // We clear the info cards list
        infoCardArrayList.clear();
        infoCardArrayList.add(new InfoCard("Wind", "--", "--", R.drawable.ic_wind));
        infoCardArrayList.add(new InfoCard("Humidity", "--", "", R.drawable.ic_humidity));
        infoCardArrayList.add(new InfoCard("Pressure", "--", "--", R.drawable.ic_pressure));
        infoCardArrayList.add(new InfoCard("Rainfall", "--", "", R.drawable.ic_rainfall));
        infoCardArrayList.add(new InfoCard("Position", "--", "--", R.drawable.ic_coordinates));
        infoCardArrayList.add(new InfoCard("Location", "--", "", R.drawable.ic_region));
        infoCardArrayList.add(new InfoCard("Time", "--", "--", R.drawable.ic_time));

        // We set the background color of the info cards to the error value
        InfoCard.setBackgroundColor(Color.parseColor("#CC292D36"));

        // We notify the info cards container that the content has been updated
        infoCardAdapter.notifyDataSetChanged();

        longitude = "";
        latitude = "";
    }

    private void fillAirQualityErrorDataForLocation()
    {
        // We fill the fields with NaN/--/error values and messages
        infoCardArrayList.add(new InfoCard("Air Quality", "--", "--", R.drawable.ic_air_quality));

        // We notify the info cards container that the content has been updated
        infoCardAdapter.notifyDataSetChanged();
    }
}