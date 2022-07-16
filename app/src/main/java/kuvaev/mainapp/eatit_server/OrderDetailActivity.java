package kuvaev.mainapp.eatit_server;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

import kuvaev.mainapp.eatit_server.Common.Common;
import kuvaev.mainapp.eatit_server.ViewHolder.OrderDetailAdapter;

public class OrderDetailActivity extends AppCompatActivity {
    TextView order_id , order_phone , order_address , order_total , order_comment;
    String order_id_value = "";
    RecyclerView lstFoods;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        order_id = findViewById(R.id.order_id);
        order_phone = findViewById(R.id.order_phone);
        order_address = findViewById(R.id.order_address);
        order_total = findViewById(R.id.order_total);
        order_comment = findViewById(R.id.order_comment);

        lstFoods = findViewById(R.id.lstFood);
        lstFoods.setHasFixedSize(true);
        lstFoods.setLayoutManager(new LinearLayoutManager(this));

        if (getIntent() != null)
            order_id_value = getIntent().getStringExtra("OrderId");

        //Set value
        order_id.setText(order_id_value);
        order_phone.setText(Common.currentRequest.getPhone());
        order_total.setText(Common.currentRequest.getTotal());
        order_address.setText(Common.currentRequest.getAddress());
        order_comment.setText(Common.currentRequest.getComment());

        OrderDetailAdapter adapter = new OrderDetailAdapter(Common.currentRequest.getFoods());
        adapter.notifyDataSetChanged();
        lstFoods.setAdapter(adapter);
    }
}