package com.cliambrown.pilltime.doses;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.text.ParcelableSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cliambrown.pilltime.PillTimeApplication;
import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.meds.Med;
import com.cliambrown.pilltime.meds.MedActivity;
import com.cliambrown.pilltime.utilities.ThemeHelper;
import com.cliambrown.pilltime.utilities.Utils;

import java.util.ArrayList;
import java.util.List;

public class DosesRecycleViewAdapter extends RecyclerView.Adapter<DosesRecycleViewAdapter.DoseViewHolder> {

    Med med;
    List<Dose> doses = new ArrayList<>();
    final Context context;
    MedActivity medActivity;
    final PillTimeApplication mApp;

    public DosesRecycleViewAdapter(Med med, Context context, MedActivity medActivity, PillTimeApplication mApp) {
        this.med = med;
        this.context = context;
        this.medActivity = medActivity;
        this.mApp = mApp;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setDoses(List<Dose> doses) {
        this.doses = doses;
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return (position == doses.size()) ? R.layout.recyclerview_load_more_doses : R.layout.recyclerview_dose;
    }

    @NonNull
    @Override
    public DoseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        DoseViewHolder holder = new DoseViewHolder(view, medActivity);
        holder.context = parent.getContext();
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull DoseViewHolder holder, int position) {

        holder.med = med;

        if (position == doses.size()) {
            holder.onDoseCountChanged();
            holder.btn_med_loadMore.setOnClickListener(view -> mApp.loadMoreDoses(med));
            return;
        }

        holder.dose = doses.get(holder.getBindingAdapterPosition());

        long takenAt = holder.dose.getTakenAt();
        holder.tv_rvDose_takenAt.setText(Utils.buildTimeOnDateString(context, takenAt));

        long expiresAtUnix = holder.dose.getTakenAt() + med.getDoseDurationInSeconds();
        holder.tv_rvDose_expiresAt.setText(Utils.buildTimeOnDateString(context, expiresAtUnix));

        holder.onTimesChanged();
        holder.onMedColorChanged();

        if (medActivity.isMultiSelectActive) {
            holder.btn_rvDose_more.setVisibility(View.GONE);
            holder.toggleSelected(medActivity.selectedDoseIDs.contains(holder.dose.getId()));
        } else {
            holder.btn_rvDose_more.setVisibility(View.VISIBLE);
            holder.toggleSelected(false);
            holder.btn_rvDose_more.setOnClickListener(view -> {
                PopupMenu popupMenu = new PopupMenu(context, holder.btn_rvDose_more);
                popupMenu.inflate(R.menu.dose_option_menu);

                popupMenu.setOnMenuItemClickListener(menuItem -> {
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
                        builder.setMessage(R.string.dialog_delete_dose)
                                .setTitle(R.string.delete)
                                .setPositiveButton(R.string.yes, (dialog, id) -> mApp.removeDose(holder.dose))
                                .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
                        builder.show();
                        return true;
                    }
                    if (itemID == R.id.mi_dose_option_delete_and_older) {
                        builder = new AlertDialog.Builder(context);
                        builder.setMessage(R.string.dialog_delete_dose_and_older)
                                .setTitle(R.string.delete_and_older)
                                .setPositiveButton(R.string.yes, (dialog, id) -> {
                                    int position1 = holder.getBindingAdapterPosition();
                                    int itemCount = doses.size() - position1;
                                    mApp.removeDoseAndOlder(holder.med, holder.dose);
                                    DosesRecycleViewAdapter.this.notifyItemRangeRemoved(position1, itemCount);
                                })
                                .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
                        builder.show();
                        return true;
                    }
                    return false;
                });

                popupMenu.show();
            });
        }
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
        switch (payloadStr) {
            case "multiselect_activated":
                holder.btn_rvDose_more.setVisibility(View.GONE);
                holder.onSelectChanged();
                return;
            case "multiselect_deactivated":
                holder.btn_rvDose_more.setVisibility(View.VISIBLE);
                holder.toggleSelected(false);
                return;
            case "dose_selected":
                holder.toggleSelected(true);
                return;
            case "dose_count_changed":
                holder.onDoseCountChanged();
                return;
            case "times_changed":
                holder.onTimesChanged();
                return;
            case "med_info_changed":
                holder.onMedColorChanged();
                return;
            default:
        }
    }

    @Override
    public int getItemCount() {
        return doses.size() + 1;
    }

    public static class DoseViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener, View.OnClickListener {
        View layout_rvDose;
        final LinearLayout ll_rvDose_doseInfo;
        final ImageView iv_rvDose_unselected;
        final ImageView iv_rvDose_selected;
        final ImageView iv_rvDose_expired;
        final ImageView iv_rvDose_active;
        final TextView tv_rvDose_expires;
        final TextView tv_rvDose_expiresAt;
        final TextView tv_rvDose_notificationWithSound;
        final TextView tv_rvDose_notificationWithoutSound;
        final TextView tv_rvDose_noNotification;
        final TextView tv_rvDose_taken;
        final TextView tv_rvDose_takenAt;
        final ImageButton btn_rvDose_more;
        final Button btn_med_loadMore;
        Dose dose;
        Med med;
        MedActivity medActivity;
        Context context;
        boolean isSelected = false;

        public DoseViewHolder(@NonNull View itemView, MedActivity medActivity) {
            super(itemView);
            this.medActivity = medActivity;
            layout_rvDose = itemView.findViewById(R.id.layout_rvDose);
            ll_rvDose_doseInfo = itemView.findViewById(R.id.ll_rvDose_doseInfo);
            iv_rvDose_unselected = itemView.findViewById(R.id.iv_rvDose_unselected);
            iv_rvDose_selected = itemView.findViewById(R.id.iv_rvDose_selected);
            iv_rvDose_expired = itemView.findViewById(R.id.iv_rvDose_expired);
            iv_rvDose_active = itemView.findViewById(R.id.iv_rvDose_active);
            tv_rvDose_expires = itemView.findViewById(R.id.tv_rvDose_expires);
            tv_rvDose_expiresAt = itemView.findViewById(R.id.tv_rvDose_expiresAt);
            tv_rvDose_notificationWithSound = itemView.findViewById(R.id.tv_rvDose_notificationWithSound);
            tv_rvDose_notificationWithoutSound = itemView.findViewById(R.id.tv_rvDose_notificationWithoutSound);
            tv_rvDose_noNotification = itemView.findViewById(R.id.tv_rvDose_noNotification);
            tv_rvDose_taken = itemView.findViewById(R.id.tv_rvDose_taken);
            tv_rvDose_takenAt = itemView.findViewById(R.id.tv_rvDose_takenAt);
            btn_rvDose_more = itemView.findViewById(R.id.btn_rvDose_more);
            btn_med_loadMore = itemView.findViewById(R.id.btn_med_loadMore);

            itemView.setOnLongClickListener(this);
            itemView.setOnClickListener(this);
            for (View view: Utils.getAllViewChildren(itemView)) {
                view.setOnLongClickListener(this);
                view.setOnClickListener(this);
            }
        }

        public void onTimesChanged() {
            if (dose == null) return;
            if (med == null) return;

            String unformattedExpires;

            if (dose.isActive()) {
                iv_rvDose_expired.setVisibility(View.GONE);
                iv_rvDose_active.setVisibility(View.VISIBLE);
                unformattedExpires = context.getResources().getQuantityString(R.plurals.amount_expires_in_time, (int) dose.getCount());
                if (dose.getNotify()) {
                    tv_rvDose_noNotification.setVisibility(View.GONE);
                    if (dose.getNotifySound()) {
                        tv_rvDose_notificationWithSound.setVisibility(View.VISIBLE);
                        tv_rvDose_notificationWithoutSound.setVisibility(View.GONE);
                    } else {
                        tv_rvDose_notificationWithSound.setVisibility(View.GONE);
                        tv_rvDose_notificationWithoutSound.setVisibility(View.VISIBLE);
                    }
                } else {
                    tv_rvDose_notificationWithSound.setVisibility(View.GONE);
                    tv_rvDose_notificationWithoutSound.setVisibility(View.GONE);
                    tv_rvDose_noNotification.setVisibility(View.VISIBLE);
                }
            } else {
                iv_rvDose_expired.setVisibility(View.VISIBLE);
                iv_rvDose_active.setVisibility(View.GONE);
                unformattedExpires = context.getResources().getQuantityString(R.plurals.amount_expired_time_ago, (int) dose.getCount());
                tv_rvDose_notificationWithSound.setVisibility(View.GONE);
                tv_rvDose_notificationWithoutSound.setVisibility(View.GONE);
                tv_rvDose_noNotification.setVisibility(View.GONE);
            }

            String countString = Utils.getStrFromDbl(dose.getCount());
            List<List<ParcelableSpan>> spansList = new ArrayList<>();
            List<ParcelableSpan> spans = new ArrayList<>();
            spans.add(new StyleSpan(Typeface.BOLD));
            spansList.add(spans);
            spans = new ArrayList<>();
            spans.add(new StyleSpan(Typeface.BOLD));
            spansList.add(spans);
            tv_rvDose_expires.setText(Utils.styleString(unformattedExpires, spansList, countString, dose.getExpiresAtTimeAgo()));

            spansList = new ArrayList<>();
            spans = new ArrayList<>();
            spans.add(new StyleSpan(Typeface.BOLD));
            spansList.add(spans);
            String unformattedTaken = context.getString(R.string.taken);
            tv_rvDose_taken.setText(Utils.styleString(unformattedTaken, spansList, dose.getTakenAtTimeAgo()));
        }

        public void onMedColorChanged() {
            int textColor = Utils.getTextColor(med.getColor(), context);
            iv_rvDose_active.setColorFilter(textColor);
            if (med == null || dose == null || !dose.isActive()) {
                textColor = ThemeHelper.getThemeAttr(R.attr.lighterText, context);
            }
            ll_rvDose_doseInfo.getBackground().setColorFilter(
                    new PorterDuffColorFilter(textColor, PorterDuff.Mode.SRC));
        }

        public void onDoseCountChanged() {
            if (btn_med_loadMore == null || med == null) return;
            if (med.hasLoadedAllDoses()) {
                btn_med_loadMore.setVisibility(View.GONE);
            } else {
                btn_med_loadMore.setVisibility(View.VISIBLE);
            }
        }

        public void toggleSelected() {
            toggleSelected(!isSelected);
        }

        public void toggleSelected(boolean newIsSelected) {
            isSelected = newIsSelected;
            onSelectChanged();
        }

        public void onSelectChanged() {
            if (!medActivity.isMultiSelectActive) {
                layout_rvDose.setBackgroundColor(Color.TRANSPARENT);
                iv_rvDose_unselected.setVisibility(View.GONE);
                iv_rvDose_selected.setVisibility(View.GONE);
                return;
            }
            if (isSelected) {
                layout_rvDose.setBackgroundColor(ThemeHelper.getThemeAttr(R.attr.highlightBackground, context));
                iv_rvDose_unselected.setVisibility(View.GONE);
                iv_rvDose_selected.setVisibility(View.VISIBLE);
            } else {
                layout_rvDose.setBackgroundColor(Color.TRANSPARENT);
                iv_rvDose_unselected.setVisibility(View.VISIBLE);
                iv_rvDose_selected.setVisibility(View.GONE);
            }
        }

        @SuppressLint("ResourceType")
        @Override
        public boolean onLongClick(View v) {
            if (medActivity.isMultiSelectActive) return false;
            medActivity.activateMultiSelect(dose.getId());
            toggleSelected();
            return true;
        }

        @Override
        public void onClick(View v) {
            if (!medActivity.isMultiSelectActive) {
                Intent intent = new Intent(context, EditDoseActivity.class);
                intent.putExtra("medID", dose.getMedID());
                intent.putExtra("doseID", dose.getId());
                context.startActivity(intent);
                return;
            }
            toggleSelected();
            medActivity.selectDose(dose.getId(), isSelected);
        }
    }
}
