package com.cliambrown.pilltime;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
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
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull MedViewHolder holder, int position) {
        Med med = meds.get(position);
        holder.tv_rvMed_name.setText(med.getName());
        holder.tv_rvMed_doseInfo.setText(med.getDoseInfo());
        double currentCount = med.getCurrentTotalDoseCount();
        holder.tv_rvMed_currentDoseVal.setText(Utils.getStrFromDbl(currentCount));
        if (currentCount >= (long) med.getMaxDose()) {
            holder.tv_rvMed_currentDoseVal.setTextColor(context.getResources().getColor(R.color.red_500));
        } else {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = context.getTheme();
            theme.resolveAttribute(R.attr.textColorPrimary, typedValue, true);
            @ColorInt int color = typedValue.data;
            holder.tv_rvMed_currentDoseVal.setTextColor(color);
        }
        String expiresStr = "";
        String ldExpiresStr = null;
        if (currentCount > 0L) {
            ldExpiresStr = med.getLatestDoseExpiresAtString();
        }
        if (ldExpiresStr != null) {
            expiresStr = "Next dose expires: <b>" + ldExpiresStr + "</b>";
            holder.tv_rvMed_latestDoseExpiresAt.setText(Html.fromHtml(expiresStr));
            holder.tv_rvMed_latestDoseExpiresAt.setVisibility(View.VISIBLE);
        } else {
            holder.tv_rvMed_latestDoseExpiresAt.setText("");
            holder.tv_rvMed_latestDoseExpiresAt.setVisibility(View.GONE);
        }

        // Set up child RecyclerView
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context);
        holder.activeDosesRecyclerView.setLayoutManager(layoutManager);

        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
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
    }

    @Override
    public int getItemCount() {
        return meds.size();
    }

    public static class MedViewHolder extends RecyclerView.ViewHolder {
        TextView tv_rvMed_name;
        TextView tv_rvMed_doseInfo;
        ImageButton btn_rvMed_more;
        TextView tv_rvMed_currentDoseVal;
        TextView tv_rvMed_latestDoseExpiresAt;
        ConstraintLayout parentLayout;
        public RecyclerView activeDosesRecyclerView;

        public MedViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_rvMed_name = itemView.findViewById(R.id.tv_rvMed_name);
            tv_rvMed_doseInfo = itemView.findViewById(R.id.tv_rvMed_doseInfo);
            btn_rvMed_more = itemView.findViewById(R.id.btn_rvMed_more);
            tv_rvMed_currentDoseVal = itemView.findViewById(R.id.tv_rvMed_currentDoseVal);
            tv_rvMed_latestDoseExpiresAt = itemView.findViewById(R.id.tv_rvMed_latestDoseExpiresAt);
            parentLayout = itemView.findViewById(R.id.layout_rvMed);
            activeDosesRecyclerView = itemView.findViewById(R.id.rv_rvMed_activeDoses);
        }
    }
}
