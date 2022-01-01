package com.cliambrown.pilltime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ActiveDosesRecycleViewAdapter extends RecyclerView.Adapter<ActiveDosesRecycleViewAdapter.ActiveDoseViewHolder> {

    List<Object> activeDoses;
    Context context;

    public ActiveDosesRecycleViewAdapter(List<Object> activeDoses, Context context) {
        this.activeDoses = activeDoses;
        this.context = context;
    }

    @NonNull
    @Override
    public ActiveDosesRecycleViewAdapter.ActiveDoseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_med_active_dose, parent, false);
        ActiveDoseViewHolder holder = new ActiveDosesRecycleViewAdapter.ActiveDoseViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ActiveDosesRecycleViewAdapter.ActiveDoseViewHolder holder, int position) {
        Object activeDose = activeDoses.get(position);
        holder.tv_rvMedActiveDose_info.setText(activeDose.toString());
    }

    @Override
    public int getItemCount() {
        return activeDoses.size();
    }

    public static class ActiveDoseViewHolder extends RecyclerView.ViewHolder {
        TextView tv_rvMedActiveDose_info;
        ConstraintLayout parentLayout;

        public ActiveDoseViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_rvMedActiveDose_info = itemView.findViewById(R.id.tv_rvMedActiveDose_info);
            parentLayout = itemView.findViewById(R.id.layout_rvMedActiveDose);
        }
    }
}
