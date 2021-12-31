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

public class DosesRecycleViewAdapter extends RecyclerView.Adapter<DosesRecycleViewAdapter.DoseViewHolder> {

    List<Dose> doses;
    Context context;

    public DosesRecycleViewAdapter(List<Dose> doses, Context context) {
        this.doses = doses;
        this.context = context;
    }

    @NonNull
    @Override
    public DoseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_dose, parent, false);
        DoseViewHolder holder = new DoseViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull DoseViewHolder holder, int position) {
        Dose dose = doses.get(position);
        String doseDetails = dose.getCount() + " (" + dose.getDateTimeString() + ")";
        holder.textViewDoseDetails.setText(doseDetails);

//        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(context, Activity.class);
//                intent.putExtra("id", med.getId());
//                context.startActivity(intent);
//            }
//        });
    }

    @Override
    public int getItemCount() {
        return doses.size();
    }

    public static class DoseViewHolder extends RecyclerView.ViewHolder {
        TextView textViewDoseDetails;
        TextView textViewTimeSince;
        ConstraintLayout parentLayout;

        public DoseViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDoseDetails = itemView.findViewById(R.id.textViewDoseDetails);
            textViewTimeSince = itemView.findViewById(R.id.textViewTimeSince);
            parentLayout = itemView.findViewById(R.id.doseLayout);
        }
    }
}
