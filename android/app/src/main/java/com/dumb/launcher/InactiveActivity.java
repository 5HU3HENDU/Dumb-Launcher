package com.dumb.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Inactive folder screen: one viewport-filling, no-scroll grid of every
 * app currently in the Inactive folder for the profile the home screen was
 * showing - automatically inactive apps and manually hidden apps together,
 * A-Z. Icons only, like the home grid.
 *
 * Opening an app here launches it. An automatically inactive app is back in
 * its original category from then on; a manually hidden app stays hidden
 * until it is unhidden in Settings.
 */
public class InactiveActivity extends Activity {

    /** Extra carrying which profile's folder to show (defaults to personal). */
    public static final String EXTRA_SHOW_WORK = "show_work";

    private boolean showWork;
    private GridArea gridArea;
    private TextView countView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        showWork = intent != null && intent.getBooleanExtra(EXTRA_SHOW_WORK, false);
        setContentView(buildContentView());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInactive();
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private View buildContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.bg));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(6), dp(14), dp(14), dp(6));

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextSize(30);
        back.setTextColor(getColor(R.color.text_secondary));
        back.setPadding(dp(10), dp(2), dp(10), dp(2));
        back.setContentDescription("Back");
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        header.addView(back, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("Inactive");
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(getColor(R.color.text_primary));
        headerText.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        countView = new TextView(this);
        countView.setTextSize(13);
        countView.setTextColor(getColor(R.color.text_secondary));
        countView.setPadding(0, dp(2), 0, 0);
        headerText.addView(countView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        header.addView(headerText, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        gridArea = new GridArea(this);
        gridArea.setPadding(dp(10), dp(6), dp(10), dp(6));
        root.addView(gridArea, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        return root;
    }

    /** Rebuild the folder contents: hidden or auto-inactive apps, A-Z. */
    private void loadInactive() {
        long now = System.currentTimeMillis();
        boolean usageGranted = InactiveStore.hasUsageAccess(this);
        Map<String, Long> usage = usageGranted
                ? InactiveStore.usageLastUsed(this,
                        now - InactiveStore.thresholdMillis(this))
                : Collections.<String, Long>emptyMap();
        List<AppInfo> inactive = new ArrayList<>();
        for (ProfileApps.Installed inst : ProfileApps.list(this)) {
            if (inst.work != showWork) {
                continue;
            }
            if (InactiveStore.inInactiveFolder(this, inst.key, now,
                    usage, usageGranted)) {
                inactive.add(new AppInfo(inst.label, inst.key, inst.icon,
                        inst.component, inst.user));
            }
        }
        Collections.sort(inactive, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo a, AppInfo b) {
                return a.label.toLowerCase(Locale.ROOT)
                        .compareTo(b.label.toLowerCase(Locale.ROOT));
            }
        });
        int n = inactive.size();
        countView.setText(n == 0 ? "No inactive apps"
                : n + (n == 1 ? " app" : " apps"));
        gridArea.setApps(inactive);
    }

    private void launch(AppInfo app) {
        InactiveStore.recordLaunch(this, app.key);
        LauncherApps la = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        if (la == null) {
            return;
        }
        try {
            la.startMainActivity(app.component, app.user, null, null);
        } catch (RuntimeException e) {
            // Profile paused/locked or activity unavailable: the platform owns
            // whatever happens next; we never bypass work-profile controls.
        }
    }

    /** One inactive app entry, mirroring MainActivity's tile data. */
    private static final class AppInfo {
        final String label;
        final String key;
        final Drawable icon;
        final ComponentName component;
        final UserHandle user;

        AppInfo(String label, String key, Drawable icon,
                ComponentName component, UserHandle user) {
            this.label = label;
            this.key = key;
            this.icon = icon;
            this.component = component;
            this.user = user;
        }
    }

    /**
     * Same fit-to-viewport grid math as the home screen (4-8 columns, tiles
     * shrink instead of scrolling), without category bands.
     */
    private final class GridArea extends ViewGroup {

        GridArea(Context context) {
            super(context);
        }

        void setApps(List<AppInfo> apps) {
            removeAllViews();
            for (final AppInfo app : apps) {
                TileView tile = new TileView(getContext(), app);
                tile.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        launch(app);
                    }
                });
                addView(tile);
            }
            requestLayout();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int w = MeasureSpec.getSize(widthMeasureSpec);
            int h = MeasureSpec.getSize(heightMeasureSpec);
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).measure(
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            }
            setMeasuredDimension(w, h);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int n = getChildCount();
            if (n == 0) {
                return;
            }
            int padL = getPaddingLeft();
            int padT = getPaddingTop();
            int w = r - l - padL - getPaddingRight();
            int h = b - t - padT - getPaddingBottom();
            if (w <= 0 || h <= 0) {
                return;
            }

            int bestCols = 4;
            int bestCell = 0;
            for (int cols = 4; cols <= 8; cols++) {
                int rows = (n + cols - 1) / cols;
                int cell = Math.min(w / cols, h / rows);
                if (cell > bestCell) {
                    bestCell = cell;
                    bestCols = cols;
                }
            }
            int cap = dp(110);
            if (bestCell > cap) {
                bestCell = cap;
            }

            int cols = bestCols;
            int rows = (n + cols - 1) / cols;
            int cw = w / cols;
            int ch = h / rows;
            int gap = dp(3);
            int offY = (h - ch * rows) / 2;

            for (int i = 0; i < n; i++) {
                int col = i % cols;
                int row = i / cols;
                int cellLeft = padL + col * cw + (cw - bestCell) / 2;
                int cellTop = padT + offY + row * ch + (ch - bestCell) / 2;
                getChildAt(i).layout(cellLeft + gap, cellTop + gap,
                        cellLeft + bestCell - gap, cellTop + bestCell - gap);
            }
        }
    }

    /** Icons-only tile, identical sizing to the home tiles. */
    private static final class TileView extends View {

        private final Drawable icon;
        private int iconSize;
        private int iconTop;

        TileView(Context context, AppInfo app) {
            super(context);
            this.icon = app.icon;
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (w <= 0 || h <= 0) {
                return;
            }
            iconSize = (int) (Math.min(w, h) * 0.85f);
            iconTop = (h - iconSize) / 2;
            if (iconTop < 0) {
                iconTop = 0;
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int cx = w / 2;
            if (icon != null && iconSize > 0) {
                icon.setBounds(cx - iconSize / 2, iconTop, cx + iconSize / 2, iconTop + iconSize);
                icon.draw(canvas);
            }
        }
    }
}
