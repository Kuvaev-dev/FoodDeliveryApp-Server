package kuvaev.mainapp.eatit_server.Remote;

import com.google.android.gms.common.api.GoogleApiClient;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GeoCoordinateAction {
    @GET("maps/api/geocode/json")
    Call<String> getGeoCode(@Query("address") String address, @Query("key") String key);

    @GET("maps/api/directions/json")
    Call<String> getDirections(@Query("origin") String origin, @Query("destination") String destination ,@Query("key") String key);

    GoogleApiClient getGeoCode(String address);

    GoogleApiClient getDirections(String s, String s1);
}
