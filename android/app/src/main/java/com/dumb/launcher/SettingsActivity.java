package com.dumb.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.GridLayout;
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
 *   one up or down, or edit one (pencil button: rename, or delete - its apps
 *   move to Other; Other itself cannot be renamed or deleted). Every
 *   category, Other included, gets a colour swatch that opens a picker for
 *   the background band colour shown behind its apps on the home grid.
 *
 *   Apps - every installed app A-Z (work copies marked " (Work)") with a
 *   category dropdown per app; choosing one moves the app there. Each row
 *   also has a Hide/Unhide button for the Inactive folder, and the tab
 *   header sets the inactivity threshold (week or month) after which an
 *   unused app moves to the Inactive folder on its own.
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

        renderThresholdRow();
        renderUsageAccessRow();

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
            final int[] current = { -1 };
            String now = CategoryStore.categoriesOf(this, app.key).get(0);
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(now)) {
                    current[0] = i;
                    spinner.setSelection(i, false);
                    break;
                }
            }
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    // Save only a real change; the synthetic setup event carries
                    // the current position and is ignored however it arrives.
                    if (position == current[0]) {
                        return;
                    }
                    current[0] = position;
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

            // Manual hide: the app moves to the Inactive folder and stays
            // there, even when used, until it is unhidden here.
            final boolean hidden = InactiveStore.isHidden(this, app.key);
            final Button hide = new Button(this);
            hide.setText(hidden ? "Unhide" : "Hide");
            hide.setTextSize(12);
            hide.setMinWidth(0);
            hide.setMinimumWidth(0);
            hide.setPadding(dp(10), 0, dp(10), 0);
            hide.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    InactiveStore.setHidden(SettingsActivity.this, app.key, !hidden);
                    renderList();
                }
            });
            LinearLayout.LayoutParams hideLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
            hideLp.setMargins(dp(4), 0, 0, 0);
            row.addView(hide, hideLp);

            list.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    /** Threshold row at the top of the Apps tab: week or month. */
    /**
     * Usage-access status for the Inactive folder, spelled out: without the
     * grant, auto-move is OFF because launches through this launcher cannot
     * prove an app went unused; manual Hide keeps working. With the grant,
     * real device usage decides, and work-profile apps are still never
     * auto-moved because the system reports no cross-profile usage stats.
     */
    private void renderUsageAccessRow() {
        final boolean granted = InactiveStore.hasUsageAccess(this);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(2), 0, dp(10));

        TextView label = new TextView(this);
        label.setTextSize(14);
        label.setTextColor(getColor(R.color.text_secondary));
        label.setText(granted
                ? "Auto-move to Inactive: on. Apps you have not opened for the"
                        + " period above move to the Inactive folder, counted by"
                        + " real device usage. Work apps are never auto-moved"
                        + " (the system shares no work-app usage); use Hide for"
                        + " those."
                : "Auto-move to Inactive: off. Tap to grant Usage access."
                        + " Without it this launcher can only see apps you open"
                        + " from here, so unused apps are not auto-moved rather"
                        + " than guessed. Hide below still moves any app"
                        + " manually.");
        row.addView(label, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (!granted) {
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(
                            android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS));
                }
            });
        }
        list.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void renderThresholdRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(2), 0, dp(10));

        TextView label = new TextView(this);
        label.setText("Move unused apps to Inactive after");
        label.setTextSize(14);
        label.setTextColor(getColor(R.color.text_secondary));
        row.addView(label, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final String[] options = {"Week", "Month"};
        final String[] values = {InactiveStore.WEEK, InactiveStore.MONTH};
        final Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        final int[] current = {
                InactiveStore.MONTH.equals(InactiveStore.threshold(this)) ? 1 : 0};
        spinner.setSelection(current[0], false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (position == current[0]) {
                    return;
                }
                current[0] = position;
                InactiveStore.setThreshold(SettingsActivity.this, values[position]);
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

            // Band colour swatch; every category gets one, Other included.
            final int currentColor = CategoryStore.colorFor(this, name);
            View swatch = new View(this);
            GradientDrawable chip = new GradientDrawable();
            chip.setShape(GradientDrawable.RECTANGLE);
            chip.setCornerRadius(dp(8));
            chip.setColor(currentColor);
            chip.setStroke(dp(1), 0x33000000);
            swatch.setBackground(chip);
            swatch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showColorPicker(name);
                }
            });
            LinearLayout.LayoutParams swatchLp = new LinearLayout.LayoutParams(dp(32), dp(32));
            swatchLp.setMargins(dp(6), 0, dp(6), 0);
            row.addView(swatch, swatchLp);

            // Rename affordance; Other is the fixed fallback bucket and keeps its name.
            if (!CategoryStore.OTHER.equals(name)) {
                Button edit = new Button(this);
                edit.setText("✎");
                edit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        promptEditCategory(name);
                    }
                });
                row.addView(edit, new LinearLayout.LayoutParams(dp(48), dp(44)));
            }

            list.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    /** Colour picker: preset swatches for the category's home band colour. */
    private void showColorPicker(final String name) {
        final int[] choices = {
                0xFFDBEAFE, 0xFFDCF5E4, 0xFFFEF3CD, 0xFFFCDEE9,
                0xFFE9E2FD, 0xFFD7F2F0, 0xFFFDE8D8, 0xFFECECF1,
                0xFF93C5FD, 0xFF86EFAC, 0xFFFCD34D, 0xFFF9A8D4,
                0xFFC4B5FD, 0xFF5EEAD4, 0xFFFDBA74, 0xFFD1D5DB
        };
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setPadding(dp(16), dp(16), dp(16), dp(16));
        for (final int color : choices) {
            View sw = new View(this);
            GradientDrawable chip = new GradientDrawable();
            chip.setShape(GradientDrawable.RECTANGLE);
            chip.setCornerRadius(dp(10));
            chip.setColor(color);
            chip.setStroke(dp(1), 0x33000000);
            sw.setBackground(chip);
            sw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CategoryStore.setColor(SettingsActivity.this, name, color);
                    dialog.dismiss();
                    renderList();
                }
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = dp(52);
            lp.height = dp(52);
            lp.setMargins(dp(6), dp(6), dp(6), dp(6));
            grid.addView(sw, lp);
        }
        dialog.setTitle(name + " colour");
        dialog.setView(grid);
        dialog.show();
    }

    /** Edit dialog for one category: rename it, or delete it (apps move to Other). */
    private void promptEditCategory(final String oldName) {
        final EditText input = new EditText(this);
        input.setText(oldName);
        input.setSingleLine(true);
        input.setSelection(input.getText().length());
        new AlertDialog.Builder(this)
                .setTitle("Edit category")
                .setView(input)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String next = input.getText().toString();
                        if (CategoryStore.renameCategory(SettingsActivity.this, oldName, next)) {
                            renderList();
                        }
                    }
                })
                .setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        confirmDeleteCategory(oldName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Confirm, then delete a category; its apps fall back to Other. */
    private void confirmDeleteCategory(final String name) {
        new AlertDialog.Builder(this)
                .setTitle("Delete " + name + "?")
                .setMessage("Apps in this category move to Other.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (CategoryStore.removeCategory(SettingsActivity.this, name)) {
                            renderList();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
