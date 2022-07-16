package kuvaev.mainapp.eatit_server;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import kuvaev.mainapp.eatit_server.Common.Common;
import kuvaev.mainapp.eatit_server.Model.Banner;
import kuvaev.mainapp.eatit_server.ViewHolder.BannerViewHolder;

public class BannerActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    FloatingActionButton fab;
    RelativeLayout rootLayout;

    // FireBase
    FirebaseDatabase database;
    DatabaseReference banners;
    FirebaseStorage storage;
    StorageReference storageReference;
    FirebaseRecyclerAdapter<Banner, BannerViewHolder> adapter;

    // Add new Banner
    MaterialEditText edtName, edtFoodId;
    Button btnSelect, btnUpload;

    Banner newBanner;
    Uri filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_banner);

        // Init FireBase
        database = FirebaseDatabase.getInstance();
        banners = database.getReference("Banner");
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Init RecyclerView
        recyclerView = (RecyclerView)findViewById(R.id.recycler_banner);
        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        rootLayout = (RelativeLayout)findViewById(R.id.rootLayout);

        fab = (FloatingActionButton)findViewById(R.id.fab);
        fab.setOnClickListener(v -> showAddBanner());

        loadListBanner();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadListBanner() {
        adapter = new FirebaseRecyclerAdapter<Banner, BannerViewHolder>(
                Banner.class,
                R.layout.layout_banner,
                BannerViewHolder.class,
                banners
        ) {
            @Override
            private void populateViewHolder(BannerViewHolder viewHolder, Banner model, int position) {
                viewHolder.banner_name.setText(model.getName());
                Picasso.with(getBaseContext())
                        .load(model.getImage())
                        .into(viewHolder.banner_image);
            }
        };
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);
    }

    private void showAddBanner() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(BannerActivity.this);
        alertDialog.setTitle("Add new Banner");
        alertDialog.setMessage("Please fill full information");

        View view = getLayoutInflater().inflate(R.layout.layout_add_new_banner, null);

        edtFoodId = (MaterialEditText) view.findViewById(R.id.edtFoodId);
        edtName = (MaterialEditText) view.findViewById(R.id.edtFoodName);
        btnSelect = (Button) view.findViewById(R.id.btnSelect);
        btnUpload =(Button) view.findViewById(R.id.btnUpload);

        btnSelect.setOnClickListener(v -> chooseImage());
        btnUpload.setOnClickListener(v -> uploadImage());

        alertDialog.setView(view);
        alertDialog.setIcon(R.drawable.ic_laptop_black_24dp);
        alertDialog.setPositiveButton("CREATE", (dialog, which) -> {
            dialog.dismiss();
            if (newBanner != null)
                banners.push().setValue(newBanner);

            loadListBanner();
        });
        alertDialog.setNegativeButton("CANCEL", (dialog, which) -> {
            dialog.dismiss();
            newBanner = null;
            loadListBanner();
        });
        alertDialog.show();
    }

    private void uploadImage() {
        if (filePath != null){
            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage("Uploading...");
            dialog.show();

            String imageName = UUID.randomUUID().toString();

            final StorageReference imageFolder = storageReference.child("images/" + imageName);
            imageFolder.putFile(filePath)
                    .addOnSuccessListener(taskSnapshot -> {
                        dialog.dismiss();
                        Toast.makeText(BannerActivity.this, "Uploaded !!!", Toast.LENGTH_SHORT).show();
                        imageFolder.getDownloadUrl().addOnSuccessListener(uri -> {
                            // Set value for newCategory if image upload and we can get download link
                            newBanner = new Banner();
                            newBanner.setName(Objects.requireNonNull(edtName.getText()).toString());
                            newBanner.setId(Objects.requireNonNull(edtFoodId.getText()).toString());
                            newBanner.setImage(uri.toString());
                        });
                    })
                    .addOnFailureListener(e -> Toast.makeText(BannerActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                    .addOnProgressListener(taskSnapshot -> {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                        dialog.setMessage("Uploaded " + progress + "%");
                    });
        }
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent , "Select Picture"), Common.PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Common.PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null){
            filePath = data.getData();
            btnSelect.setText(R.string.str_image_selected);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle().equals(Common.UPDATE)){
            showUpdateBannerDialog(adapter.getRef(item.getOrder()).getKey() , adapter.getItem(item.getOrder()));
        }
        else if (item.getTitle().equals(Common.DELETE)){
            deleteBanner(adapter.getRef(item.getOrder()).getKey());
        }
        return super.onContextItemSelected(item);
    }

    private void deleteBanner(String key) {
        banners.child(key).removeValue();
    }

    private void showUpdateBannerDialog(final String key, final Banner item) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(BannerActivity.this);
        alertDialog.setTitle("Edit Banner");
        alertDialog.setMessage("Please fill full information");

        View edit_banner = getLayoutInflater().inflate(R.layout.layout_add_new_banner, null);

        edtName = (MaterialEditText)edit_banner.findViewById(R.id.edtFoodName);
        edtFoodId = (MaterialEditText)edit_banner.findViewById(R.id.edtFoodId);

        // Set default value for view
        edtName.setText(item.getName());
        edtFoodId.setText(item.getId());

        btnSelect = (Button)edit_banner.findViewById(R.id.btnSelect);
        btnUpload =(Button)edit_banner.findViewById(R.id.btnUpload);

        // Event for Button
        btnSelect.setOnClickListener(v -> {
            chooseImage();  //Let user select image from Gallery and save URI of this image
        });

        btnUpload.setOnClickListener(v -> changeImage(item));

        alertDialog.setView(edit_banner);
        alertDialog.setIcon(R.drawable.ic_laptop_black_24dp);

        // Set Button
        alertDialog.setPositiveButton("UPDATE", (dialog, which) -> {
            dialog.dismiss();

            // Update information
            item.setName(Objects.requireNonNull(edtName.getText()).toString());
            item.setId(Objects.requireNonNull(edtFoodId.getText()).toString());

            Map<String , Object> update = new HashMap<>();
            update.put("id" , item.getId());
            update.put("name" , item.getName());
            update.put("image" , item.getImage());

            banners.child(key)
                    .updateChildren(update)
                    .addOnCompleteListener(task -> {
                        Snackbar.make(rootLayout , "updated" , Snackbar.LENGTH_SHORT).show();
                        loadListBanner();
                    })
                    .addOnFailureListener(e -> Toast.makeText(BannerActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());

            // Refresh Banners list
            loadListBanner();
        });

        alertDialog.setNegativeButton("NO", (dialog, which) -> {
            dialog.dismiss();
            loadListBanner();
        });
        alertDialog.show();
    }

    private void changeImage(final Banner item){
        if (filePath != null){
            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage("Uploading...");
            dialog.show();

            String imageName = UUID.randomUUID().toString();

            final StorageReference imageFolder = storageReference.child("images/" + imageName);
            imageFolder.putFile(filePath)
                .addOnSuccessListener(taskSnapshot -> {
                    dialog.dismiss();
                    Toast.makeText(BannerActivity.this, "Uploaded !!!", Toast.LENGTH_SHORT).show();
                    imageFolder.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Set value for newCategory if image upload and we can get download link
                        item.setImage(uri.toString());
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(BannerActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    dialog.setMessage("Uploaded " + progress + "%");
                });
        }
    }
}