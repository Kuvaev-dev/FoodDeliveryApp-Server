package kuvaev.mainapp.eatit_server;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import java.util.Objects;
import java.util.UUID;

import info.hoang8f.widget.FButton;
import kuvaev.mainapp.eatit_server.Common.Common;
import kuvaev.mainapp.eatit_server.Model.Food;
import kuvaev.mainapp.eatit_server.ViewHolder.FoodViewHolder;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FoodList extends AppCompatActivity {
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    FloatingActionButton fab;
    RelativeLayout rootLayout;

    // Firebase
    FirebaseDatabase db;
    DatabaseReference foodList;
    FirebaseStorage storage;
    StorageReference storageReference;

    String categoryId = "";

    FirebaseRecyclerAdapter<Food, FoodViewHolder> adapter;

    // add new food
    MaterialEditText edtName,edtDescription, edtPrice;
    FButton btnSelect,btnUpload;
    Food newFood;
    Uri saveUri;

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

        setContentView(R.layout.activity_food_list);

        // Firebase
        db = FirebaseDatabase.getInstance();
        foodList = db.getReference("Foods");
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Init
        recyclerView = findViewById(R.id.recycler_food);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // set layout of foodlist
        rootLayout = findViewById(R.id.rootLayout);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showAddFoodDialog());

        if (getIntent() != null)
            categoryId = getIntent().getStringExtra("CategoryId");
        if (!categoryId.isEmpty())
            loadListFood(categoryId);
     }

    private void showAddFoodDialog() {
        // add food menu
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(FoodList.this, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        alertDialog.setTitle("Add New Food");
        alertDialog.setMessage("Please fill full formation");

        LayoutInflater inflater = this.getLayoutInflater();
        View add_menu_layout = inflater.inflate(R.layout.add_new_food_layout,null);

        edtName = add_menu_layout.findViewById(R.id.edtName);
        edtDescription = add_menu_layout.findViewById(R.id.edtDescription);
        edtPrice = add_menu_layout.findViewById(R.id.edtPrice);
        btnSelect = add_menu_layout.findViewById(R.id.btnSelect);
        btnUpload = add_menu_layout.findViewById(R.id.btnUpload);

        // Event for button
        btnSelect.setOnClickListener(v -> {
            // let users select image from gallery and save URL of this image
            chooseImage();
        });
        btnUpload.setOnClickListener(v -> {
            // upload image
            uploadImage();
        });

        alertDialog.setView(add_menu_layout);
        alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        // set Button
        alertDialog.setPositiveButton("YES", (dialog, which) -> {
            dialog.dismiss();
            // create new category
            if(newFood !=null){
                foodList.push().setValue(newFood);
                Snackbar.make(rootLayout, " New Food " + newFood.getName() + " was added ",
                        Snackbar.LENGTH_SHORT).show();
            }
        });

        alertDialog.setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void uploadImage() {
        if (saveUri != null){
            final ProgressDialog mDialog = new ProgressDialog(this);
            mDialog.setMessage("Uploading...");
            mDialog.show();

            String imageName = UUID.randomUUID().toString();
            final StorageReference imageFolder = storageReference.child("images/" + imageName);
            imageFolder.putFile(saveUri).addOnSuccessListener(taskSnapshot -> {
                mDialog.dismiss();
                Toast.makeText(FoodList.this, "Uploaded!!!", Toast.LENGTH_SHORT).show();
                imageFolder.getDownloadUrl().addOnSuccessListener(uri -> {
                    // set value for newCategory if image upload and we can get download link
                    newFood = new Food();
                    newFood.setName(Objects.requireNonNull(edtName.getText()).toString());
                    newFood.setDescription(Objects.requireNonNull(edtDescription.getText()).toString());
                    newFood.setPrice(Objects.requireNonNull(edtPrice.getText()).toString());
                    newFood.setMenuId(categoryId);
                    newFood.setImage(uri.toString());
                });
            }).addOnFailureListener(e -> {
                mDialog.dismiss();
                Toast.makeText(FoodList.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }).addOnProgressListener(taskSnapshot -> {
                double progress = (100 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                mDialog.setMessage("Uploading" + progress +" % ");
            });
        }
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityIfNeeded(Intent.createChooser(intent, "Select Image"), Common.PICK_IMAGE_REQUEST);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadListFood(String categoryId) {
        Query listFoodByCategoryId = foodList.orderByChild("menuId").equalTo(categoryId);
        FirebaseRecyclerOptions<Food> options = new FirebaseRecyclerOptions.Builder<Food>()
                .setQuery(listFoodByCategoryId, Food.class)
                .build();

        adapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull FoodViewHolder viewHolder, int position, @NonNull Food model) {
                viewHolder.food_name.setText(model.getName());
                Picasso.get()
                        .load(model.getImage())
                        .into(viewHolder.food_image);

                viewHolder.setItemClickListener((view, position1, isLongClick) -> {});
            }

            @NonNull
            @Override
            public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.food_item, parent,false);
                return new FoodViewHolder(itemView);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Common.PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data !=null
                && data.getData()!= null){

            saveUri = data.getData();
            btnSelect.setText(R.string.fl_image_selected_string);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle().equals(Common.UPDATE)){
            showUpdateFoodDialog(adapter.getRef(item.getOrder()).getKey(),adapter.getItem(item.getOrder()));
        } else if(item.getTitle().equals(Common.DELETE)){
            ConfirmDeleteDialog(item);
        }
        return super.onContextItemSelected(item);
    }

    private void ConfirmDeleteDialog(final MenuItem item) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(FoodList.this, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        alertDialog.setTitle("Confirm Delete?");

        LayoutInflater inflater = this.getLayoutInflater();
        View confirm_delete_layout = inflater.inflate(R.layout.confirm_delete_layout,null);
        alertDialog.setView(confirm_delete_layout);
        alertDialog.setIcon(R.drawable.ic_delete_black_24dp);

        alertDialog.setPositiveButton("DELETE", (dialog, which) -> {
            dialog.dismiss();
            deleteFood(adapter.getRef(item.getOrder()).getKey());
        });

        alertDialog.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void showUpdateFoodDialog(final String key, final Food item) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(FoodList.this, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        alertDialog.setTitle("Edit Food");
        alertDialog.setMessage("Please fill full formation");

        LayoutInflater inflater = this.getLayoutInflater();
        View add_menu_layout = inflater.inflate(R.layout.add_new_food_layout,null);

        edtName = add_menu_layout.findViewById(R.id.edtName);
        edtDescription = add_menu_layout.findViewById(R.id.edtDescription);
        edtPrice = add_menu_layout.findViewById(R.id.edtPrice);
        btnSelect = add_menu_layout.findViewById(R.id.btnSelect);
        btnUpload = add_menu_layout.findViewById(R.id.btnUpload);

        // set default value for view
        edtName.setText(item.getName());
        edtDescription.setText(item.getName());
        edtPrice.setText(item.getName());

        // Event for button
        btnSelect.setOnClickListener(v -> {
            // let users select image from gallery and save URL of this image
            chooseImage();
        });
        btnUpload.setOnClickListener(v -> {
            //upload image
            changeImage(item);
        });

        alertDialog.setView(add_menu_layout);
        alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        //set Button
        alertDialog.setPositiveButton("YES", (dialog, which) -> {
            dialog.dismiss();
            // update information
            item.setName(Objects.requireNonNull(edtName.getText()).toString());
            item.setDescription(Objects.requireNonNull(edtDescription.getText()).toString());
            item.setPrice(Objects.requireNonNull(edtPrice.getText()).toString());

            foodList.child(key).setValue(item);

            Snackbar.make(rootLayout, " Food " + item.getName() + " was edited ",
                    Snackbar.LENGTH_SHORT).show();
        });

        alertDialog.setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void changeImage(final Food item) {
        if (saveUri != null){
            final ProgressDialog mDialog = new ProgressDialog(this);
            mDialog.setMessage("Uploading...");
            mDialog.show();

            String imageName = UUID.randomUUID().toString();
            final StorageReference imageFolder = storageReference.child("images/" + imageName);
            imageFolder.putFile(saveUri).addOnSuccessListener(taskSnapshot -> {
                mDialog.dismiss();
                Toast.makeText(FoodList.this, "Uploaded!!!", Toast.LENGTH_SHORT).show();
                imageFolder.getDownloadUrl().addOnSuccessListener(uri -> {
                    // set value for newCategory if image upload and we can get download link
                    item.setImage(uri.toString());
                    Toast.makeText(FoodList.this, "Image Changed Successfully!", Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                mDialog.dismiss();
                Toast.makeText(FoodList.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }).addOnProgressListener(taskSnapshot -> {
                double progress = (100 * taskSnapshot.getBytesTransferred() /taskSnapshot.getTotalByteCount());
                mDialog.setMessage("Uploading" + progress + " % ");
            });
        }
    }

    private void deleteFood(String key) {
        // get all food in category
        DatabaseReference foods = db.getReference("Foods");
        final Query foodInCategory = foods.orderByChild("menuId").equalTo(key);
        foodInCategory.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                    postSnapShot.getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        foodList.child(key).removeValue();
        Toast.makeText(FoodList.this, "Food Deleted Successfully!", Toast.LENGTH_SHORT).show();
    }
}
