package com.cliambrown.pilltime;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DosesRecycleViewAdapter extends RecyclerView.Adapter<DosesRecycleViewAdapter.DoseViewHolder> {

    Med med;
    List<Dose> doses;
    Context context;
    PillTimeApplication mApp;

    public DosesRecycleViewAdapter(Med med, List<Dose> doses, Context context, PillTimeApplication mApp) {
        this.med = med;
        this.doses = doses;
        this.context = context;
        this.mApp = mApp;
    }

    @NonNull
    @Override
    public DoseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_dose, parent, false);
        DoseViewHolder holder = new DoseViewHolder(view);
        holder.context = parent.getContext();
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull DoseViewHolder holder, int position) {

        holder.dose = doses.get(position);
        holder.med = med;
        long takenAt = holder.dose.getTakenAt();

        holder.tv_rvDose_count.setText(Utils.getStrFromDbl(holder.dose.getCount()));
        long takenAtMs = takenAt * 1000L;
        String takenAtStr = context.getString(R.string.at) + " " +
                DateUtils.formatDateTime(context, takenAtMs, DateUtils.FORMAT_SHOW_TIME).toLowerCase() + " " +
                DateUtils.formatDateTime(context, takenAtMs, DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_YEAR) + " ";
        holder.tv_rvDose_takenAt.setText(takenAtStr);

        long expiresAtUnix = holder.dose.getTakenAt() + (med.getDoseHours() * 60L * 60L);
        long expiresAtMs = expiresAtUnix * 1000L;
        String expiresAtStr = " " + context.getString(R.string.at) + " " +
                DateUtils.formatDateTime(context, expiresAtMs, DateUtils.FORMAT_SHOW_TIME).toLowerCase() + " " +
                DateUtils.formatDateTime(context, expiresAtMs, DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_YEAR) + " ";
        holder.tv_rvDose_expiresAt.setText(expiresAtStr);

        holder.updateTimes();

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
                                intent.putExtra("med_id", holder.dose.getMedID());
                                intent.putExtra("dose_id", holder.dose.getId());
                                context.startActivity(intent);
                                return true;
                            case R.id.mi_dose_option_delete:
                                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                builder.setMessage(R.string.dialog_delete_item)
                                        .setTitle(R.string.delete)
                                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                mApp.removeDose(holder.dose);
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
    public void onBindViewHolder(@NonNull DosesRecycleViewAdapter.DoseViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
            return;
        }
        if (payloads.get(0) == "update_times") {
            holder.updateTimes();
        }
    }

    @Override
    public int getItemCount() {
        return doses.size();
    }

    public static class DoseViewHolder extends RecyclerView.ViewHolder {
        TextView tv_rvDose_count;
        TextView tv_rvDose_takenAt;
        TextView tv_rvDose_takenAtTimeAgo;
        ImageView iv_rvDose_active;
        TextView tv_rvDose_expires;
        TextView tv_rvDose_expiresAt;
        TextView tv_rvDose_expiresAtTimeAgo;
        ImageButton btn_rvDose_more;
        Dose dose;
        Med med;
        Context context;

        public DoseViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_rvDose_count = itemView.findViewById(R.id.tv_rvDose_count);
            tv_rvDose_takenAt = itemView.findViewById(R.id.tv_rvDose_takenAt);
            tv_rvDose_takenAtTimeAgo = itemView.findViewById(R.id.tv_rvDose_takenAtTimeAgo);
            iv_rvDose_active = itemView.findViewById(R.id.iv_rvDose_active);
            tv_rvDose_expires = itemView.findViewById(R.id.tv_rvDose_expires);
            tv_rvDose_expiresAt = itemView.findViewById(R.id.tv_rvDose_expiresAt);
            tv_rvDose_expiresAtTimeAgo = itemView.findViewById(R.id.tv_rvDose_expiresAtTimeAgo);
            btn_rvDose_more = itemView.findViewById(R.id.btn_rvDose_more);
        }

        public void updateTimes() {
            if (dose == null) return;
            if (med == null) return;
            dose.updateDoseStatus(med);
            if (dose.isActive()) {
                iv_rvDose_active.setVisibility(View.VISIBLE);
                tv_rvDose_expires.setText(context.getString(R.string.expires));
            } else {
                iv_rvDose_active.setVisibility(View.GONE);
                tv_rvDose_expires.setText(context.getString(R.string.expired));
            }
            tv_rvDose_takenAtTimeAgo.setText(dose.getTakenAtTimeAgo());
            tv_rvDose_expiresAtTimeAgo.setText(dose.getExpiresAtTimeAgo());
        }
    }
}
