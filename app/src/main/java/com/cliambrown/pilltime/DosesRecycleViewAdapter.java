package com.cliambrown.pilltime;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

public class DosesRecycleViewAdapter extends RecyclerView.Adapter<DosesRecycleViewAdapter.DoseViewHolder> {

    List<Dose> doses;
    Context context;
    PillTimeApplication mApp;

    public DosesRecycleViewAdapter(List<Dose> doses, Context context, PillTimeApplication mApp) {
        this.doses = doses;
        this.context = context;
        this.mApp = mApp;
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
        NumberFormat nf = new DecimalFormat("##.###");
        String doseDetails = "x" + nf.format(dose.getCount()) + " (" + dose.getDateTimeString() + ")";
        holder.tv_rvDose_details.setText(doseDetails);
        holder.tv_rvDose_timeSince.setText((dose.getTakenAt() + ""));

        holder.btn_rvDose_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(context, holder.btn_rvDose_more);
                popupMenu.inflate(R.menu.dose_option_menu);

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.mi_dose_option_edit:
                                Intent intent = new Intent(context, EditDoseActivity.class);
                                intent.putExtra("med_id", dose.getMedID());
                                intent.putExtra("dose_id", dose.getId());
                                context.startActivity(intent);
                                return true;
                            case R.id.mi_dose_option_delete:
                                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                builder.setMessage(R.string.dialog_delete_item)
                                        .setTitle(R.string.delete)
                                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                mApp.removeDose(dose);
                                                DosesRecycleViewAdapter.this.notifyItemRemoved(holder.getAdapterPosition());
                                            }
                                        })
                                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.dismiss();
                                            }
                                        });
                                builder.show();
                        }
                        return false;
                    }
                });

                popupMenu.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return doses.size();
    }

    public static class DoseViewHolder extends RecyclerView.ViewHolder {
        TextView tv_rvDose_details;
        TextView tv_rvDose_timeSince;
        ImageButton btn_rvDose_more;
        ConstraintLayout parentLayout;

        public DoseViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_rvDose_details = itemView.findViewById(R.id.tv_rvDose_details);
            tv_rvDose_timeSince = itemView.findViewById(R.id.tv_rvDose_timeSince);
            btn_rvDose_more = itemView.findViewById(R.id.btn_rvDose_more);
            parentLayout = itemView.findViewById(R.id.layout_rvDose);
        }
    }
}
