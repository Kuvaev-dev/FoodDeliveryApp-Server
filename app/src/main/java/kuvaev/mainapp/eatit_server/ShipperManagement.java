package kuvaev.mainapp.eatit_server;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import kuvaev.mainapp.eatit_server.Common.Common;
import kuvaev.mainapp.eatit_server.Model.Shipper;
import kuvaev.mainapp.eatit_server.ViewHolder.ShipperViewHolder;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ShipperManagement extends AppCompatActivity {
    FloatingActionButton fabAdd;
    FirebaseDatabase database;
    DatabaseReference shippers;

    public RecyclerView recyclerView;
    public RecyclerView.LayoutManager layoutManager;

    FirebaseRecyclerAdapter<Shipper, ShipperViewHolder> adapter;

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

        setContentView(R.layout.activity_shipper_management);

        // Init View
        fabAdd = (FloatingActionButton)findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(v -> showCreateShipperLayout());

        recyclerView = (RecyclerView)findViewById(R.id.recycler_shippers);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // Database
        database = FirebaseDatabase.getInstance();
        shippers = database.getReference(Common.SHIPPER_TABLE);

        // load all shipper
        loadAllShipper();
    }

    private void loadAllShipper() {
        FirebaseRecyclerOptions<Shipper> allShipper = new FirebaseRecyclerOptions.Builder<Shipper>()
                .setQuery(shippers, Shipper.class)
                .build();

        adapter = new FirebaseRecyclerAdapter<Shipper, ShipperViewHolder>(allShipper) {
            @Override
            protected void onBindViewHolder(@NonNull ShipperViewHolder holder, final int position, @NonNull final Shipper model) {
                holder.shipper_phone.setText(model.getPhone());
                holder.shipper_name.setText(model.getName());
                holder.shipper_password.setText(model.getPassword());

                holder.btn_edit.setOnClickListener(v -> showEditDialog(adapter.getRef(position).getKey(), model));
                holder.btn_remove.setOnClickListener(v -> showDeleteAccountDialog(adapter.getRef(position).getKey()));
            }

            @NonNull
            @Override
            public ShipperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.shipper_layout, parent, false);
                return new ShipperViewHolder(itemView);
            }
        };
        adapter.startListening();
        recyclerView.setAdapter(adapter);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void showDeleteAccountDialog(final String key) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ShipperManagement.this, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        alertDialog.setTitle("Confirm Delete?");

        LayoutInflater inflater = this.getLayoutInflater();
        View confirm_delete_layout = inflater.inflate(R.layout.confirm_delete_layout,null);
        alertDialog.setView(confirm_delete_layout);
        alertDialog.setIcon(R.drawable.ic_delete_black_24dp);

        alertDialog.setPositiveButton("DELETE", (dialog, which) -> {
            dialog.dismiss();
            shippers.child(key).removeValue();
            Toast.makeText(ShipperManagement.this, "Account Delete Successfully!", Toast.LENGTH_SHORT).show();
        });
        alertDialog.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());

        alertDialog.show();
        adapter.notifyDataSetChanged();
    }


    private void showEditDialog(String key, Shipper model) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ShipperManagement.this, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        alertDialog.setTitle("UPDATE SHIPPER ACCOUNT");
        alertDialog.setMessage("Please fill in all information");

        LayoutInflater inflater = LayoutInflater.from(this);
        View layout_shipper = inflater.inflate(R.layout.create_shipper_layout, null);

        final MaterialEditText shipper_phone = (MaterialEditText) layout_shipper.findViewById(R.id.create_shipper_phone);
        final MaterialEditText shipper_name = (MaterialEditText) layout_shipper.findViewById(R.id.create_shipper_name);
        final MaterialEditText shipper_password = (MaterialEditText) layout_shipper.findViewById(R.id.create_shipper_password);
        shipper_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        shipper_password.setTransformationMethod(new PasswordTransformationMethod());

        // set data
        shipper_name.setText(model.getName());
        shipper_phone.setText(model.getPhone());
        shipper_phone.setEnabled(false);
        shipper_password.setText(model.getPassword());

        alertDialog.setView(layout_shipper);
        alertDialog.setIcon(R.drawable.ic_create_black_24dp);

        alertDialog.setPositiveButton("UPDATE", (dialog, which) -> {
            dialog.dismiss();
            // create account

            if (TextUtils.isEmpty(shipper_phone.getText())) {
                Toast.makeText(ShipperManagement.this, "Phone Number is Empty!", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(shipper_name.getText())) {
                Toast.makeText(ShipperManagement.this, "Username is Empty!", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(shipper_password.getText())) {
                Toast.makeText(ShipperManagement.this, "Password is Empty!", Toast.LENGTH_SHORT).show();
            } else if (Objects.requireNonNull(shipper_phone.getText()).length() < 11    ){
                Toast.makeText(ShipperManagement.this, "Phone Number cannot less than 11 digts!", Toast.LENGTH_SHORT).show();
            } else if (shipper_phone.getText().length() >13) {
                Toast.makeText(ShipperManagement.this, "Phone Number cannot exceed 13 digits!", Toast.LENGTH_SHORT).show();
            } else {
                Map<String, Object> update = new HashMap<>();
                update.put("name", Objects.requireNonNull(shipper_name.getText()).toString());
                update.put("phone", shipper_phone.getText().toString());
                update.put("password", Objects.requireNonNull(shipper_password.getText()).toString());

                shippers.child(shipper_phone.getText().toString())
                        .updateChildren(update)
                        .addOnSuccessListener(aVoid -> Toast.makeText(ShipperManagement.this, "Shipper Updated Successfully!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(ShipperManagement.this, "Failed to Update Account!", Toast.LENGTH_SHORT).show());
            }
        });

        alertDialog.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }


    private void showCreateShipperLayout() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ShipperManagement.this, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        alertDialog.setTitle("CREATE SHIPPER ACCOUNT");
        alertDialog.setMessage("Please fill in all information");

        LayoutInflater inflater = LayoutInflater.from(this);
        View layout_shipper = inflater.inflate(R.layout.create_shipper_layout, null);

        final MaterialEditText shipper_phone = (MaterialEditText) layout_shipper.findViewById(R.id.create_shipper_phone);
        final MaterialEditText shipper_name = (MaterialEditText) layout_shipper.findViewById(R.id.create_shipper_name);
        final MaterialEditText shipper_password = (MaterialEditText) layout_shipper.findViewById(R.id.create_shipper_password);
        shipper_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        shipper_password.setTransformationMethod(new PasswordTransformationMethod());

        alertDialog.setView(layout_shipper);
        alertDialog.setIcon(R.drawable.ic_create_black_24dp);

        alertDialog.setPositiveButton("CREATE", (dialog, which) -> {
            dialog.dismiss();
            // create account

            if (TextUtils.isEmpty(shipper_phone.getText())) {
                Toast.makeText(ShipperManagement.this, "Phone Number is Empty!", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(shipper_name.getText())) {
                Toast.makeText(ShipperManagement.this, "Username is Empty!", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(shipper_password.getText())) {
                Toast.makeText(ShipperManagement.this, "Password is Empty!", Toast.LENGTH_SHORT).show();
            } else if (Objects.requireNonNull(shipper_phone.getText()).length() < 11    ){
                Toast.makeText(ShipperManagement.this, "Phone Number cannot less than 11 digts!", Toast.LENGTH_SHORT).show();
            } else if (shipper_phone.getText().length() >13) {
                Toast.makeText(ShipperManagement.this, "Phone Number cannot exceed 13 digits!", Toast.LENGTH_SHORT).show();
            } else {
                Shipper shipper = new Shipper();
                shipper.setName(Objects.requireNonNull(shipper_name.getText()).toString());
                shipper.setPassword(Objects.requireNonNull(shipper_password.getText()).toString());
                shipper.setPhone(shipper_phone.getText().toString());
                shipper.setIsadmin("false");
                shipper.setIsstaff("true");

                shippers.child(shipper_phone.getText().toString())
                        .setValue(shipper)
                        .addOnSuccessListener(aVoid -> Toast.makeText(ShipperManagement.this, "Shipper Created Successfully!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(ShipperManagement.this, "Failed to Create Account!", Toast.LENGTH_SHORT).show());
            }
        });

        alertDialog.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }
}
