package com.dumb.launcher;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.android.controller.ActivityController;

import java.io.File;
import java.io.FileOutputStream;

/** Renders real screens to PNGs with native graphics, for visual inspection. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class UiRenderTest {

    private static void draw(View root, String name) throws Exception {
        int w = 1080, h = 2000;
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.AT_MOST));
        root.layout(0, 0, w, Math.max(h, root.getMeasuredHeight()));
        Bitmap b = Bitmap.createBitmap(root.getWidth(), root.getHeight(),
                Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(b));
        File out = new File("build/render/" + name + ".png");
        out.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(out);
        b.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.close();
    }

    static boolean clickText(View root, String text) {
        if (root instanceof android.widget.TextView
                && text.equals(((android.widget.TextView) root).getText().toString())) {
            return root.performClick();
        }
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) {
                if (clickText(g.getChildAt(i), text)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    public void renderSettingsAppsTab() throws Exception {
        ActivityController<SettingsActivity> controller =
                Robolectric.buildActivity(SettingsActivity.class);
        controller.setup();
        View content = controller.get().findViewById(android.R.id.content);
        clickText(content, "Apps");
        draw(content, "settings_apps");
        controller.destroy();
    }

    @Test
    public void renderSettingsAppsTabNoAccess() throws Exception {
        android.app.AppOpsManager aom = (android.app.AppOpsManager)
                org.robolectric.RuntimeEnvironment.getApplication()
                        .getSystemService(android.content.Context.APP_OPS_SERVICE);
        org.robolectric.Shadows.shadowOf(aom).setMode(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                org.robolectric.RuntimeEnvironment.getApplication().getPackageName(),
                android.app.AppOpsManager.MODE_IGNORED);
        ActivityController<SettingsActivity> controller =
                Robolectric.buildActivity(SettingsActivity.class);
        controller.setup();
        View content = controller.get().findViewById(android.R.id.content);
        clickText(content, "Apps");
        draw(content, "settings_apps_noaccess");
        controller.destroy();
    }

    @Test
    public void renderHome() throws Exception {
        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class);
        controller.setup();
        draw(controller.get().findViewById(android.R.id.content), "home");
        controller.destroy();
    }

    @Test
    public void renderInactiveFolder() throws Exception {
        ActivityController<InactiveActivity> controller =
                Robolectric.buildActivity(InactiveActivity.class);
        controller.setup();
        draw(controller.get().findViewById(android.R.id.content), "inactive");
        controller.destroy();
    }
}
