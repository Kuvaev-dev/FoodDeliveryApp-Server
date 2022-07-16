package kuvaev.mainapp.eatit_server.ViewHolder;

import android.view.ContextMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import kuvaev.mainapp.eatit_server.Common.Common;
import kuvaev.mainapp.eatit_server.Interface.ItemClickListener;
import kuvaev.mainapp.eatit_server.R;

public class FoodViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener {
    public TextView food_name;
    public ImageView food_image;

    private ItemClickListener itemClickListener;

    public FoodViewHolder(View itemView) {
        super(itemView);

        food_name = itemView.findViewById(R.id.food_name);
        food_image = itemView.findViewById(R.id.food_image);

        itemView.setOnCreateContextMenuListener(this); //For Context Menu
        itemView.setOnClickListener(this);
    }

    public void setItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    @Override
    public void onClick(View v) {
        itemClickListener.onClick(v , getBindingAdapterPosition(), false);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle("Select the action");
        menu.add(0, 0, getBindingAdapterPosition() , Common.UPDATE);
        menu.add(0, 1, getBindingAdapterPosition() , Common.DELETE);
    }
}
