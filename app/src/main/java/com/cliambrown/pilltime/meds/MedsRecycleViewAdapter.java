package com.cliambrown.pilltime.meds;

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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cliambrown.pilltime.PillTimeApplication;
import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.doses.Dose;
import com.cliambrown.pilltime.doses.EditDoseActivity;
import com.cliambrown.pilltime.utilities.ThemeHelper;
import com.cliambrown.pilltime.utilities.Utils;

import java.util.ArrayList;
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

    public void setData(List<Med> meds) {
        this.meds = meds;
        notifyDataSetChanged();
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

        holder.ll_rvMed_medInfo.setOnClickListener(view -> {
            Intent intent = new Intent(context, MedActivity.class);
            intent.putExtra("id", medID);
            context.startActivity(intent);
        });

        holder.btn_rvMed_add.setOnClickListener(view -> {
            Intent intent = new Intent(context, EditDoseActivity.class);
            intent.putExtra("medID", medID);
            context.startActivity(intent);
        });

        holder.btn_rvMed_more.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(context, holder.btn_rvMed_more);
            popupMenu.inflate(R.menu.med_option_menu);

            popupMenu.setOnMenuItemClickListener(menuItem -> {
                Intent intent;
                int itemID = menuItem.getItemId();
                if (itemID == R.id.mi_med_option_edit) {
                    intent = new Intent(context, EditMedActivity.class);
                    intent.putExtra("id", medID);
                    context.startActivity(intent);
                    return true;
                }
                if (itemID == R.id.mi_med_option_history) {
                    intent = new Intent(context, MedActivity.class);
                    intent.putExtra("id", medID);
                    context.startActivity(intent);
                    return true;
                }
                if (itemID == R.id.mi_med_option_delete) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(R.string.dialog_delete_medication)
                            .setTitle(R.string.delete)
                            .setPositiveButton(R.string.yes, (dialog, id) -> {
                                mApp.removeMedById(medID);
                                MedsRecycleViewAdapter.this.notifyItemRemoved(holder.getBindingAdapterPosition());
                            })
                            .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
                    builder.show();
                }
                return false;
            });

            popupMenu.show();
        });
    }

    @SuppressWarnings("UnnecessaryReturnStatement")
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
        TextView tv_rvMed_takenInPast;
        TextView tv_rvMed_latestDoseExpiresIn;
        TextView tv_rvMed_lastTaken;
        LinearLayout ll_rvMed_inventory;
        TextView tv_rvMed_inventory;
        ImageButton btn_rvMed_more;
        Med med;
        Context context;

        public MedViewHolder(@NonNull View itemView) {
            super(itemView);
            btn_rvMed_add = itemView.findViewById(R.id.btn_rvMed_add);
            ll_rvMed_medInfo = itemView.findViewById(R.id.ll_rvMed_medInfo);
            tv_rvMed_name = itemView.findViewById(R.id.tv_rvMed_name);
            tv_rvMed_maxDoseInfo = itemView.findViewById(R.id.tv_rvMed_maxDoseInfo);
            tv_rvMed_takenInPast = itemView.findViewById(R.id.tv_rvMed_takenInPast);
            tv_rvMed_latestDoseExpiresIn = itemView.findViewById(R.id.tv_rvMed_latestDoseExpiresIn);
            tv_rvMed_lastTaken = itemView.findViewById(R.id.tv_rvMed_lastTaken);
            ll_rvMed_inventory = itemView.findViewById(R.id.ll_rvMed_inventory);
            tv_rvMed_inventory = itemView.findViewById(R.id.tv_rvMed_inventory);
            btn_rvMed_more = itemView.findViewById(R.id.btn_rvMed_more);
        }

        public void updateTimes() {
            if (med == null) return;
            med.updateTimes();
            double currentTotalDoseCount = med.getActiveDoseCount();
            Dose latestDose = med.getLatestDose();
            Dose nextExpiringDose = med.getNextExpiringDose();
            if (latestDose == null || currentTotalDoseCount == 0 || nextExpiringDose == null) {
                tv_rvMed_latestDoseExpiresIn.setVisibility(View.GONE);
            } else {
                double nextExpiringDoseCount = nextExpiringDose.getCount();
                long expiresAtUnix = nextExpiringDose.getTakenAt() + med.getDoseDurationInSeconds();
                String timeAgo = Utils.getRelativeTimeSpanString(context, expiresAtUnix, false);
                String countStr = Utils.getStrFromDbl(nextExpiringDoseCount);
                String unformatted = context.getResources().getQuantityString(R.plurals.expires,
                    (int) nextExpiringDoseCount);
                List<List<ParcelableSpan>> spansList = new ArrayList<>();
                List<ParcelableSpan> spans = new ArrayList<>();
                spans.add(new StyleSpan(Typeface.BOLD));
                spansList.add(spans);
                spans = new ArrayList<>();
                spans.add(new StyleSpan(Typeface.BOLD));
                spansList.add(spans);
                tv_rvMed_latestDoseExpiresIn.setText(Utils.styleString(unformatted, spansList, countStr,
                        timeAgo + " (" + Utils.simpleFutureTime(context, expiresAtUnix) + ")"));
                tv_rvMed_latestDoseExpiresIn.setVisibility(View.VISIBLE);
            }
            String timeAgoString;
            if (latestDose == null) {
                timeAgoString = context.getString(R.string.never);
            } else {
                timeAgoString = Utils.getRelativeTimeSpanString(context, latestDose.getTakenAt(), true);
            }
            String unformatted = context.getString(R.string.last_taken);
            List<List<ParcelableSpan>> spansList = new ArrayList<>();
            List<ParcelableSpan> spans = new ArrayList<>();
            spans.add(new StyleSpan(Typeface.BOLD));
            spansList.add(spans);
            tv_rvMed_lastTaken.setText(Utils.styleString(unformatted, spansList, timeAgoString));
        }

        public void updateInfo() {
            tv_rvMed_name.setText(med.getName());
            String colorName = med.getColor();
            int attrResourceID = Utils.getResourceIdentifier(context, colorName + "Text", "attr");
            int textColor = ThemeHelper.getThemeAttr(attrResourceID, context);
            try {
                int drawableID = Utils.getResourceIdentifier(context, "round_button_" + colorName, "drawable");
                btn_rvMed_add.setBackgroundResource(drawableID);
            } catch (Exception e) {
                // Do nothing
            }
            tv_rvMed_name.setTextColor(textColor);
            if (med.getIsInventoryTracked()) {
                if (med.getIsInventoryLow()) {
                    tv_rvMed_inventory.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warning, 0, 0, 0);
                } else {
                    tv_rvMed_inventory.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                }
                ll_rvMed_inventory.setVisibility(View.VISIBLE);
                tv_rvMed_inventory.setText(context.getString(R.string.inventory, med.getInventoryStr()));
            } else {
                ll_rvMed_inventory.setVisibility(View.GONE);
            }
            ll_rvMed_medInfo.getBackground().setColorFilter(new PorterDuffColorFilter(textColor, PorterDuff.Mode.SRC));
            tv_rvMed_maxDoseInfo.setText(med.getMaxDoseInfoStr());
            int colorAttrResId = R.attr.textColorPrimary;
            if (med.getActiveDoseCount() >= med.getMaxDose()) {
                colorAttrResId = R.attr.redText;
            }
            tv_rvMed_takenInPast.setText(Utils.buildTakenInPastString(context, colorAttrResId, med.getActiveDoseCount(),
                    med.getDoseHours()));
        }
    }
}
