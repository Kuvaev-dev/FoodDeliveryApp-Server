package kuvaev.mainapp.eatit_server.Service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import kuvaev.mainapp.eatit_server.Common.Common;
import kuvaev.mainapp.eatit_server.Model.Token;

public class MyFirebaseIdService {
    public void onTokenRefresh() {
        String refreshedToken = FirebaseMessaging.getInstance().getToken().toString();
        updateToServer(refreshedToken);
    }

    private void updateToServer(String refreshedToken) {
        if (Common.currentUser != null) {
            FirebaseDatabase db = FirebaseDatabase.getInstance();
            DatabaseReference tokens = db.getReference("Tokens");
            Token data = new Token(refreshedToken, true);
            // false because token send from client app

            tokens.child(Common.currentUser.getPhone()).setValue(data);
        }
    }
}
