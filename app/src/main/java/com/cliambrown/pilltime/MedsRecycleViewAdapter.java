package com.cliambrown.pilltime;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
        ConstraintLayout parentLayout;

        public MedViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_rvMed_name = itemView.findViewById(R.id.tv_rvMed_name);
            tv_rvMed_doseInfo = itemView.findViewById(R.id.tv_rvMed_doseInfo);
            btn_rvMed_more = itemView.findViewById(R.id.btn_rvMed_more);
            parentLayout = itemView.findViewById(R.id.layout_rvMed);
        }
    }
}
