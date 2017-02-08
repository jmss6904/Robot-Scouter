package com.supercilex.robotscouter.data.model;

import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v7.widget.SimpleItemAnimator;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.Query;
import com.supercilex.robotscouter.util.Constants;

public class CounterMetric extends ScoutMetric<Integer> {
    @Exclude
    @Nullable
    private String mUnit;

    @RestrictTo(RestrictTo.Scope.TESTS)
    public CounterMetric() { // Needed for Firebase
        super();
    }

    public CounterMetric(String name, int value, @Nullable String unit) {
        super(name, value, MetricType.COUNTER);
        mUnit = unit;
    }

    @Keep
    @Nullable
    public String getUnit() {
        return mUnit;
    }

    @Keep
    public void setUnit(@Nullable String unit) {
        mUnit = unit;
    }

    public void setUnit(Query query, String unit, SimpleItemAnimator animator) {
        if (mUnit != null && mUnit.equals(unit)) return;
        mUnit = unit;

        animator.setSupportsChangeAnimations(false);
        query.getRef().child(Constants.FIREBASE_UNIT).setValue(mUnit);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CounterMetric metric = (CounterMetric) o;

        return mUnit == null ? metric.mUnit == null : mUnit.equals(metric.mUnit);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (mUnit == null ? 0 : mUnit.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + "\nUnit = " + mUnit;
    }
}
