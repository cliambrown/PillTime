package com.cliambrown.pilltime;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

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
        holder.context = parent.getContext();
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull MedViewHolder holder, int position) {

        holder.med = meds.get(position);
        int medID = holder.med.getId();

        holder.updateInfo();
        holder.updateTimes();

        holder.ll_rvMed_medInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, MedActivity.class);
                intent.putExtra("id", medID);
                context.startActivity(intent);
            }
        });

        holder.btn_rvMed_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, EditDoseActivity.class);
                intent.putExtra("med_id", medID);
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
                                intent.putExtra("id", medID);
                                context.startActivity(intent);
                                return true;
                            case R.id.mi_med_option_history:
                                intent = new Intent(context, MedActivity.class);
                                intent.putExtra("id", medID);
                                context.startActivity(intent);
                                return true;
                            case R.id.mi_med_option_delete:
                                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                builder.setMessage(R.string.dialog_delete_item)
                                        .setTitle(R.string.delete)
                                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                mApp.removeMedById(medID);
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
    }

    @Override
    public void onBindViewHolder(@NonNull MedViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
            return;
        }
        if (payloads.get(0) == "update_times") {
            holder.updateTimes();
            return;
        }
        if (payloads.get(0) == "update_info") {
            holder.updateInfo();
            return;
        }
    }

    @Override
    public int getItemCount() {
        return meds.size();
    }

    public static class MedViewHolder extends RecyclerView.ViewHolder {
        ImageButton btn_rvMed_add;
        LinearLayout ll_rvMed_medInfo;
        TextView tv_rvMed_name;
        TextView tv_rvMed_maxDoseInfo;
        TextView tv_rvMed_currentTotalDoseCount;
        TextView tv_rvMed_takenInPast;
        TextView tv_rvMed_latestDoseExpiresIn;
        ImageButton btn_rvMed_more;
        Med med;
        Context context;

        public MedViewHolder(@NonNull View itemView) {
            super(itemView);
            btn_rvMed_add = itemView.findViewById(R.id.btn_rvMed_add);
            ll_rvMed_medInfo = itemView.findViewById(R.id.ll_rvMed_medInfo);
            tv_rvMed_name = itemView.findViewById(R.id.tv_rvMed_name);
            tv_rvMed_maxDoseInfo = itemView.findViewById(R.id.tv_rvMed_maxDoseInfo);
            tv_rvMed_currentTotalDoseCount = itemView.findViewById(R.id.tv_rvMed_currentTotalDoseCount);
            tv_rvMed_takenInPast = itemView.findViewById(R.id.tv_rvMed_takenInPast);
            tv_rvMed_latestDoseExpiresIn = itemView.findViewById(R.id.tv_rvMed_latestDoseExpiresIn);
            btn_rvMed_more = itemView.findViewById(R.id.btn_rvMed_more);
        }

        public void updateTimes() {
            if (med == null) return;
            med.updateDoseStatus();
            double currentTotalDoseCount = med.getCurrentTotalDoseCount();
            tv_rvMed_currentTotalDoseCount.setText(Utils.getStrFromDbl(currentTotalDoseCount));
            if (currentTotalDoseCount >= (long) med.getMaxDose()) {
                tv_rvMed_currentTotalDoseCount.setTextColor(ThemeProvider.getThemeAttr(R.attr.redText, context));
            } else {
                tv_rvMed_currentTotalDoseCount.setTextColor(ThemeProvider.getThemeAttr(R.attr.textColorPrimary, context));
            }
            Dose latestDose = med.getLatestDose();
            if (latestDose == null || currentTotalDoseCount == 0) {
                tv_rvMed_latestDoseExpiresIn.setVisibility(View.GONE);
            } else {
                String expiresIn = med.getLatestDoseExpiresInStr();
                tv_rvMed_latestDoseExpiresIn.setText(expiresIn);
                tv_rvMed_latestDoseExpiresIn.setVisibility(View.VISIBLE);
            }
        }

        public void updateInfo() {
            int doseHours = med.getDoseHours();
            String takenInPast = " " + context.getString(R.string.taken_in_past) + " " +
                    doseHours + " " + context.getString(R.string.hours);
            tv_rvMed_name.setText(med.getName());
            String colorName = med.getColor();
            int attrResourceID = Utils.getResourceIdentifier(context, colorName + "Text", "attr");
            int textColor = ThemeProvider.getThemeAttr(attrResourceID, context);
            try {
                int drawableID = Utils.getResourceIdentifier(context, "round_button_" + colorName, "drawable");
                btn_rvMed_add.setBackgroundResource(drawableID);
            } catch (Exception e) {
                // Do nothing
            }
            tv_rvMed_name.setTextColor(textColor);
            tv_rvMed_maxDoseInfo.setText(med.getMaxDoseInfo());
            tv_rvMed_takenInPast.setText(takenInPast);
        }
    }
}
