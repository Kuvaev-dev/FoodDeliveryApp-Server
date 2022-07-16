package kuvaev.mainapp.eatit_server;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import java.util.Objects;
import java.util.UUID;

import kuvaev.mainapp.eatit_server.Common.Common;
import kuvaev.mainapp.eatit_server.Model.Food;
import kuvaev.mainapp.eatit_server.ViewHolder.FoodViewHolder;

public class FoodListActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    FloatingActionButton fab;
    RelativeLayout rootLayout;

    // FireBase
    FirebaseDatabase database;
    DatabaseReference foodList;
    FirebaseStorage storage;
    StorageReference storageReference;

    String categoryId = "";
    Uri saveUri;

    FirebaseRecyclerAdapter<Food, FoodViewHolder> adapter;

    // Add new Food
    MaterialEditText edtName, edtDescription, edtPrice, edtDiscount;
    Button btnSelect, btnUpload;
    Food newFood;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_list);

        // FireBase
        database = FirebaseDatabase.getInstance();
        foodList = database.getReference("Foods");
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Init widgets
        recyclerView = findViewById(R.id.recycler_food);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        rootLayout = findViewById(R.id.rootLayout);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showAddFoodDialog());

        if (getIntent() != null)
            categoryId = getIntent().getStringExtra("CategoryId");

        if (!categoryId.isEmpty())
            loadListFood(categoryId);
    }

    private void showAddFoodDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(FoodListActivity.this);
        alertDialog.setTitle("Add new Food");
        alertDialog.setMessage("Please fill full information");

        View add_menu_layout = getLayoutInflater().inflate(R.layout.layout_add_new_food, null);

        edtName = add_menu_layout.findViewById(R.id.edtName);
        edtDescription = add_menu_layout.findViewById(R.id.edtDescription);
        edtPrice = add_menu_layout.findViewById(R.id.edtPrice);
        edtDiscount = add_menu_layout.findViewById(R.id.edtDiscount);
        btnSelect = add_menu_layout.findViewById(R.id.btnSelect);
        btnUpload = add_menu_layout.findViewById(R.id.btnUpload);

        btnSelect.setOnClickListener(v -> {
            chooseImage();  //Let user select image from Gallery and save URI of this image
        });
        btnUpload.setOnClickListener(v -> uploadImage());

        alertDialog.setView(add_menu_layout);
        alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        // Set Button
        alertDialog.setPositiveButton("YES", (dialog, which) -> {
            dialog.dismiss();
            // Here, just create new Category
            if (newFood != null){
                foodList.push().setValue(newFood);
                Snackbar.make(rootLayout , "New category" + newFood.getName() + "was added",
                        Snackbar.LENGTH_SHORT).show();
            }
        });

        alertDialog.setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadListFood(String categoryId) {
        adapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(
                Food.class,
                R.layout.food_item,
                FoodViewHolder.class,
                foodList.orderByChild("menuId").equalTo(categoryId)
        ) {
            @Override
            private void populateViewHolder(FoodViewHolder viewHolder, Food model, int position) {
                viewHolder.food_name.setText(model.getName());
                Picasso.with(getBaseContext())
                        .load(model.getImage())
                        .into(viewHolder.food_image);
                viewHolder.setItemClickListener((view, position1, isLongClick) -> {});
            }
        };

        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent , "Select Picture"), Common.PICK_IMAGE_REQUEST);
    }

    private void uploadImage() {
        if (saveUri != null){
            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage("Uploading...");
            dialog.show();

            String imageName = UUID.randomUUID().toString();

            final StorageReference imageFolder = storageReference.child("images/" + imageName);
            imageFolder.putFile(saveUri)
                .addOnSuccessListener(taskSnapshot -> {
                    dialog.dismiss();
                    Toast.makeText(FoodListActivity.this, "Uploaded !!!", Toast.LENGTH_SHORT).show();
                    imageFolder.getDownloadUrl().addOnSuccessListener(uri -> {
                        //set value for newCategory if image upload and we can get download link
                        newFood = new Food();
                        newFood.setName(Objects.requireNonNull(edtName.getText()).toString());
                        newFood.setDescription(Objects.requireNonNull(edtDescription.getText()).toString());
                        newFood.setPrice(Objects.requireNonNull(edtPrice.getText()).toString());
                        newFood.setDiscount(Objects.requireNonNull(edtDiscount.getText()).toString());
                        newFood.setMenuId(categoryId);
                        newFood.setImage(uri.toString());
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(FoodListActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    dialog.setMessage("Uploaded " + progress + "%");
                });
        }
    }

    private void changeImage(final Food item) {
        if (saveUri != null){
            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage("Uploading...");
            dialog.show();

            String imageName = UUID.randomUUID().toString();

            final StorageReference imageFolder = storageReference.child("images/" + imageName);
            imageFolder.putFile(saveUri)
                .addOnSuccessListener(taskSnapshot -> {
                    dialog.dismiss();
                    Toast.makeText(FoodListActivity.this, "Uploaded !!!", Toast.LENGTH_SHORT).show();
                    imageFolder.getDownloadUrl().addOnSuccessListener(uri -> {
                        //set value for newCategory if image upload and we can get download link
                        item.setImage(uri.toString());
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(FoodListActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    dialog.setMessage("Uploaded " + progress + "%");
                });
        }
    }

    private void deleteFood(String key) {
        foodList.child(key).removeValue();
    }

    private void showUpdateFoodDialog(final String key, final Food item) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(FoodListActivity.this);
        alertDialog.setTitle("Edit Food");
        alertDialog.setMessage("Please fill full information");

        View add_menu_layout = getLayoutInflater().inflate(R.layout.layout_add_new_food , null);

        edtName = add_menu_layout.findViewById(R.id.edtName);
        edtDescription = add_menu_layout.findViewById(R.id.edtDescription);
        edtPrice = add_menu_layout.findViewById(R.id.edtPrice);
        edtDiscount = add_menu_layout.findViewById(R.id.edtDiscount);

        // Set default value for view
        edtName.setText(item.getName());
        edtDescription.setText(item.getDescription());
        edtPrice.setText(item.getPrice());
        edtDiscount.setText(item.getDiscount());

        btnSelect = add_menu_layout.findViewById(R.id.btnSelect);
        btnUpload = add_menu_layout.findViewById(R.id.btnUpload);

        // Event for Button
        btnSelect.setOnClickListener(v -> {
            chooseImage();  // Let user select image from Gallery and save URI of this image
        });

        btnUpload.setOnClickListener(v -> changeImage(item));

        alertDialog.setView(add_menu_layout);
        alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        // Set Button
        alertDialog.setPositiveButton("YES", (dialog, which) -> {
            dialog.dismiss();

            // Update information
            item.setName(Objects.requireNonNull(edtName.getText()).toString());
            item.setDescription(Objects.requireNonNull(edtDescription.getText()).toString());
            item.setPrice(Objects.requireNonNull(edtPrice.getText()).toString());
            item.setDiscount(Objects.requireNonNull(edtDiscount.getText()).toString());

            foodList.child(key).setValue(item);

            Snackbar.make(rootLayout , " Food" + item.getName() + "was edited" , Snackbar.LENGTH_SHORT).show();
        });

        alertDialog.setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    // Ctrl + O
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Common.PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null){
            saveUri = data.getData();
            btnSelect.setText(R.string.str_image_selected);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle().equals(Common.UPDATE)){
            showUpdateFoodDialog(adapter.getRef(item.getOrder()).getKey() , adapter.getItem(item.getOrder()));
        }
        else if (item.getTitle().equals(Common.DELETE)){
            deleteFood(adapter.getRef(item.getOrder()).getKey());
        }
        return super.onContextItemSelected(item);
    }
}