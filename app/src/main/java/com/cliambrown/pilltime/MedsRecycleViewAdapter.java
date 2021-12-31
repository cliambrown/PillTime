package com.cliambrown.pilltime;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MedsRecycleViewAdapter extends RecyclerView.Adapter<MedsRecycleViewAdapter.MedViewHolder> {

    List<Med> meds;
    Context context;

    public MedsRecycleViewAdapter(List<Med> meds, Context context) {
        this.meds = meds;
        this.context = context;
    }

    @NonNull
    @Override
    public MedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_med, parent, false);
        MedViewHolder holder = new MedViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull MedViewHolder holder, int position) {
        Med med = meds.get(position);
        holder.tv_rvMed_name.setText(med.getName());
        holder.tv_rvMed_doseInfo.setText(med.getDoseInfo());

        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, MedActivity.class);
                intent.putExtra("id", med.getId());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return meds.size();
    }

    public static class MedViewHolder extends RecyclerView.ViewHolder {
        TextView tv_rvMed_name;
        TextView tv_rvMed_doseInfo;
        ConstraintLayout parentLayout;

        public MedViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_rvMed_name = itemView.findViewById(R.id.tv_rvMed_name);
            tv_rvMed_doseInfo = itemView.findViewById(R.id.tv_rvMed_doseInfo);
            parentLayout = itemView.findViewById(R.id.layout_rvMed);
        }
    }
}
