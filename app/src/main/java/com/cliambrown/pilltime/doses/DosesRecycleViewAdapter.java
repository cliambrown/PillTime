package com.cliambrown.pilltime.doses;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
import com.cliambrown.pilltime.utilities.ThemeHelper;
import com.cliambrown.pilltime.utilities.Utils;

import java.util.ArrayList;
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

    public void setData(Med med) {
        this.med = med;
        this.doses = med.getDoses();
        notifyDataSetChanged();
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
            holder.btn_med_loadMore.setOnClickListener(view -> mApp.loadMoreDoses(med));
            return;
        }

        holder.dose = doses.get(holder.getBindingAdapterPosition());

        long takenAt = holder.dose.getTakenAt();
        holder.tv_rvDose_takenAt.setText(Utils.buildTimeOnDateString(context, takenAt));

        long expiresAtUnix = holder.dose.getTakenAt() + med.getDoseDurationInSeconds();
        holder.tv_rvDose_expiresAt.setText(Utils.buildTimeOnDateString(context, expiresAtUnix));

        holder.updateTimes();

        holder.ll_rvDose_doseInfo.setOnClickListener(view -> {
            Intent intent = new Intent(context, EditDoseActivity.class);
            intent.putExtra("medID", holder.dose.getMedID());
            intent.putExtra("doseID", holder.dose.getId());
            context.startActivity(intent);
        });

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
                            .setPositiveButton(R.string.yes, (dialog, id) -> {
                                mApp.removeDose(holder.dose);
                                DosesRecycleViewAdapter.this.notifyItemRemoved(holder.getBindingAdapterPosition());
                            })
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
                                int itemCount = doses.size()- position1;
                                mApp.removeDoseAndOlder(holder.med, holder.dose);
                                DosesRecycleViewAdapter.this.notifyItemRangeRemoved(position1, itemCount);
                                holder.updateLoadMore();
                                DosesRecycleViewAdapter.this.notifyItemChanged(doses.size());
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
        LinearLayout ll_rvDose_doseInfo;
        ImageView iv_rvDose_clock;
        ImageView iv_rvDose_notification;
        TextView tv_rvDose_expires;
        TextView tv_rvDose_expiresAt;
        TextView tv_rvDose_taken;
        TextView tv_rvDose_takenAt;
        ImageButton btn_rvDose_more;
        Button btn_med_loadMore;
        Dose dose;
        Med med;
        Context context;

        public DoseViewHolder(@NonNull View itemView) {
            super(itemView);
            ll_rvDose_doseInfo = itemView.findViewById(R.id.ll_rvDose_doseInfo);
            iv_rvDose_clock = itemView.findViewById(R.id.iv_rvDose_clock);
            iv_rvDose_notification = itemView.findViewById(R.id.iv_rvDose_notification);
            tv_rvDose_expires = itemView.findViewById(R.id.tv_rvDose_expires);
            tv_rvDose_expiresAt = itemView.findViewById(R.id.tv_rvDose_expiresAt);
            tv_rvDose_taken = itemView.findViewById(R.id.tv_rvDose_taken);
            tv_rvDose_takenAt = itemView.findViewById(R.id.tv_rvDose_takenAt);
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
                clockIconID = R.drawable.ic_baseline_access_time_filled_24;
                clockTextColor = ThemeHelper.getThemeAttr(R.attr.greenText, context);
                if (dose.getNotify()) {
                    notifTextColor = ThemeHelper.getThemeAttr(R.attr.cyanText, context);
                    if (dose.getNotifySound()) {
                        notifIconID = R.drawable.ic_baseline_notifications_active_24;
                    } else {
                        notifIconID = R.drawable.ic_baseline_notifications_24;
                    }
                } else {
                    notifTextColor = ThemeHelper.getThemeAttr(R.attr.lightText, context);
                    notifIconID = R.drawable.ic_baseline_notifications_off_24;
                }
                iv_rvDose_notification.setImageResource(notifIconID);
                iv_rvDose_notification.setColorFilter(notifTextColor);
                iv_rvDose_notification.setVisibility(View.VISIBLE);
            } else {
                clockIconID = R.drawable.ic_baseline_access_time_24;
                clockTextColor = ThemeHelper.getThemeAttr(R.attr.lighterText, context);
                iv_rvDose_notification.setVisibility(View.GONE);
            }
            iv_rvDose_clock.setImageResource(clockIconID);
            iv_rvDose_clock.setColorFilter(clockTextColor);

            String colorName = med.getColor();
            int attrResourceID = Utils.getResourceIdentifier(context, colorName + "Text", "attr");
            int textColor = ThemeHelper.getThemeAttr(attrResourceID, context);
            ll_rvDose_doseInfo.getBackground().setColorFilter(
                    new PorterDuffColorFilter(textColor, PorterDuff.Mode.SRC));

            String countString = Utils.getStrFromDbl(dose.getCount());
            List<List<ParcelableSpan>> spansList = new ArrayList<>();
            List<ParcelableSpan> spans = new ArrayList<>();
            spans.add(new StyleSpan(Typeface.BOLD));
            spansList.add(spans);
            spans = new ArrayList<>();
            spans.add(new StyleSpan(Typeface.BOLD));
            spansList.add(spans);
            String unformatted;
            if (dose.getExpiresAt() > System.currentTimeMillis() / 1000L) {
                unformatted = context.getResources().getQuantityString(R.plurals.expires, (int) dose.getCount());
            } else {
                unformatted = context.getResources().getQuantityString(R.plurals.expired, (int) dose.getCount());
            }
            tv_rvDose_expires.setText(Utils.styleString(unformatted, spansList, countString, dose.getExpiresAtTimeAgo()));

            spansList = new ArrayList<>();
            spans = new ArrayList<>();
            spans.add(new StyleSpan(Typeface.BOLD));
            spansList.add(spans);
            unformatted = context.getString(R.string.taken);
            tv_rvDose_taken.setText(Utils.styleString(unformatted, spansList, dose.getTakenAtTimeAgo()));
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
