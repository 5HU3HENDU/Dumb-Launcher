package com.dumb.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dumb Launcher home screen: clock/date header with a settings gear, then one
 * viewport-filling grid of the installed apps grouped by category (A-Z inside a
 * category, no scrolling ever). Tiles are neutral; an app's own icon is the only
 * color. Categories, their order and app membership come from CategoryStore and
 * are editable in SettingsActivity. Long-pressing the grid also opens settings.
 */
public class MainActivity extends Activity {

    private GridArea gridArea;
    private TextView clockView;
    private TextView dateView;
    private TextView profileToggle;
    /** True while the grid shows the work profile, false for personal. */
    private boolean showWork;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            tickClock();
            handler.postDelayed(this, 15000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContentView());
        showWork = uiPrefs().getBoolean(KEY_SHOW_WORK, false);
        tickClock();
        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        tickClock();
        loadApps();
        handler.removeCallbacks(ticker);
        handler.postDelayed(ticker, 15000);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(ticker);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(ticker);
        super.onDestroy();
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
        header.setPadding(dp(14), dp(14), dp(14), dp(6));

        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);

        clockView = new TextView(this);
        clockView.setTextSize(34);
        clockView.setTypeface(Typeface.DEFAULT_BOLD);
        clockView.setTextColor(getColor(R.color.text_primary));
        clockView.setLetterSpacing(-0.01f);
        headerText.addView(clockView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dateView = new TextView(this);
        dateView.setTextSize(13);
        dateView.setTextColor(getColor(R.color.text_secondary));
        dateView.setPadding(0, dp(2), 0, 0);
        headerText.addView(dateView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        header.addView(headerText, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Profile switch, adjacent to the settings gear: shows one profile's
        // apps at a time. Only appears when the device has a work profile.
        profileToggle = new TextView(this);
        profileToggle.setTextSize(15);
        profileToggle.setTypeface(Typeface.DEFAULT_BOLD);
        profileToggle.setTextColor(getColor(R.color.text_secondary));
        profileToggle.setPadding(dp(10), dp(6), dp(10), dp(6));
        profileToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWork = !showWork;
                uiPrefs().edit().putBoolean(KEY_SHOW_WORK, showWork).apply();
                loadApps();
            }
        });
        header.addView(profileToggle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView gear = new TextView(this);
        gear.setText("⚙");
        gear.setTextSize(24);
        gear.setTextColor(getColor(R.color.text_secondary));
        gear.setPadding(dp(10), dp(4), dp(2), dp(4));
        gear.setContentDescription("Launcher settings");
        gear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });
        header.addView(gear, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        gridArea = new GridArea(this);
        gridArea.setPadding(dp(10), dp(6), dp(10), dp(6));
        gridArea.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                openSettings();
                return true;
            }
        });
        root.addView(gridArea, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        return root;
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void tickClock() {
        Date now = new Date();
        clockView.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
        dateView.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH).format(now));
    }

    private static final String PREFS_UI = "dumb_launcher_ui";
    private static final String KEY_SHOW_WORK = "show_work";

    private SharedPreferences uiPrefs() {
        return getSharedPreferences(PREFS_UI, MODE_PRIVATE);
    }

    private void loadApps() {
        List<ProfileApps.Installed> all = ProfileApps.list(this);

        // Fall back to personal if the work profile is gone or has no apps.
        boolean hasWork = false;
        for (ProfileApps.Installed inst : all) {
            if (inst.work) {
                hasWork = true;
                break;
            }
        }
        if (showWork && !hasWork) {
            showWork = false;
            uiPrefs().edit().putBoolean(KEY_SHOW_WORK, false).apply();
        }
        profileToggle.setVisibility(hasWork ? View.VISIBLE : View.GONE);
        profileToggle.setText(showWork ? "Work" : "Personal");

        List<String> cats = CategoryStore.categories(this);
        Map<String, List<AppInfo>> groups = new LinkedHashMap<>();
        for (String c : cats) {
            groups.put(c, new ArrayList<AppInfo>());
        }
        if (!groups.containsKey(CategoryStore.OTHER)) {
            groups.put(CategoryStore.OTHER, new ArrayList<AppInfo>());
        }

        long now = System.currentTimeMillis();
        boolean usageGranted = InactiveStore.hasUsageAccess(this);
        Map<String, Long> usage = usageGranted
                ? InactiveStore.usageLastUsed(this,
                        now - InactiveStore.thresholdMillis(this))
                : Collections.<String, Long>emptyMap();
        int inactiveCount = 0;
        for (ProfileApps.Installed inst : all) {
            InactiveStore.noteSeen(this, inst.key, now);
            if (inst.work != showWork) {
                continue;
            }
            AppInfo app = new AppInfo(inst.label, inst.key, inst.icon,
                    inst.component, inst.user);
            if (InactiveStore.inInactiveFolder(this, inst.key, now,
                    usage, usageGranted)) {
                // In the Inactive folder instead of its category; the folder
                // tile at the end of the grid opens the combined set.
                inactiveCount++;
                continue;
            }
            boolean placed = false;
            for (String cat : CategoryStore.categoriesOf(this, inst.key)) {
                List<AppInfo> group = groups.get(cat);
                if (group != null) {
                    app.category = cat;
                    group.add(app);
                    placed = true;
                }
            }
            if (!placed) {
                app.category = CategoryStore.OTHER;
                groups.get(CategoryStore.OTHER).add(app);
            }
        }

        List<AppInfo> ordered = new ArrayList<>();
        for (Map.Entry<String, List<AppInfo>> entry : groups.entrySet()) {
            List<AppInfo> group = entry.getValue();
            if (group.isEmpty()) {
                continue;
            }
            Collections.sort(group, new Comparator<AppInfo>() {
                @Override
                public int compare(AppInfo a, AppInfo b) {
                    return a.label.toLowerCase(Locale.ROOT)
                            .compareTo(b.label.toLowerCase(Locale.ROOT));
                }
            });
            ordered.addAll(group);
        }

        gridArea.setApps(ordered, inactiveCount);
    }

    private void openInactive() {
        startActivity(new Intent(this, InactiveActivity.class)
                .putExtra(InactiveActivity.EXTRA_SHOW_WORK, showWork));
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

    /** One installed, launchable app in one profile, with its icon. */
    private static final class AppInfo {
        final String label;
        final String key;
        String category = CategoryStore.OTHER;
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
     * Grid of app tiles that always fits exactly inside its bounds: it picks the
     * column count (4 to 8) that gives the largest tiles, then centers the grid.
     * There is deliberately no scrolling; tiles shrink to fit instead.
     */
    private final class GridArea extends ViewGroup {

        private final Paint bandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bandRect = new RectF();
        private int lastCols;
        private int cellW;
        private int cellH;
        private int cellSize;
        private int gapPx;

        GridArea(Context context) {
            super(context);
        }

        void setApps(List<AppInfo> apps, int inactiveCount) {
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
            // The Inactive folder sits as the last tile, after every active app.
            if (inactiveCount > 0) {
                FolderTileView folder = new FolderTileView(getContext());
                folder.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openInactive();
                    }
                });
                addView(folder);
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
            lastCols = cols;
            cellW = cw;
            cellH = ch;
            cellSize = bestCell;
            gapPx = gap;

            for (int i = 0; i < n; i++) {
                int col = i % cols;
                int row = i / cols;
                int cellLeft = padL + col * cw + (cw - bestCell) / 2;
                int cellTop = padT + offY + row * ch + (ch - bestCell) / 2;
                getChildAt(i).layout(cellLeft + gap, cellTop + gap,
                        cellLeft + bestCell - gap, cellTop + bestCell - gap);
            }
        }

        /**
         * Category bands: one rounded segment per maximal run of same-category
         * tiles within a row, drawn behind the tiles. The grid itself stays
         * continuous - a category can end mid-row and the next category's band
         * starts at the next cell; a category that wraps gets a segment per row.
         */
        private void drawCategoryBands(Canvas canvas) {
            int n = getChildCount();
            if (n == 0 || lastCols == 0) {
                return;
            }
            int hx = (cellW - cellSize) / 2 + gapPx - dp(2);
            int hy = (cellH - cellSize) / 2 + gapPx - dp(2);
            int min = dp(2);
            if (hx < min) {
                hx = min;
            }
            if (hy < min) {
                hy = min;
            }
            int i = 0;
            while (i < n) {
                View child = getChildAt(i);
                if (!(child instanceof TileView)) {
                    // The Inactive folder tile carries no category band.
                    i++;
                    continue;
                }
                TileView first = (TileView) child;
                String cat = first.app.category;
                int row = i / lastCols;
                int j = i;
                while (j + 1 < n && (j + 1) / lastCols == row
                        && getChildAt(j + 1) instanceof TileView) {
                    TileView next = (TileView) getChildAt(j + 1);
                    if (!next.app.category.equals(cat)) {
                        break;
                    }
                    j++;
                }
                TileView last = (TileView) getChildAt(j);
                bandRect.set(first.getLeft() - hx, first.getTop() - hy,
                        last.getRight() + hx, last.getBottom() + hy);
                bandPaint.setColor(CategoryStore.colorFor(getContext(), cat));
                float radius = Math.min(dp(16),
                        Math.min(bandRect.width(), bandRect.height()) / 2f);
                canvas.drawRoundRect(bandRect, radius, radius, bandPaint);
                i = j + 1;
            }
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            drawCategoryBands(canvas);
            super.dispatchDraw(canvas);
        }
    }

    /**
     * One tile: just the app's own icon, centered, no per-tile background
     * square and no name label (icons only per the owner's design call; names
     * still show in settings). The category band behind it is drawn by
     * GridArea. The icon takes most of the cell so it reads larger without
     * changing the grid.
     */
    private static final class TileView extends View {

        private final AppInfo app;
        private final Drawable icon;
        private int iconSize;
        private int iconTop;

        TileView(Context context, AppInfo app) {
            super(context);
            this.app = app;
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

    /**
     * The Inactive folder tile: a greyish folder with a small clock badge,
     * always the last tile after the active apps. Tapping it opens
     * InactiveActivity with the combined inactive and manually hidden set.
     */
    private final class FolderTileView extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private int iconSize;
        private int iconTop;

        FolderTileView(Context context) {
            super(context);
            setContentDescription("Inactive folder");
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
            if (iconSize <= 0) {
                return;
            }
            float left = (getWidth() - iconSize) / 2f;
            float top = iconTop;
            float size = iconSize;

            // Folder tab (slightly darker grey), then the folder body.
            float tabW = size * 0.44f;
            float tabH = size * 0.16f;
            paint.setColor(0xFF868D96);
            rect.set(left, top, left + tabW, top + tabH + size * 0.06f);
            canvas.drawRoundRect(rect, size * 0.08f, size * 0.08f, paint);

            paint.setColor(0xFFA6ADB5);
            float bodyTop = top + tabH * 0.7f;
            rect.set(left, bodyTop, left + size, top + size);
            canvas.drawRoundRect(rect, size * 0.1f, size * 0.1f, paint);

            // Clock badge, bottom-right: white face, grey ring and hands.
            float badgeR = size * 0.24f;
            float bx = left + size - badgeR * 0.9f;
            float by = top + size - badgeR * 0.9f;
            paint.setColor(0xFFFFFFFF);
            canvas.drawCircle(bx, by, badgeR, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2f, size * 0.035f));
            paint.setColor(0xFF6E747B);
            canvas.drawCircle(bx, by, badgeR, paint);
            paint.setStrokeWidth(Math.max(2f, size * 0.04f));
            canvas.drawLine(bx, by, bx, by - badgeR * 0.5f, paint);
            canvas.drawLine(bx, by, bx + badgeR * 0.38f, by + badgeR * 0.12f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }
}
