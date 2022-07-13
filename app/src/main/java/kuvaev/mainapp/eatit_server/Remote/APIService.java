package kuvaev.mainapp.eatit_server.Remote;

import kuvaev.mainapp.eatit_server.Model.CustomResponse;
import kuvaev.mainapp.eatit_server.Model.DataMessage;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=...................................."
            }
    )

    @POST("fcm/send")
    Call<CustomResponse> sendNotification(@Body DataMessage body);
}
