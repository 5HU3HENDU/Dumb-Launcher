package com.dumb.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Membership editor for one category: every launchable app, A-Z, with a switch.
 * Membership is exclusive - switching an app on here removes it from whichever
 * category it was in; switching it off sends it to Other. Removing an app from
 * a category never uninstalls it.
 */
public class CategoryAppsActivity extends Activity {

    private static final String EXTRA_CATEGORY = "category";

    public static void start(Context context, String category) {
        Intent intent = new Intent(context, CategoryAppsActivity.class);
        intent.putExtra(EXTRA_CATEGORY, category);
        context.startActivity(intent);
    }

    private String category;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        category = getIntent().getStringExtra(EXTRA_CATEGORY);
        if (category == null) {
            category = CategoryStore.OTHER;
        }
        setContentView(buildContentView());
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
        title.setText("Apps in " + category);
        title.setTextSize(22);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getColor(R.color.text_primary));
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = new TextView(this);
        hint.setText("Switch an app on to move it here, off to send it to Other.");
        hint.setTextSize(13);
        hint.setTextColor(getColor(R.color.text_secondary));
        hint.setPadding(0, dp(4), 0, dp(10));
        root.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        String self = getPackageName();

        List<AppRow> apps = new ArrayList<>();
        for (ResolveInfo ri : pm.queryIntentActivities(intent, 0)) {
            String pkg = ri.activityInfo.packageName;
            if (pkg == null || pkg.equals(self)) {
                continue;
            }
            CharSequence labelCs = ri.loadLabel(pm);
            String label = labelCs == null ? pkg : labelCs.toString();
            apps.add(new AppRow(label, pkg, ri.loadIcon(pm)));
        }
        Collections.sort(apps, new Comparator<AppRow>() {
            @Override
            public int compare(AppRow a, AppRow b) {
                return a.label.toLowerCase(Locale.ROOT)
                        .compareTo(b.label.toLowerCase(Locale.ROOT));
            }
        });

        for (final AppRow app : apps) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(4), 0, dp(4));

            ImageView icon = new ImageView(this);
            icon.setImageDrawable(app.icon);
            row.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));

            TextView label = new TextView(this);
            label.setText(app.label);
            label.setTextSize(16);
            label.setTextColor(getColor(R.color.text_primary));
            label.setSingleLine(true);
            label.setPadding(dp(12), 0, dp(8), 0);
            row.addView(label, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            final Switch toggle = new Switch(this);
            toggle.setChecked(CategoryStore.categoriesOf(this, app.pkg).contains(category));
            toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                    Set<String> next = new LinkedHashSet<>();
                    next.add(isChecked ? category : CategoryStore.OTHER);
                    CategoryStore.setMembership(CategoryAppsActivity.this, app.pkg, next);
                }
            });
            row.addView(toggle, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            list.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        return root;
    }

    private static final class AppRow {
        final String label;
        final String pkg;
        final Drawable icon;

        AppRow(String label, String pkg, Drawable icon) {
            this.label = label;
            this.pkg = pkg;
            this.icon = icon;
        }
    }
}
