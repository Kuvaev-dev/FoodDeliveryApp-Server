package kuvaev.mainapp.eatit_server;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import kuvaev.mainapp.eatit_server.Common.Common;
import kuvaev.mainapp.eatit_server.Common.NumberOfFood;
import kuvaev.mainapp.eatit_server.Model.Rating;
import kuvaev.mainapp.eatit_server.ViewHolder.ShowCommentViewHolder;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ViewComment extends AppCompatActivity {
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FirebaseDatabase database;
    DatabaseReference ratingDb;

    String foodId = "";

    FirebaseRecyclerAdapter<Rating, ShowCommentViewHolder> adapter;

    SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null)
            adapter.stopListening();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // add calligraphy
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/restaurant_font.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_show_comment);

        // Init SwipeRefreshLayout view
        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (Common.isConnectedToInternet(getBaseContext()))
                loadComment(foodId);
            else {
                Toast.makeText(getBaseContext(), "Please check your internet connection!", Toast.LENGTH_SHORT).show();
            }
        });

        // Default, load for first time
        swipeRefreshLayout.post(() -> {
            if (Common.isConnectedToInternet(getBaseContext()))
                loadComment(foodId);
            else {
                Toast.makeText(getBaseContext(), "Please check your internet connection!", Toast.LENGTH_SHORT).show();
            }
        });

        // Firebase
        database = FirebaseDatabase.getInstance();
        ratingDb = database.getReference("Rating");

        recyclerView = (RecyclerView) findViewById(R.id.recycler_comment);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        FirebaseRecyclerOptions<Rating> options = new FirebaseRecyclerOptions.Builder<Rating>()
              .setQuery(ratingDb, Rating.class)
              .build();

        adapter = new FirebaseRecyclerAdapter<Rating, ShowCommentViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ShowCommentViewHolder holder, int position, @NonNull Rating model) {
                holder.ratingBar.setRating(Float.parseFloat(model.getRateValue()));
                holder.txtComment.setText(model.getComment());
                holder.txtUserPhone.setText(model.getUserPhone());
                holder.txtFoodName.setText(NumberOfFood.convertIdToName(model.getFoodId()));
                Picasso.get().load(model.getImage()).into(holder.commentImage);

                holder.btnDeleteComment.setOnClickListener(v -> ConfirmDeleteDialog(adapter.getRef(holder.getAbsoluteAdapterPosition()).getKey()));
            }
            @NonNull
            @Override
            public ShowCommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.show_comment_layout, parent, false);
                return new ShowCommentViewHolder(view);
            }
        };

        loadComment(foodId);
    }

    private void loadComment(String foodId) {
        adapter.startListening();
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setRefreshing(false);
    }


    private void ConfirmDeleteDialog(final String key) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ViewComment.this, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        alertDialog.setTitle("Confirm Delete?");

        LayoutInflater inflater = this.getLayoutInflater();
        View confirm_delete_layout = inflater.inflate(R.layout.confirm_delete_layout,null);
        alertDialog.setView(confirm_delete_layout);
        alertDialog.setIcon(R.drawable.ic_delete_black_24dp);

        alertDialog.setPositiveButton("DELETE", (dialog, which) -> {
            dialog.dismiss();
            ratingDb.child(key).removeValue();
            Toast.makeText(ViewComment.this, "Comment Delete Successfully!", Toast.LENGTH_SHORT).show();
        });

        alertDialog.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }
}

