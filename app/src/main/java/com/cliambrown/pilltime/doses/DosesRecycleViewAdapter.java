package com.cliambrown.pilltime.doses;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cliambrown.pilltime.PillTimeApplication;
import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.utilities.ThemeHelper;
import com.cliambrown.pilltime.utilities.Utils;
import com.cliambrown.pilltime.meds.Med;

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

    @Override
    public int getItemViewType(int position) {

        return (position == doses.size()) ? R.layout.recyclerview_load_more_doses : R.layout.recyclerview_dose;
    }

    @NonNull
    @Override
    public DoseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        DoseViewHolder holder = new DoseViewHolder(view);
        holder.context = parent.getContext();
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull DoseViewHolder holder, int position) {

        holder.med = med;

        if (position == doses.size()) {
            holder.updateLoadMore();
            holder.btn_med_loadMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mApp.loadMoreDoses(med);
                }
            });
            return;
        }

        holder.dose = doses.get(position);
        long takenAt = holder.dose.getTakenAt();

        holder.tv_rvDose_count.setText(Utils.getStrFromDbl(holder.dose.getCount()));
        long takenAtMs = takenAt * 1000L;
        String takenAtStr = DateUtils.formatDateTime(context, takenAtMs, DateUtils.FORMAT_SHOW_TIME).toLowerCase() + " " +
                context.getString(R.string.on) + " " +
                DateUtils.formatDateTime(context, takenAtMs, DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_YEAR);
        holder.tv_rvDose_takenAt.setText(takenAtStr);

        long expiresAtUnix = holder.dose.getTakenAt() + (med.getDoseHours() * 60L * 60L);
        long expiresAtMs = expiresAtUnix * 1000L;
        String expiresAtStr = DateUtils.formatDateTime(context, expiresAtMs, DateUtils.FORMAT_SHOW_TIME).toLowerCase() + " " +
                context.getString(R.string.on) + " " +
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
                        AlertDialog.Builder builder;
                        int itemID = menuItem.getItemId();
                        if (itemID == R.id.mi_dose_option_edit) {
                            Intent intent = new Intent(context, EditDoseActivity.class);
                            intent.putExtra("medID", holder.dose.getMedID());
                            intent.putExtra("doseID", holder.dose.getId());
                            context.startActivity(intent);
                            return true;
                        }
                        if (itemID == R.id.mi_dose_option_delete) {
                            builder = new AlertDialog.Builder(context);
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
                            return true;
                        }
                        if (itemID == R.id.mi_dose_option_delete_and_older) {
                            builder = new AlertDialog.Builder(context);
                            builder.setMessage(R.string.dialog_delete_item)
                                    .setTitle(R.string.delete_and_older)
                                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            int position = holder.getAdapterPosition();
                                            int itemCount = doses.size()- position;
                                            mApp.removeDoseAndOlder(holder.med, holder.dose);
                                            DosesRecycleViewAdapter.this.notifyItemRangeRemoved(position, itemCount);
                                            holder.updateLoadMore();
                                            DosesRecycleViewAdapter.this.notifyItemChanged(doses.size());
                                        }
                                    })
                                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.dismiss();
                                        }
                                    });
                            builder.show();
                            return true;
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
        String payloadStr;
        try {
            payloadStr = String.valueOf(payloads.get(0));
        } catch (Exception e) {
            return;
        }
        if (payloadStr.equals("update_show_more_btn")) {
            holder.updateLoadMore();
            DosesRecycleViewAdapter.this.notifyItemChanged(doses.size());
        }
        if (payloadStr.equals("update_times")) {
            holder.updateTimes();
        }
    }

    @Override
    public int getItemCount() {
        return doses.size() + 1;
    }

    public static class DoseViewHolder extends RecyclerView.ViewHolder {
        ImageView iv_rvDose_clock;
        ImageView iv_rvDose_notification;
        TextView tv_rvDose_count;
        TextView tv_rvDose_expires;
        TextView tv_rvDose_expiresAt;
        TextView tv_rvDose_expiresAtTimeAgo;
        TextView tv_rvDose_takenAt;
        TextView tv_rvDose_takenAtTimeAgo;
        ImageButton btn_rvDose_more;
        Button btn_med_loadMore;
        Dose dose;
        Med med;
        Context context;

        public DoseViewHolder(@NonNull View itemView) {
            super(itemView);
            iv_rvDose_clock = itemView.findViewById(R.id.iv_rvDose_clock);
            iv_rvDose_notification = itemView.findViewById(R.id.iv_rvDose_notification);
            tv_rvDose_count = itemView.findViewById(R.id.tv_rvDose_count);
            tv_rvDose_expires = itemView.findViewById(R.id.tv_rvDose_expires);
            tv_rvDose_expiresAt = itemView.findViewById(R.id.tv_rvDose_expiresAt);
            tv_rvDose_expiresAtTimeAgo = itemView.findViewById(R.id.tv_rvDose_expiresAtTimeAgo);
            tv_rvDose_takenAt = itemView.findViewById(R.id.tv_rvDose_takenAt);
            tv_rvDose_takenAtTimeAgo = itemView.findViewById(R.id.tv_rvDose_takenAtTimeAgo);
            btn_rvDose_more = itemView.findViewById(R.id.btn_rvDose_more);
            btn_med_loadMore = itemView.findViewById(R.id.btn_med_loadMore);
        }

        public void updateTimes() {
            if (dose == null) return;
            if (med == null) return;
            dose.updateTimes(med);
            int clockTextColor;
            int clockIconID;
            int notifTextColor;
            int notifIconID;
            if (dose.isActive()) {
                clockIconID = Utils.getResourceIdentifier(context, "ic_baseline_access_time_filled_24", "drawable");
                clockTextColor = ThemeHelper.getThemeAttr(R.attr.greenText, context);
                if (dose.getNotify()) {
                    notifTextColor = ThemeHelper.getThemeAttr(R.attr.cyanText, context);
                    if (dose.getNotifySound()) {
                        notifIconID = Utils.getResourceIdentifier(context, "ic_baseline_notifications_active_24", "drawable");
                    } else {
                        notifIconID = Utils.getResourceIdentifier(context, "ic_baseline_notifications_24", "drawable");
                    }
                } else {
                    notifTextColor = ThemeHelper.getThemeAttr(R.attr.lightText, context);
                    notifIconID = Utils.getResourceIdentifier(context, "ic_baseline_notifications_off_24", "drawable");
                }
                iv_rvDose_notification.setImageResource(notifIconID);
                iv_rvDose_notification.setColorFilter(notifTextColor);
                iv_rvDose_notification.setVisibility(View.VISIBLE);
            } else {
                clockIconID = Utils.getResourceIdentifier(context, "ic_baseline_access_time_24", "drawable");
                clockTextColor = ThemeHelper.getThemeAttr(R.attr.lighterText, context);
                tv_rvDose_expires.setText(context.getString(R.string.expired));
                iv_rvDose_notification.setVisibility(View.GONE);
            }
            long now = System.currentTimeMillis() / 1000L;
            if (dose.getExpiresAt() > now) {
                tv_rvDose_expires.setText(context.getString(R.string.expires));
            } else {
                tv_rvDose_expires.setText(context.getString(R.string.expired));
            }
            iv_rvDose_clock.setImageResource(clockIconID);
            iv_rvDose_clock.setColorFilter(clockTextColor);
            tv_rvDose_takenAtTimeAgo.setText(dose.getTakenAtTimeAgo());
            tv_rvDose_expiresAtTimeAgo.setText(dose.getExpiresAtTimeAgo());
        }

        public void updateLoadMore() {
            if (btn_med_loadMore == null || med == null) return;
            if (med.hasLoadedAllDoses()) {
                btn_med_loadMore.setVisibility(View.GONE);
            } else {
                btn_med_loadMore.setVisibility(View.VISIBLE);
            }
        }
    }
}
