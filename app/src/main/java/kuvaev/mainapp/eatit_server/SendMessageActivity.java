package kuvaev.mainapp.eatit_server;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import kuvaev.mainapp.eatit_server.Common.Common;
import kuvaev.mainapp.eatit_server.Model.CustomResponse;
import kuvaev.mainapp.eatit_server.Model.DataMessage;
import kuvaev.mainapp.eatit_server.Remote.APIService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SendMessageActivity extends AppCompatActivity {
    MaterialEditText edtTitle , edtMessage;
    Button btnSend;
    RelativeLayout rootLayout;

    APIService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);

        mService = Common.getFCMClient();

        edtTitle = findViewById(R.id.edtTitle);
        edtMessage = findViewById(R.id.edtMessage);
        btnSend = findViewById(R.id.btnSend);
        rootLayout = findViewById(R.id.rootLayout);

        btnSend.setOnClickListener(v -> {
            //Create Message
            Map<String , String> data = new HashMap<>();
            data.put("title" , Objects.requireNonNull(edtTitle.getText()).toString());
            data.put("message" , Objects.requireNonNull(edtMessage.getText()).toString());

            DataMessage dataMessage = new DataMessage("/topics/" + Common.topicName, data);

            mService.sendNotification(dataMessage)
                    .enqueue(new Callback<CustomResponse>() {
                        @Override
                        public void onResponse(Call<CustomResponse> call, Response<CustomResponse> response) {
                            if (response.isSuccessful())
                                Snackbar.make(rootLayout  , "Message Sent" , Snackbar.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Call<CustomResponse> call, Throwable t) {
                            Toast.makeText(SendMessageActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}