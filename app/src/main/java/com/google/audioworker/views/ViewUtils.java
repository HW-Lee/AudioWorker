package com.google.audioworker.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Collection;

public class ViewUtils {
    public static Spinner getSimpleSpinner(
            @NonNull Context ctx, Collection<? extends String> values) {
        return getSimpleSpinner(ctx, values, null);
    }

    public static Spinner getSimpleSpinner(
            @NonNull Context ctx,
            Collection<? extends String> values,
            AdapterView.OnItemSelectedListener l) {
        Spinner spinner = new Spinner(ctx);
        spinner.setAdapter(getSimpleAdapter(ctx, values));
        spinner.setOnItemSelectedListener(l);

        return spinner;
    }

    public static ArrayAdapter<String> getSimpleAdapter(
            @NonNull Context ctx, Collection<? extends String> values) {
        return new ArrayAdapter<>(
                ctx,
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                new ArrayList<>(values));
    }

    public static View getHorizontalBorder(@NonNull Context ctx, int borderWidth) {
        View border = new View(ctx);
        border.setLayoutParams(
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, borderWidth));
        return border;
    }
}
