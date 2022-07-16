package kuvaev.mainapp.eatit_server;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
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

public class ShipperManagementActivity extends AppCompatActivity {
    FloatingActionButton fabAdd;

    // Firebase
    FirebaseDatabase database;
    DatabaseReference shippers;
    FirebaseRecyclerAdapter<Shipper, ShipperViewHolder> adapter;

    public RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipper_management);

        // Init FireBase
        database = FirebaseDatabase.getInstance();
        shippers = database.getReference(Common.SHIPPERS_TABLE);

        // Init Views
        fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(v -> showCreateShipperLayout());

        recyclerView = findViewById(R.id.recycler_shippers);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load all shippers
        loadAllShippers();
    }

    private void loadAllShippers() {
        adapter = new FirebaseRecyclerAdapter<Shipper, ShipperViewHolder>(
                Shipper.class,
                R.layout.layout_shipper,
                ShipperViewHolder.class,
                shippers
        ) {
            @Override
            protected void populateViewHolder(ShipperViewHolder viewHolder, final Shipper model, final int position) {
                viewHolder.shipper_phone.setText(model.getPhone());
                viewHolder.shipper_name.setText(model.getName());

                viewHolder.btn_edit.setOnClickListener(v -> showEditDialog(adapter.getRef(position).getKey(), model));
                viewHolder.btn_remove.setOnClickListener(v -> removeShipper(adapter.getRef(position).getKey()));
            }
        };
        recyclerView.setAdapter(adapter);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void removeShipper(String key) {
        shippers.child(key)
                .removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(ShipperManagementActivity.this, "Remove succeed !", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(ShipperManagementActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());

        adapter.notifyDataSetChanged();
    }

    private void showEditDialog(String key , Shipper model) {
        AlertDialog.Builder create_shipper_dialog = new AlertDialog.Builder(this);
        create_shipper_dialog.setTitle("Update Shipper");

        View view = getLayoutInflater().inflate(R.layout.layout_create_shipper , null);
        final MaterialEditText edtName = view.findViewById(R.id.edtName);
        final MaterialEditText edtPhone = view.findViewById(R.id.edtPhone);
        final MaterialEditText edtPassword = view.findViewById(R.id.edtPassword);

        // Set data
        edtName.setText(model.getName());
        edtPhone.setText(model.getPhone());
        edtPassword.setText(model.getPassword());

        create_shipper_dialog.setView(view);
        create_shipper_dialog.setIcon(R.drawable.ic_local_shipping_black_24dp);
        create_shipper_dialog.setPositiveButton("UPDATE", (dialog, which) -> {
            dialog.dismiss();

            Map<String, Object> update = new HashMap<>();
            update.put("name" , Objects.requireNonNull(edtName.getText()).toString());
            update.put("phone" , Objects.requireNonNull(edtPhone.getText()).toString());
            update.put("password" , Objects.requireNonNull(edtPassword.getText()).toString());

            shippers.child(edtPhone.getText().toString())
                    .updateChildren(update)
                    .addOnSuccessListener(aVoid -> Toast.makeText(ShipperManagementActivity.this, "Shipper is updated !", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(ShipperManagementActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        create_shipper_dialog.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        create_shipper_dialog.show();
    }

    private void showCreateShipperLayout() {
        AlertDialog.Builder create_shipper_dialog = new AlertDialog.Builder(this);
        create_shipper_dialog.setTitle("Create Shipper");

        View view = getLayoutInflater().inflate(R.layout.layout_create_shipper, null);
        final MaterialEditText edtName = view.findViewById(R.id.edtName);
        final MaterialEditText edtPhone = view.findViewById(R.id.edtPhone);
        final MaterialEditText edtPassword = view.findViewById(R.id.edtPassword);

        create_shipper_dialog.setView(view);
        create_shipper_dialog.setIcon(R.drawable.ic_local_shipping_black_24dp);
        create_shipper_dialog.setPositiveButton("CREATE", (dialog, which) -> {
            dialog.dismiss();

            Shipper shipper = new Shipper();
            shipper.setName(Objects.requireNonNull(edtName.getText()).toString());
            shipper.setPhone(Objects.requireNonNull(edtPhone.getText()).toString());
            shipper.setPassword(Objects.requireNonNull(edtPassword.getText()).toString());

            shippers.child(edtPhone.getText().toString())
                    .setValue(shipper)
                    .addOnSuccessListener(aVoid -> Toast.makeText(ShipperManagementActivity.this, "Shipper is created !", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(ShipperManagementActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        create_shipper_dialog.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        create_shipper_dialog.show();
    }
}