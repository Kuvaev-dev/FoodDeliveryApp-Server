package kuvaev.mainapp.eatit_server.Service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import kuvaev.mainapp.eatit_server.Common.Common;
import kuvaev.mainapp.eatit_server.Model.Token;

public class MyFirebaseIdService extends Service {
    public void onTokenRefresh() {
        super.onTokenRefresh();

        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        updateToService(refreshedToken);
    }

    private void updateToService(String refreshedToken) {
        if (Common.currentUser != null){
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference tokens = database.getReference("Tokens");
            Token token = new Token(refreshedToken , true);  //true becuz this token send from Server Side
            tokens.child(Common.currentUser.getPhone()).setValue(token);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
