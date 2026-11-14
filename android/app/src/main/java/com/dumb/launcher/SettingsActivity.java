package com.dumb.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Launcher settings: the category list in home-screen order. From here the
 * owner can add a category, move one up or down in the order, or tap a category
 * to choose which apps belong to it. Apps stay A-Z inside each category.
 */
public class SettingsActivity extends Activity {

    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContentView());
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderList();
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private View buildContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.bg));
        root.setPadding(dp(16), dp(16), dp(16), dp(8));

        TextView title = new TextView(this);
        title.setText("Launcher settings");
        title.setTextSize(22);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getColor(R.color.text_primary));
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = new TextView(this);
        hint.setText("Tap a category to choose its apps. Each app sits in one category. "
                + "Arrows change the order categories appear on the home screen.");
        hint.setTextSize(13);
        hint.setTextColor(getColor(R.color.text_secondary));
        hint.setPadding(0, dp(4), 0, dp(10));
        root.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button add = new Button(this);
        add.setText("+ New category");
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptNewCategory();
            }
        });
        root.addView(add, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return root;
    }

    private void promptNewCategory() {
        final EditText input = new EditText(this);
        input.setHint("Category name");
        input.setSingleLine(true);
        int pad = dp(16);
        input.setPadding(pad, pad, pad, pad);
        new AlertDialog.Builder(this)
                .setTitle("New category")
                .setView(input)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CategoryStore.addCategory(SettingsActivity.this,
                                input.getText().toString());
                        renderList();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void renderList() {
        list.removeAllViews();

        // Count apps per category once for the row subtitles.
        Map<String, Integer> counts = new HashMap<>();
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        String self = getPackageName();
        for (ResolveInfo ri : pm.queryIntentActivities(intent, 0)) {
            String pkg = ri.activityInfo.packageName;
            if (pkg == null || pkg.equals(self)) {
                continue;
            }
            String cat = CategoryStore.categoriesOf(this, pkg).get(0);
            Integer n = counts.get(cat);
            counts.put(cat, n == null ? 1 : n + 1);
        }

        final List<String> cats = CategoryStore.categories(this);
        for (int i = 0; i < cats.size(); i++) {
            final int index = i;
            final String name = cats.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(4), dp(6), dp(4), dp(6));

            Button up = new Button(this);
            up.setText("↑");
            up.setEnabled(index > 0);
            up.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CategoryStore.moveCategory(SettingsActivity.this, index, -1);
                    renderList();
                }
            });
            row.addView(up, new LinearLayout.LayoutParams(dp(48), dp(44)));

            Button down = new Button(this);
            down.setText("↓");
            down.setEnabled(index < cats.size() - 1);
            down.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CategoryStore.moveCategory(SettingsActivity.this, index, +1);
                    renderList();
                }
            });
            row.addView(down, new LinearLayout.LayoutParams(dp(48), dp(44)));

            LinearLayout text = new LinearLayout(this);
            text.setOrientation(LinearLayout.VERTICAL);
            text.setPadding(dp(10), 0, 0, 0);

            TextView label = new TextView(this);
            label.setText(name);
            label.setTextSize(17);
            label.setTextColor(getColor(R.color.text_primary));
            text.addView(label, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            Integer count = counts.get(name);
            TextView sub = new TextView(this);
            sub.setText(count == null ? "No apps" : count + (count == 1 ? " app" : " apps"));
            sub.setTextSize(12);
            sub.setTextColor(getColor(R.color.text_secondary));
            text.addView(sub, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            row.addView(text, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CategoryAppsActivity.start(SettingsActivity.this, name);
                }
            });

            list.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }
}
