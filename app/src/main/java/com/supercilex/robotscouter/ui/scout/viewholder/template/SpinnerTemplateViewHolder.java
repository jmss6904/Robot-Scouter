package com.supercilex.robotscouter.ui.scout.viewholder.template;

import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;

import com.google.firebase.database.Query;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.ui.scout.viewholder.SpinnerViewHolder;

import java.util.ArrayList;

// TODO: 12/18/2016 let user edit spinner list items
public class SpinnerTemplateViewHolder extends SpinnerViewHolder implements View.OnFocusChangeListener {
    public SpinnerTemplateViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    public void bind(ScoutMetric<ArrayList<String>> metric,
                     Query query,
                     SimpleItemAnimator animator) {
        super.bind(metric, query, animator);
        mName.setOnFocusChangeListener(this);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) updateMetricName(mName.getText().toString());
    }
}