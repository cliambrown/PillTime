package com.cliambrown.pilltime.settings;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.cliambrown.pilltime.R;
import com.cliambrown.pilltime.utilities.ThemeHelper;

public class ClearDbPreference extends Preference {
    public ClearDbPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ClearDbPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ClearDbPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClearDbPreference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView tv = (TextView) holder.findViewById(android.R.id.title);
        tv.setTextColor(ThemeHelper.getThemeAttr(R.attr.redText, getContext()));
        tv.setTypeface(null, Typeface.BOLD);
        int dp12 = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                12,
                getContext().getResources().getDisplayMetrics()
        );
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) tv.getLayoutParams();
        params.topMargin = dp12;
        tv.setLayoutParams(params);
    }
}
