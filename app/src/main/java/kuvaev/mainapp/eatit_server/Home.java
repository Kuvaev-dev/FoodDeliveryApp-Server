package kuvaev.mainapp.eatit_server;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import java.util.Objects;
import java.util.UUID;

import info.hoang8f.widget.FButton;
import kuvaev.mainapp.eatit_server.Common.Common;
import kuvaev.mainapp.eatit_server.Model.Category;
import kuvaev.mainapp.eatit_server.Model.Token;
import kuvaev.mainapp.eatit_server.ViewHolder.MenuViewHolder;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static kuvaev.mainapp.eatit_server.Common.Common.PICK_IMAGE_REQUEST;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    TextView txtFullName;

    // Firebase
    FirebaseDatabase database;
    DatabaseReference categories;
    FirebaseStorage storage;
    StorageReference storageReference;
    FirebaseRecyclerAdapter<Category, MenuViewHolder> adapter;

    // View
    RecyclerView recycler_menu;
    RecyclerView.LayoutManager layoutManager;

    // Add new menu layout
    MaterialEditText edtName;
    FButton btnUpload, btnSelect;

    Category newCategory;
    Uri saveUri;
    DrawerLayout drawer;

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

        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.colorWhite));
        toolbar.setTitle("Menu Management");
        setSupportActionBar(toolbar);

        // Init firebase
        database = FirebaseDatabase.getInstance();
        categories = database.getReference("Category");
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> showDialog());

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // set name for user
        View headerView = navigationView.getHeaderView(0);
        txtFullName = (TextView)headerView.findViewById(R.id.txtFullName);
        txtFullName.setText(Common.currentUser.getName());

        // init view
        recycler_menu = (RecyclerView)findViewById(R.id.recycler_menu);
        recycler_menu.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recycler_menu.setLayoutManager(layoutManager);

        loadMenu();

        // send token
        updateToken(FirebaseMessaging.getInstance().getToken().toString());
    }

    private void updateToken(String token) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference("Tokens");
        Token data = new Token(token, true);
        // false because token send from client app

        tokens.child(Common.currentUser.getPhone()).setValue(data);
    }

    private void showDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        alertDialog.setTitle("Add New Category");
        alertDialog.setMessage("Please fill full formation");

        LayoutInflater inflater = this.getLayoutInflater();
        View add_menu_layout = inflater.inflate(R.layout.add_new_menu_layout,null);

        edtName = add_menu_layout.findViewById(R.id.edtName);
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
            //create new category
            if(newCategory !=null){
                categories.push().setValue(newCategory);
                Snackbar.make(drawer, " New Category " + newCategory.getName()+ " was added ",
                        Snackbar.LENGTH_SHORT).show();
            }
        });

        alertDialog.setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data !=null
                && data.getData()!= null){

            saveUri = data.getData();
            btnSelect.setText("Image Selected!");
        }
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
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
                Toast.makeText(Home.this, "Uploaded!!!", Toast.LENGTH_SHORT).show();
                imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {

                        //set value for newCategory if image upload and we can get download link
                        newCategory = new Category(Objects.requireNonNull(edtName.getText()).toString(), uri.toString());
                    }
                });
            }).addOnFailureListener(e -> {
                mDialog.dismiss();
                Toast.makeText(Home.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }).addOnProgressListener(taskSnapshot -> {
                double progress = (100 * taskSnapshot.getBytesTransferred() /taskSnapshot.getTotalByteCount());
                mDialog.setMessage("Uploading" + progress + " % ");
            });
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadMenu() {
        FirebaseRecyclerOptions<Category> options = new FirebaseRecyclerOptions.Builder<Category>()
                .setQuery(categories, Category.class)
                .build();

        adapter = new FirebaseRecyclerAdapter<Category, MenuViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MenuViewHolder viewHolder, int position, @NonNull Category model) {
                viewHolder.txtMenuName.setText(model.getName());
                Picasso.get()
                        .load(model.getImage())
                        .into(viewHolder.imageView);

                viewHolder.setItemClickListener((view, position1, isLongClick) -> {
                    // send category Id and start new activity
                    Intent foodList = new Intent(Home.this, FoodList.class);
                    foodList.putExtra("CategoryId", adapter.getRef(position1).getKey());
                    startActivity(foodList);
                });
            }

            @NonNull
            @Override
            public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
               View itemView = LayoutInflater.from(parent.getContext())
                       .inflate(R.layout.menu_item, parent, false);
                       return new MenuViewHolder(itemView);
            }
        };

        // refresh data if have data changed
        adapter.startListening();
        adapter.notifyDataSetChanged();
        recycler_menu.setAdapter(adapter);
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
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_orders) {
            Intent orders = new Intent(Home.this, OrderStatus.class);
            startActivity(orders);
        } else if (id == R.id.nav_banner) {
            Intent banner = new Intent(Home.this, BannerActivity.class);
            startActivity(banner);
        } else if (id == R.id.nav_message) {
            Intent message = new Intent(Home.this, SendMessage.class);
            startActivity(message);
        } else if (id == R.id.nav_sign_out) {
            ConfirmSignOutDialog();
        } else if (id == R.id.nav_about) {
            Intent about = new Intent(Home.this, ScrollingActivity.class);
            startActivity(about);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // Update and delete
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle().equals(Common.UPDATE)) {
            showUpdateDialog(adapter.getRef(item.getOrder()).getKey(), adapter.getItem(item.getOrder()));
        } else if(item.getTitle().equals(Common.DELETE)) {
            ConfirmDeleteDialog(item);
        }
        return super.onContextItemSelected(item);
    }

    private void ConfirmSignOutDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        alertDialog.setTitle("Confirm Sign Out?");

        LayoutInflater inflater = LayoutInflater.from(this);
        View layout_signout = inflater.inflate(R.layout.confirm_signout_layout, null);
        alertDialog.setView(layout_signout);
        alertDialog.setIcon(R.drawable.ic_exit_to_app_black_24dp);

        alertDialog.setPositiveButton("SIGN OUT", (dialog, which) -> {
            dialog.dismiss();
            Intent signout = new Intent(Home.this, MainActivity.class);
            startActivity(signout);
        });

        alertDialog.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void ConfirmDeleteDialog(final MenuItem item) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        alertDialog.setTitle("Confirm Delete?");

        LayoutInflater inflater = this.getLayoutInflater();
        View confirm_delete_layout = inflater.inflate(R.layout.confirm_delete_layout,null);
        alertDialog.setView(confirm_delete_layout);
        alertDialog.setIcon(R.drawable.ic_delete_black_24dp);

        alertDialog.setPositiveButton("DELETE", (dialog, which) -> {
            dialog.dismiss();
            deleteCategory(adapter.getRef(item.getOrder()).getKey());
        });

        alertDialog.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }


    private void showUpdateDialog(final String key, final Category item) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        alertDialog.setTitle("Update Category");
        alertDialog.setMessage("Please fill full formation");

        LayoutInflater inflater = this.getLayoutInflater();
        View add_menu_layout = inflater.inflate(R.layout.add_new_menu_layout,null);

        edtName = add_menu_layout.findViewById(R.id.edtName);
        btnSelect = add_menu_layout.findViewById(R.id.btnSelect);
        btnUpload = add_menu_layout.findViewById(R.id.btnUpload);

        // set default name
        edtName.setText(item.getName());

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

        // set Button
        alertDialog.setPositiveButton("YES", (dialog, which) -> {
            dialog.dismiss();

            // update information
            item.setName(Objects.requireNonNull(edtName.getText()).toString());
            categories.child(key).setValue(item);
            Toast.makeText(Home.this, "Category Name Updated Successfully!", Toast.LENGTH_SHORT).show();
        });

        alertDialog.setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void deleteCategory(String key) {
        // get all food in category
        DatabaseReference foods = database.getReference("Foods");
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

        categories.child(key).removeValue();
        Toast.makeText(Home.this, "Category Deleted Successfully!", Toast.LENGTH_SHORT).show();
    }

    private void changeImage(final Category item) {
        if (saveUri != null){
            final ProgressDialog mDialog = new ProgressDialog(this);
            mDialog.setMessage("Uploading...");
            mDialog.show();

            String imageName = UUID.randomUUID().toString();
            final StorageReference imageFolder = storageReference.child("images/" + imageName);
            imageFolder.putFile(saveUri).addOnSuccessListener(taskSnapshot -> {
                mDialog.dismiss();
                Toast.makeText(Home.this, "Uploaded!!!", Toast.LENGTH_SHORT).show();
                imageFolder.getDownloadUrl().addOnSuccessListener(uri -> {
                    // set value for newCategory if image upload and we can get download link
                    item.setImage(uri.toString());
                    Toast.makeText(Home.this, "Image Changed Successfully!", Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                mDialog.dismiss();
                Toast.makeText(Home.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }).addOnProgressListener(taskSnapshot -> {
                double progress = (100 * taskSnapshot.getBytesTransferred() /taskSnapshot.getTotalByteCount());
                mDialog.setMessage("Uploading" + progress + " % ");
            });
        }
    }
}
