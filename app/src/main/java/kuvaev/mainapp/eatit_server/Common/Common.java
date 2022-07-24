package kuvaev.mainapp.eatit_server.Common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.Calendar;
import java.util.Locale;

import kuvaev.mainapp.eatit_server.Model.Request;
import kuvaev.mainapp.eatit_server.Model.User;
import kuvaev.mainapp.eatit_server.Remote.APIService;
import kuvaev.mainapp.eatit_server.Remote.FCMRetrofitClient;
import kuvaev.mainapp.eatit_server.Remote.GeoCoordinateAction;
import kuvaev.mainapp.eatit_server.Remote.RetrofitClient;

public class Common {
    public static final String SHIPPER_TABLE ="Shippers" ;
    public static final String ORDER_NEED_SHIP_TABLE = "OrdersNeedShip";
    public static final String Staff_TABLE ="User" ;

    public static User currentUser;
    public static Request currentRequest;

    public static String DISTANCE= "";
    public static String DURATION= "";
    public static String ESTIMATED_TIME = "";

    public static String topicName = "News";

    public static String PHONE_TEXT = "userPhone";

    public static final String UPDATE = "Update";
    public static final String DELETE = "Delete";

    public static final int PICK_IMAGE_REQUEST = 71;

    public static final String baseURL = "https://maps.googleapis.com/";
    public static final String fcmURL =  "https://fcm.googleapis.com/";

    public static String convertCodeToStatus(String code){
        switch (code) {
            case "0":
                return "Placed";
            case "1":
                return "Preparing Orders";
            case "2":
                return "Shipping";
            default:
                return "Delivered";
        }
    }

    public static String convertRole(String code){
        switch (code) {
            case "true":
                return "Role:Staff";
            case "false":
                return "Role:User";
            case "yes":
                return "Role:Admin";
            case "no":
                return "Role:Not Admin";
            default:
                return "Role:User";
        }
    }

    public static APIService getFCMClient(){
        return FCMRetrofitClient.getClient(fcmURL).create(APIService.class);
    }

    public static GeoCoordinateAction getGeoCodeService(){
        return RetrofitClient.getClient(baseURL).create(GeoCoordinateAction.class);
    }

    public static boolean isConnectedToInternet(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null){
            NetworkInfo[] info = connectivityManager.getAllNetworkInfo();
            if (info != null){
                for (NetworkInfo networkInfo : info) {
                    if (networkInfo.getState() == NetworkInfo.State.CONNECTED)
                        return true;
                }
            }
        }
        return false;
    }

    public static Bitmap scaleBitmap(Bitmap bitmap, int newWidth, int newHeight){
        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth,newHeight,Bitmap.Config.ARGB_8888);

        float scaleX = newWidth / (float)bitmap.getWidth();
        float scaleY = newHeight / (float)bitmap.getHeight();
        float pivotX = 0, pivotY = 0;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(scaleX, scaleY, pivotX, pivotY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap,0,0, new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;
    }

    public static String getDate(long time) {
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(time);
        return android.text.format.DateFormat.format("dd-MM-yyyy HH:mm", calendar).toString();
    }
}
