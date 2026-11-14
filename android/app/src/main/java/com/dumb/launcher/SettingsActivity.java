package com.dumb.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Launcher settings with two tabs:
 *
 *   Categories - the category list in home-screen order: add a category, move
 *   one up or down, or tap one to choose which apps belong to it.
 *
 *   Apps - every installed app A-Z (work copies marked " (Work)") with a
 *   category dropdown per app; choosing one moves the app there.
 *
 * Both tabs edit the same exclusive membership: each app sits in exactly one
 * category. Apps stay A-Z inside each category on the home screen.
 */
public class SettingsActivity extends Activity {

    private LinearLayout list;
    private Button addButton;
    private Button tabApps;
    private Button tabCategories;
    private boolean showingApps;

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
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(getColor(R.color.text_primary));
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(0, dp(6), 0, dp(6));

        tabApps = new Button(this);
        tabApps.setText("Apps");
        tabApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showingApps = true;
                renderList();
            }
        });
        tabs.addView(tabApps, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        tabCategories = new Button(this);
        tabCategories.setText("Categories");
        tabCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showingApps = false;
                renderList();
            }
        });
        tabs.addView(tabCategories, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(tabs, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        addButton = new Button(this);
        addButton.setText("+ New category");
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptNewCategory();
            }
        });
        root.addView(addButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return root;
    }

    private void styleTabs() {
        tabApps.setTypeface(null, showingApps ? Typeface.BOLD : Typeface.NORMAL);
        tabCategories.setTypeface(null, showingApps ? Typeface.NORMAL : Typeface.BOLD);
        addButton.setVisibility(showingApps ? View.GONE : View.VISIBLE);
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
        styleTabs();
        list.removeAllViews();
        if (showingApps) {
            renderApps();
        } else {
            renderCategories();
        }
    }

    /** Apps tab: every installed app A-Z, with a category dropdown per app. */
    private void renderApps() {
        List<ProfileApps.Installed> apps = ProfileApps.list(this);
        Collections.sort(apps, new Comparator<ProfileApps.Installed>() {
            @Override
            public int compare(ProfileApps.Installed a, ProfileApps.Installed b) {
                int byLabel = a.label.toLowerCase(Locale.ROOT)
                        .compareTo(b.label.toLowerCase(Locale.ROOT));
                if (byLabel != 0) {
                    return byLabel;
                }
                return Boolean.compare(a.work, b.work);
            }
        });

        final List<String> cats = CategoryStore.categories(this);
        final String[] names = cats.toArray(new String[0]);

        for (final ProfileApps.Installed app : apps) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(4), 0, dp(4));

            ImageView icon = new ImageView(this);
            Drawable d = app.icon;
            if (d != null) {
                icon.setImageDrawable(d);
            }
            row.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));

            TextView label = new TextView(this);
            label.setText(app.display);
            label.setTextSize(16);
            label.setTextColor(getColor(R.color.text_primary));
            label.setSingleLine(true);
            label.setPadding(dp(12), 0, dp(8), 0);
            row.addView(label, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            final Spinner spinner = new Spinner(this);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, names);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            String now = CategoryStore.categoriesOf(this, app.key).get(0);
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(now)) {
                    spinner.setSelection(i, false);
                    break;
                }
            }
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                private boolean firstFire = true;

                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    // Spinner fires once when the selection is set up; ignore that.
                    if (firstFire) {
                        firstFire = false;
                        return;
                    }
                    Set<String> next = new LinkedHashSet<>();
                    next.add(names[position]);
                    CategoryStore.setMembership(SettingsActivity.this, app.key, next);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            row.addView(spinner, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            list.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    /** Categories tab: the ordered category list with reorder arrows. */
    private void renderCategories() {
        // Count apps per category once for the row subtitles. Profile-aware:
        // a work copy and a personal copy of one package count separately.
        Map<String, Integer> counts = new HashMap<>();
        for (ProfileApps.Installed inst : ProfileApps.list(this)) {
            String cat = CategoryStore.categoriesOf(this, inst.key).get(0);
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

            list.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }
}
