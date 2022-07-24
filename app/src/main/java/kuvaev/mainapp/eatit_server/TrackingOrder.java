package kuvaev.mainapp.eatit_server;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import kuvaev.mainapp.eatit_server.Common.Common;
import kuvaev.mainapp.eatit_server.Common.JSONParseAction;
import kuvaev.mainapp.eatit_server.Remote.GeoCoordinateAction;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class TrackingOrder extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{

    private GoogleMap mMap;

    private final static int PLAY_SERVICE_RESOLUTION_REQUEST = 1000;
    private final static int LOCATION_PERMISSION_REQUEST = 1001;

    private Task<Location> mLastLocation;
    private GoogleSignInClient mGoogleApiClient;

    private GeoCoordinateAction mService;
    TextView distance,duration,time;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // add calligraphy
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/restaurant_font.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_tracking_order);

        mService = Common.getGeoCodeService();
        distance = (TextView)findViewById(R.id.display_distance);
        duration = (TextView)findViewById(R.id.display_duration);
        time = (TextView)findViewById(R.id.display_expected_hour);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestRuntimePermission();
        } else {
            if (checkPlayServices()){
                buildGoogleApiClient();
                createLocationRequest();
            }
        }

        mLastLocation = LocationServices.getFusedLocationProviderClient(this).getLastLocation();
        displayLocation();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    private void requestRuntimePermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (checkPlayServices()) {
                    buildGoogleApiClient();
                    createLocationRequest();
                    displayLocation();
                }
            }
        }
    }

    private void displayLocation() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestRuntimePermission();
        } else {
            mLastLocation = LocationServices.getFusedLocationProviderClient(this).getLastLocation()
                    .addOnCompleteListener(task -> {
                        Location location = task.getResult();
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        LatLng yourLocation = new LatLng(latitude, longitude);
                        mMap.addMarker(new MarkerOptions().position(yourLocation).title("Your Location"));
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(yourLocation));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(17.0f));
                        drawRoute(yourLocation, Common.currentRequest.getAddress());
                    });
        }
    }

    private void drawRoute(final LatLng yourLocation, final String address) {
        mService.getGeoCode(address)
                .enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                try{
                    JSONObject jsonObject = new JSONObject(response.body());

                    String lat = ((JSONArray)jsonObject.get("results"))
                            .getJSONObject(0)
                            .getJSONObject("geometry")
                            .getJSONObject("location")
                            .get("lat").toString();

                    String lng = ((JSONArray)jsonObject.get("results"))
                            .getJSONObject(0)
                            .getJSONObject("geometry")
                            .getJSONObject("location")
                            .get("lng").toString();

                    LatLng orderLocation = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));

                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.deliverybox);
                    bitmap = Common.scaleBitmap(bitmap, 70, 70);

                    MarkerOptions marker = new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                            .title("Order of " + Common.currentRequest.getPhone())
                            .position(orderLocation);

                    mMap.addMarker(marker);

                    //draw route

                    mService.getDirections(yourLocation.latitude + "," + yourLocation.longitude,
                            orderLocation.latitude + "," + orderLocation.longitude)
                            .enqueue(new Callback<String>() {
                                @Override
                                public void onResponse(Call<String> call, Response<String> response) {
                                    new ParserTask().execute(response.body());
                                }

                                @Override
                                public void onFailure(Call<String> call, Throwable t) {

                                }
                            });
                } catch (JSONException e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

            }
        });
    }

    private void createLocationRequest() {
        LocationRequest mLocationRequest = LocationRequest.create();
        int UPDATE_INTERVAL = 1000;
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        int FATEST_INTERVAL = 5000;
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
        int DISPLACEMENT = 10;
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    protected synchronized void buildGoogleApiClient() {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
        mGoogleApiClient = GoogleSignIn.getClient(this, options);
    }

    private boolean checkPlayServices() {
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS){
            if (GoogleApiAvailability.getInstance().isUserResolvableError(resultCode)){
                Objects.requireNonNull(GoogleApiAvailability.getInstance().getErrorDialog(this, resultCode, PLAY_SERVICE_RESOLUTION_REQUEST)).show();
            } else {
                Toast.makeText(this, "This device is not support", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return  true;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        displayLocation();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.getFusedLocationProviderClient(this).getLastLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.getSignInIntent();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null)
            mGoogleApiClient.getSignInIntent();
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>>> {
        ProgressDialog mDialog = new ProgressDialog(TrackingOrder.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog.setMessage("Please waiting...");
            mDialog.show();
        }

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject;
            List<List<HashMap<String,String>>> routes = null;
            try {
                jsonObject = new JSONObject(strings[0]);
                JSONParseAction parser = new JSONParseAction();
                routes = parser.parse(jsonObject);
            } catch (JSONException e){
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            mDialog.dismiss();
            distance.setText(Common.DISTANCE);
            duration.setText(Common.DURATION);
            time.setText(Common.ESTIMATED_TIME);

            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            for (int i = 0; i<lists.size(); i++){
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();
                List<HashMap<String,String>> path = lists.get(i);

                for (int j = 0; j<path.size(); j++){
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(Objects.requireNonNull(point.get("lat")));
                    double lng = Double.parseDouble(Objects.requireNonNull(point.get("lng")));

                    LatLng position = new LatLng(lat,lng);
                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(8);
                lineOptions.color(Color.RED);
                lineOptions.geodesic(true);
            }

            assert lineOptions != null;
            mMap.addPolyline(lineOptions);
        }
    }
}
