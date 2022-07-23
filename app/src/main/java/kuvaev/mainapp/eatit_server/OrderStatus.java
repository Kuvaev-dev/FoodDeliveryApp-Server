package kuvaev.mainapp.eatit_server;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jaredrummler.materialspinner.MaterialSpinner;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import kuvaev.mainapp.eatit_server.Common.Common;
import kuvaev.mainapp.eatit_server.Model.CustomResponse;
import kuvaev.mainapp.eatit_server.Model.Notification;
import kuvaev.mainapp.eatit_server.Model.Request;
import kuvaev.mainapp.eatit_server.Model.Sender;
import kuvaev.mainapp.eatit_server.Model.Token;
import kuvaev.mainapp.eatit_server.Remote.APIService;
import kuvaev.mainapp.eatit_server.ViewHolder.OrderViewHolder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class OrderStatus extends AppCompatActivity {
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FirebaseRecyclerAdapter<Request, OrderViewHolder> adapter;

    FirebaseDatabase db;
    DatabaseReference requests;

    MaterialSpinner spinner, shipperSpinner;

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

        setContentView(R.layout.activity_order_status);

        // Firebase
        db = FirebaseDatabase.getInstance();
        requests = db.getReference("Requests");

        // Init service
        mService = Common.getFCMClient();

        // Init
        recyclerView = (RecyclerView) findViewById(R.id.listOrders);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        loadOrders();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadOrders() {
        FirebaseRecyclerOptions<Request> options = new FirebaseRecyclerOptions.Builder<Request>()
                .setQuery(requests, Request.class)
                .build();

        adapter = new FirebaseRecyclerAdapter<Request, OrderViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull OrderViewHolder viewHolder, final int position, @NonNull final Request model) {
                viewHolder.txtOrderId.setText(adapter.getRef(position).getKey());
                viewHolder.txtOrderPhone.setText(model.getPhone());
                viewHolder.txtOrderAddress.setText(model.getAddress());
                viewHolder.txtOrderStatus.setText(Common.convertCodeToStatus(model.getStatus()));
                viewHolder.txtOrderDate.setText(Common.getDate(Long.parseLong(Objects.requireNonNull(adapter.getRef(position).getKey()))));
                viewHolder.txtOrderName.setText(model.getName());
                viewHolder.txtOrderPrice.setText(model.getTotal());

                // New event Button
                viewHolder.btnEdit.setOnClickListener(v -> showUpdateDialog(adapter.getRef(position).getKey(), adapter.getItem(position)));
                viewHolder.btnRemove.setOnClickListener(v -> ConfirmDeleteDialog(adapter.getRef(position).getKey()));
                viewHolder.btnDetail.setOnClickListener(v -> {
                    Intent orderDetail = new Intent(OrderStatus.this, OrderDetail.class);
                    Common.currentRequest = model;
                    orderDetail.putExtra("OrderId", adapter.getRef(position).getKey());
                    startActivity(orderDetail);
                });
                viewHolder.btnDirection.setOnClickListener(v -> {
                    Intent trackingOrder = new Intent(OrderStatus.this, TrackingOrder.class);
                    Common.currentRequest = model;
                    startActivity(trackingOrder);
                });
            }

            @NonNull
            @Override
            public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.order_layout, parent, false);
                return new OrderViewHolder(itemView);
            }
        };

        adapter.startListening();
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);
    }


    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void showUpdateDialog(String key, final Request item) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(OrderStatus.this, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        alertDialog.setTitle("Update Order");
        alertDialog.setMessage("Please Choose Status");

        LayoutInflater inflater = this.getLayoutInflater();
        final View view = inflater.inflate(R.layout.update_order_layout, null);

        spinner = (MaterialSpinner) view.findViewById(R.id.statusSpinner);
        spinner.setItems("Placed", "Preparing Orders", "Shipping", "Delivered");

        shipperSpinner = (MaterialSpinner) view.findViewById(R.id.shipperSpinner);

        // load all shipper to spinner
        final List<String> shipperList = new ArrayList<>();
        FirebaseDatabase.getInstance().getReference(Common.SHIPPER_TABLE)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot shipperSnapshot:dataSnapshot.getChildren())
                            shipperList.add(shipperSnapshot.getKey());
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

            if (item.getStatus().equals("2")) {
                // copy item to table "OrdersNeedShip"
                FirebaseDatabase.getInstance().getReference(Common.ORDER_NEED_SHIP_TABLE)
                        .child(shipperSpinner.getItems().get(shipperSpinner.getSelectedIndex()).toString())
                        .child(localKey)
                        .setValue(item);

                requests.child(localKey).setValue(item);
                adapter.notifyDataSetChanged(); // add to update item size
                sendOrderStatusToUser(localKey, item);
                sendOrderShipRequestToShipper(shipperSpinner.getItems().get(shipperSpinner.getSelectedIndex()).toString(), item);
            } else {
                requests.child(localKey).setValue(item);
                adapter.notifyDataSetChanged(); // add to update item size
                sendOrderStatusToUser(localKey, item);
            }
        });

        alertDialog.setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void sendOrderShipRequestToShipper(String shipperPhone, Request item) {
        DatabaseReference tokens = db.getReference("Tokens");

        tokens.child(shipperPhone)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Token token = dataSnapshot.getValue(Token.class);

                    // make raw payload
                    Notification notification = new Notification("iDeliveryServer", "You have new order need ship");
                    assert token != null;
                    Sender content = new Sender(token.getToken(), notification);

                    mService.sendNotification(content).enqueue(new Callback<CustomResponse>() {
                        @Override
                        public void onResponse(Call<CustomResponse> call, Response<CustomResponse> response) {
                            if (response.body().success == 1) {
                                Toast.makeText(OrderStatus.this, "Sent to Shippers!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(OrderStatus.this, "Failed to send notification!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<CustomResponse> call, Throwable t) {
                            Log.e("ERROR", t.getMessage());

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    @SuppressLint("NotifyDataSetChanged")
    private void ConfirmDeleteDialog(final String key) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(OrderStatus.this,  androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        alertDialog.setTitle("Confirm Delete?");

        LayoutInflater inflater = this.getLayoutInflater();
        View confirm_delete_layout = inflater.inflate(R.layout.confirm_delete_layout,null);
        alertDialog.setView(confirm_delete_layout);
        alertDialog.setIcon(R.drawable.ic_delete_black_24dp);

        alertDialog.setPositiveButton("DELETE", (dialog, which) -> {
            dialog.dismiss();
            requests.child(key).removeValue();
            Toast.makeText(OrderStatus.this, "Order Deleted Successfully!", Toast.LENGTH_SHORT).show();
        });
        alertDialog.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
        adapter.notifyDataSetChanged();
    }

    private void sendOrderStatusToUser(final String key, final Request item) {
        DatabaseReference tokens = db.getReference("Tokens");
        tokens.child(item.getPhone())
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Token token = dataSnapshot.getValue(Token.class);

                    // make raw payload
                    Notification notification = new Notification("iDeliveryServer", "Your order " + key + " was updated");
                    assert token != null;
                    Sender content = new Sender(token.getToken(), notification);

                    mService.sendNotification(content).enqueue(new Callback<CustomResponse>() {
                        @Override
                        public void onResponse(Call<CustomResponse> call, Response<CustomResponse> response) {
                            if (response.body().success == 1) {
                                Toast.makeText(OrderStatus.this, "Order was updated!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(OrderStatus.this, "Order was updated but failed to send notification!"
                                        , Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<CustomResponse> call, Throwable t) {
                            Log.e("ERROR", t.getMessage());

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
 }
