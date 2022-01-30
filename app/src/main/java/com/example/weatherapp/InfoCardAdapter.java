package com.example.weatherapp;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;


public class InfoCardAdapter extends RecyclerView.Adapter<InfoCardAdapter.ViewHolder> {
    private Context context;
    private ArrayList<InfoCard> infoCardsList;


    public InfoCardAdapter(Context context, ArrayList<InfoCard> infoCardsList) {
        this.context = context;
        this.infoCardsList = infoCardsList;
    }

    @NonNull
    @Override
    public InfoCardAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // We get the layout from the XML
        View view = LayoutInflater.from(context).inflate(R.layout.info_card_element, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InfoCardAdapter.ViewHolder holder, int position) {
        // We get the current info card from the list
        InfoCard card = infoCardsList.get(position);

        // We fill the fields and images with the corresponding values from the info card
        holder.title.setText(card.getTitle());
        holder.valueTop.setText(card.getValueTop());
        holder.valueBottom.setText(card.getValueBottom());
        holder.cardIcon.setImageResource(card.getIcon());

        // We set the color of the background for the info cards
        holder.infoCardBackground.setColorFilter(InfoCard.getBackgroundColor(), PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public int getItemCount() {
        // We return the size of the list
        return infoCardsList.size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {
        // UI elements variables
        private TextView valueTop, valueBottom, title;
        private ImageView cardIcon;
        private RelativeLayout infoCardLayout;
        private GradientDrawable infoCardBackground;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // We get the elements from the layout
            title = itemView.findViewById(R.id.idCardTitle);
            valueTop = itemView.findViewById(R.id.idValueTop);
            valueBottom = itemView.findViewById(R.id.idValueBottom);
            cardIcon = itemView.findViewById(R.id.idCardIcon);
            infoCardLayout = itemView.findViewById(R.id.idCardLayout);

            // We get the background of the info cards
            infoCardBackground = (GradientDrawable) infoCardLayout.getBackground();
        }
    }
}
