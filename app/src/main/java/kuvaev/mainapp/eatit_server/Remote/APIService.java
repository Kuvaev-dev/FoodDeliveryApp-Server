package kuvaev.mainapp.eatit_server.Remote;

import kuvaev.mainapp.eatit_server.Model.CustomResponse;
import kuvaev.mainapp.eatit_server.Model.Sender;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
        {
            "Content-Type:application/json",
            "Authorization:key=yourKey"
        }
    )

    @POST("fcm/send")
    Call<CustomResponse> sendNotification(@Body Sender body);
}
