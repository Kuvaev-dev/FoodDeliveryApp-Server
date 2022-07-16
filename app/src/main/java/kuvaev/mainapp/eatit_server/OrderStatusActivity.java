package kuvaev.mainapp.eatit_server;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jaredrummler.materialspinner.MaterialSpinner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kuvaev.mainapp.eatit_server.Common.Common;
import kuvaev.mainapp.eatit_server.Model.CustomResponse;
import kuvaev.mainapp.eatit_server.Model.DataMessage;
import kuvaev.mainapp.eatit_server.Model.Request;
import kuvaev.mainapp.eatit_server.Model.Token;
import kuvaev.mainapp.eatit_server.Remote.APIService;
import kuvaev.mainapp.eatit_server.ViewHolder.OrderViewHolder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderStatusActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    MaterialSpinner spinner , shipperSpinner;

    FirebaseRecyclerAdapter<Request, OrderViewHolder> adapter;
    FirebaseDatabase database;
    DatabaseReference requests;

    APIService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_status);

        database = FirebaseDatabase.getInstance();
        requests = database.getReference("Requests");

        mService = Common.getFCMClient();

        recyclerView = findViewById(R.id.listOrders);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadOrders();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadOrders() {

        adapter = new FirebaseRecyclerAdapter<Request, OrderViewHolder>(
                Request.class,
                R.layout.layout_order,
                OrderViewHolder.class,
                requests
        ) {
            @NonNull
            @Override
            public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return null;
            }

            @Override
            protected void onBindViewHolder(@NonNull OrderViewHolder holder, int position, @NonNull Request model) {
                holder.txtOrderId.setText(adapter.getRef(position).getKey());
                holder.txtOrderStatus.setText(Common.convertCodeToStatus(model.getStatus()));
                holder.txtOrderPhone.setText(model.getPhone());
                holder.txtOrderAddress.setText(model.getAddress());
                holder.txtOrderDate.setText(Common.getData(Long.parseLong(Objects.requireNonNull(adapter.getRef(position).getKey()))));

                holder.btnEdit.setOnClickListener(v -> showUpdateDialog(adapter.getRef(position).getKey(), adapter.getItem(position)));
                holder.btnRemove.setOnClickListener(v -> deleteOrder(adapter.getRef(position).getKey()));
                holder.btnDetail.setOnClickListener(v -> {
                    Intent orderDetail = new Intent(OrderStatusActivity.this, OrderDetailActivity.class);
                    Common.currentRequest = model;
                    orderDetail.putExtra("OrderId", adapter.getRef(position).getKey());
                    startActivity(orderDetail);
                });
                holder.btnDescription.setOnClickListener(v -> {
                    Intent trackingOrder = new Intent(OrderStatusActivity.this, TrackingOrderActivity.class);
                    Common.currentRequest = model;
                    startActivity(trackingOrder);
                });
            }
        };
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void showUpdateDialog(String key, final Request item) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Update Order");
        alertDialog.setMessage("Please choose status");

        View view = getLayoutInflater().inflate(R.layout.layout_update_order , null);

        spinner = view.findViewById(R.id.statusSpinner);
        spinner.setItems("Placed" , "On may way" , "Shipping");

        shipperSpinner = view.findViewById(R.id.shipperSpinner);
        //Load all shipper phone to spinner
        final List<String> shipperList = new ArrayList<>();
        FirebaseDatabase.getInstance().getReference(Common.SHIPPERS_TABLE)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot shipperSnapShot : dataSnapshot.getChildren())
                            shipperList.add(shipperSnapShot.getKey());

                        shipperSpinner.setItems(shipperList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        alertDialog.setView(view);

        final String localKey = key;

        alertDialog.setPositiveButton("YES", (dialog, which) -> {
            dialog.dismiss();
            item.setStatus(String.valueOf(spinner.getSelectedIndex()));

            if (item.getStatus().equals("2")){
                //Copy item to table "OrderNeedShip"
                FirebaseDatabase.getInstance().getReference(Common.ORDER_NEED_TO_SHIP_TABLE)
                        .child(shipperSpinner.getItems().get(shipperSpinner.getSelectedIndex()).toString())
                        .child(localKey)
                        .setValue(item);

                requests.child(localKey).setValue(item);
                adapter.notifyDataSetChanged();  //Add to update  item size

                sendOrderStatusToUser(localKey , item);
                sendOrderShipRequestToShipper(shipperSpinner.getItems().get(shipperSpinner.getSelectedIndex()).toString() , item);
            }
            else {
                requests.child(localKey).setValue(item);
                adapter.notifyDataSetChanged();  //Add to update  item size
                sendOrderStatusToUser(localKey , item);
            }
        });

        alertDialog.setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void sendOrderShipRequestToShipper(String shipperPhone, Request item) {
        DatabaseReference tokens = database.getReference("Tokens");

        tokens.child(shipperPhone)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()){
                            Token token = dataSnapshot.getValue(Token.class);

                            //Make raw payload
                            Map<String , String> data = new HashMap<>();
                            data.put("title" , "ABD");
                            data.put("message" , "You have new order need ship");
                            assert token != null;
                            DataMessage dataMessage = new DataMessage(token.getToken() , data);

                            mService.sendNotification(dataMessage)
                                    .enqueue(new Callback<CustomResponse>() {
                                        @Override
                                        public void onResponse(Call<CustomResponse> call, Response<CustomResponse> response) {
                                            if (response.body().success == 1){

                                                Toast.makeText(OrderStatusActivity.this, "Sent to shipper", Toast.LENGTH_SHORT).show();
                                            }
                                            else {
                                                Toast.makeText(OrderStatusActivity.this, "failed to send notification !",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<CustomResponse> call, Throwable t) {
                                            Log.e("ERROR" , t.getMessage());
                                        }
                                    });
                        }
                        else
                            Toast.makeText(OrderStatusActivity.this, "ERROR", Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void sendOrderStatusToUser(final String key , final Request item) {

        DatabaseReference tokens = database.getReference("Tokens");
        tokens.child(item.getPhone())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()){
                            Token token = dataSnapshot.getValue(Token.class);

                            //Make raw payload
                            Map<String , String> data = new HashMap<>();
                            data.put("title" , "ABD");
                            data.put("message" , "You order " + key + " was updated !");
                            assert token != null;
                            DataMessage dataMessage = new DataMessage(token.getToken() , data);

                            mService.sendNotification(dataMessage)
                                    .enqueue(new Callback<CustomResponse>() {
                                        @Override
                                        public void onResponse(Call<CustomResponse> call, Response<CustomResponse> response) {
                                            if (response.body().success == 1){
                                                Toast.makeText(OrderStatusActivity.this, "Order was updated !", Toast.LENGTH_SHORT).show();
                                            }
                                            else {
                                                Toast.makeText(OrderStatusActivity.this, "Order was updated but failed to send notification !",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<CustomResponse> call, Throwable t) {
                                            Log.e("ERROR" , t.getMessage());
                                        }
                                    });
                        }
                        else
                            Toast.makeText(OrderStatusActivity.this, "ERROR", Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void deleteOrder(String key) {
        requests.child(key).removeValue();
        adapter.notifyDataSetChanged();
    }
}