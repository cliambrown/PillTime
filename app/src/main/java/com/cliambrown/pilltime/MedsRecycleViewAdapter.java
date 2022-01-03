package com.cliambrown.pilltime;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;
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

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MedsRecycleViewAdapter extends RecyclerView.Adapter<MedsRecycleViewAdapter.MedViewHolder> {

    List<Med> meds;
    Context context;
    PillTimeApplication mApp;

    public MedsRecycleViewAdapter(List<Med> meds, Context context, PillTimeApplication mApp) {
        this.meds = meds;
        this.context = context;
        this.mApp = mApp;
    }

    @NonNull
    @Override
    public MedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_med, parent, false);
        MedViewHolder holder = new MedViewHolder(view);
        return holder;
    }

    private void updateTimes(@NonNull MedViewHolder holder, Med med) {
        Dose latestDose = med.getLatestDose();
        double currentTotalDoseCount = med.getCurrentTotalDoseCount();
        holder.tv_rvMed_currentTotalDoseCount.setText(Utils.getStrFromDbl(currentTotalDoseCount));
        if (currentTotalDoseCount >= (long) med.getMaxDose()) {
            holder.tv_rvMed_currentTotalDoseCount.setTextColor(context.getResources().getColor(R.color.red_500));
        } else {
            holder.tv_rvMed_currentTotalDoseCount.setTextColor(ThemeProvider.getDefaultTextColor(context));
        }

        if (latestDose == null || currentTotalDoseCount == 0) {
            holder.tv_rvMed_latestDoseExpiresIn.setVisibility(View.GONE);
        } else {
            double latestDoseCount = latestDose.getCount();
            long expiresAtUnix = latestDose.getTakenAt() + (med.getDoseHours() * 60L * 60L);
            String timeAgo = DateUtils.getRelativeTimeSpanString(
                    expiresAtUnix * 1000L,
                    System.currentTimeMillis(),
                    0,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                ).toString();
            String expiresIn = context.getString(R.string.expires) + " " +
                    Utils.decapitalize(timeAgo) + " (" + Utils.simpleFutureTime(context, expiresAtUnix) + ")";
            if (latestDoseCount < currentTotalDoseCount) {
                expiresIn = "x" + Utils.getStrFromDbl(latestDoseCount) + " " + expiresIn;
            }
            holder.tv_rvMed_latestDoseExpiresIn.setText(expiresIn);
            holder.tv_rvMed_latestDoseExpiresIn.setVisibility(View.VISIBLE);
        }
        Log.d("clb", "updateTimes() from medID=" + med.getId());
    }

    @Override
    public void onBindViewHolder(@NonNull MedViewHolder holder, int position) {
        Med med = meds.get(position);

        int doseHours = med.getDoseHours();
        String takenInPast = " " + context.getString(R.string.taken_in_past) + " " +
                doseHours + " " + context.getString(R.string.hours);

        holder.tv_rvMed_name.setText(med.getName());
        holder.tv_rvMed_maxDoseInfo.setText(med.getMaxDoseInfo());
        holder.tv_rvMed_takenInPast.setText(takenInPast);

        updateTimes(holder, med);

        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, MedActivity.class);
                intent.putExtra("id", med.getId());
                context.startActivity(intent);
            }
        });

        holder.btn_rvMed_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, EditDoseActivity.class);
                intent.putExtra("med_id", med.getId());
                context.startActivity(intent);
            }
        });

        holder.btn_rvMed_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(context, holder.btn_rvMed_more);
                popupMenu.inflate(R.menu.med_option_menu);

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        Intent intent;
                        switch (menuItem.getItemId()) {
                            case R.id.mi_med_option_edit:
                                intent = new Intent(context, EditMedActivity.class);
                                intent.putExtra("id", med.getId());
                                context.startActivity(intent);
                                return true;
                            case R.id.mi_med_option_history:
                                intent = new Intent(context, MedActivity.class);
                                intent.putExtra("id", med.getId());
                                context.startActivity(intent);
                                return true;
                            case R.id.mi_med_option_delete:
                                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                builder.setMessage(R.string.dialog_delete_item)
                                        .setTitle(R.string.delete)
                                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                mApp.removeMedById(med.getId());
                                                MedsRecycleViewAdapter.this.notifyItemRemoved(holder.getAdapterPosition());
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

        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            updateTimes(holder, med);
                        } catch (Exception e) {
                        }
                    }
                });
            }
        };
//        timer.schedule(doAsynchronousTask, 0, 60000);
        timer.schedule(doAsynchronousTask, 0, 10000);
    }

    @Override
    public int getItemCount() {
        return meds.size();
    }

    public static class MedViewHolder extends RecyclerView.ViewHolder {
        ImageButton btn_rvMed_add;
        TextView tv_rvMed_name;
        TextView tv_rvMed_maxDoseInfo;
        TextView tv_rvMed_doseHours;
        TextView tv_rvMed_currentTotalDoseCount;
        TextView tv_rvMed_takenInPast;
        TextView tv_rvMed_latestDoseExpiresIn;
        ImageButton btn_rvMed_more;
        ConstraintLayout parentLayout;

        public MedViewHolder(@NonNull View itemView) {
            super(itemView);
            btn_rvMed_add = itemView.findViewById(R.id.btn_rvMed_add);
            tv_rvMed_name = itemView.findViewById(R.id.tv_rvMed_name);
            tv_rvMed_maxDoseInfo = itemView.findViewById(R.id.tv_rvMed_maxDoseInfo);
            tv_rvMed_currentTotalDoseCount = itemView.findViewById(R.id.tv_rvMed_currentTotalDoseCount);
            tv_rvMed_takenInPast = itemView.findViewById(R.id.tv_rvMed_takenInPast);
            tv_rvMed_latestDoseExpiresIn = itemView.findViewById(R.id.tv_rvMed_latestDoseExpiresIn);
            btn_rvMed_more = itemView.findViewById(R.id.btn_rvMed_more);
            parentLayout = itemView.findViewById(R.id.layout_rvMed);
        }
    }
}
