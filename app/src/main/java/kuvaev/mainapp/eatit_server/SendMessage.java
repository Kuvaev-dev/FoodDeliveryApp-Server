package kuvaev.mainapp.eatit_server;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.Objects;

import info.hoang8f.widget.FButton;
import kuvaev.mainapp.eatit_server.Common.Common;
import kuvaev.mainapp.eatit_server.Model.CustomResponse;
import kuvaev.mainapp.eatit_server.Model.Notification;
import kuvaev.mainapp.eatit_server.Model.Sender;
import kuvaev.mainapp.eatit_server.Remote.APIService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class SendMessage extends AppCompatActivity {
    MaterialEditText edtTitle, edtMessage;
    FButton btnSubmit;

    APIService mService;

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

        setContentView(R.layout.activity_send_message);

        mService = Common.getFCMClient();

        edtTitle = (MaterialEditText) findViewById(R.id.edtTitle);
        edtMessage = (MaterialEditText) findViewById(R.id.edtMessage);
        btnSubmit = (FButton) findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(v -> verifyTitleAndMessage());
    }

    private void verifyTitleAndMessage() {
        if (TextUtils.isEmpty(Objects.requireNonNull(edtTitle.getText()).toString())) {
            Toast.makeText(this, "Title is empty", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(Objects.requireNonNull(edtMessage.getText()).toString())) {
            Toast.makeText(this, "Message is empty!", Toast.LENGTH_SHORT).show();
        } else
            sendNotification();
    }

    private void sendNotification() {
        Notification notification = new Notification(Objects.requireNonNull(edtTitle.getText()).toString(),
                Objects.requireNonNull(edtMessage.getText()).toString());

        Sender toTopic = new Sender();
        toTopic.to = "/topics/" + Common.topicName;
        toTopic.notification = notification;

        mService.sendNotification(toTopic).enqueue(new Callback<CustomResponse>() {
            @Override
            public void onResponse(Call<CustomResponse> call, Response<CustomResponse> response) {
                if (response.isSuccessful())
                    Toast.makeText(SendMessage.this, "Message Sent!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Call<CustomResponse> call, Throwable t) {
                Toast.makeText(SendMessage.this, "" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}



