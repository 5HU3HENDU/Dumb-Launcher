package com.dumb.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * User-controlled category data: the appearance order, user-created categories,
 * and per-app membership overrides. Persisted in SharedPreferences so edits
 * survive process death and app updates. On first run the order and membership
 * seed from CategoryMap's built-in data; after that, user edits win.
 */
public final class CategoryStore {

    private static final String PREFS = "dumb_launcher_categories";
    private static final String KEY_ORDER = "category_order";
    private static final String KEY_MEMBERS = "membership";

    /** Name of the fallback bucket for apps with no other membership. */
    public static final String OTHER = "Other";

    private CategoryStore() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Category names in home-screen appearance order. */
    public static List<String> categories(Context c) {
        String raw = prefs(c).getString(KEY_ORDER, null);
        if (raw != null) {
            try {
                JSONArray arr = new JSONArray(raw);
                List<String> out = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    out.add(arr.getString(i));
                }
                if (!out.isEmpty()) {
                    return out;
                }
            } catch (JSONException ignored) {
                // fall through to seed
            }
        }
        return new ArrayList<>(Arrays.asList(CategoryMap.NAMES));
    }

    private static void saveOrder(Context c, List<String> names) {
        JSONArray arr = new JSONArray();
        for (String n : names) {
            arr.put(n);
        }
        prefs(c).edit().putString(KEY_ORDER, arr.toString()).apply();
    }

    /** Add a new empty category at the end, unless the name already exists. */
    public static void addCategory(Context c, String name) {
        name = name == null ? "" : name.trim();
        if (name.isEmpty()) {
            return;
        }
        List<String> cats = categories(c);
        for (String n : cats) {
            if (n.equalsIgnoreCase(name)) {
                return;
            }
        }
        cats.add(name);
        saveOrder(c, cats);
    }

    /** Move the category at index by delta (-1 = up, +1 = down). */
    public static void moveCategory(Context c, int index, int delta) {
        List<String> cats = categories(c);
        int target = index + delta;
        if (index < 0 || index >= cats.size() || target < 0 || target >= cats.size()) {
            return;
        }
        String name = cats.remove(index);
        cats.add(target, name);
        saveOrder(c, cats);
    }

    private static boolean categoryExists(Context c, String name) {
        for (String n : categories(c)) {
            if (n.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Categories this app belongs to: the user's override when one is stored,
     * otherwise the built-in seed mapping, otherwise Other. The key is
     * profile-aware (see ProfileApps): a work copy can be categorized
     * separately from the personal copy of the same package. Seeds always
     * resolve by the bare package name, so a work copy with no override yet
     * lands in the same seeded category as its personal counterpart.
     */
    public static List<String> categoriesOf(Context c, String key) {
        String raw = prefs(c).getString(KEY_MEMBERS, null);
        if (raw != null) {
            try {
                JSONObject obj = new JSONObject(raw);
                if (obj.has(key)) {
                    JSONArray arr = obj.getJSONArray(key);
                    List<String> out = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        String n = arr.getString(i);
                        if (categoryExists(c, n)) {
                            out.add(n);
                        }
                    }
                    if (out.isEmpty()) {
                        out.add(OTHER);
                    }
                    return out;
                }
            } catch (JSONException ignored) {
                // fall through to seed
            }
        }
        List<String> out = new ArrayList<>();
        out.add(CategoryMap.NAMES[CategoryMap.categoryFor(ProfileApps.basePackage(key))]);
        return out;
    }

    /**
     * Rename a category in the order list and in every stored membership.
     * Other is the fixed fallback bucket and cannot be renamed; empty or
     * duplicate names are rejected. Returns true when the rename happened.
     */
    public static boolean renameCategory(Context c, String oldName, String newName) {
        newName = newName == null ? "" : newName.trim();
        if (newName.isEmpty() || OTHER.equals(oldName)) {
            return false;
        }
        List<String> cats = categories(c);
        int idx = -1;
        for (int i = 0; i < cats.size(); i++) {
            if (cats.get(i).equals(oldName)) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return false;
        }
        for (String n : cats) {
            if (!n.equals(oldName) && n.equalsIgnoreCase(newName)) {
                return false;
            }
        }
        cats.set(idx, newName);
        saveOrder(c, cats);
        SharedPreferences p = prefs(c);
        try {
            JSONObject obj = new JSONObject(p.getString(KEY_MEMBERS, "{}"));
            JSONArray keys = obj.names();
            if (keys != null) {
                for (int i = 0; i < keys.length(); i++) {
                    String k = keys.getString(i);
                    JSONArray arr = obj.getJSONArray(k);
                    JSONArray next = new JSONArray();
                    for (int j = 0; j < arr.length(); j++) {
                        String n = arr.getString(j);
                        next.put(n.equals(oldName) ? newName : n);
                    }
                    obj.put(k, next);
                }
                p.edit().putString(KEY_MEMBERS, obj.toString()).apply();
            }
        } catch (JSONException ignored) {
            // membership rewrite is best-effort; stale names resolve to Other
        }
        return true;
    }

    /**
     * Delete a category from the order list. Stored memberships that name it
     * resolve to Other on the next read (categoriesOf filters names that no
     * longer exist), so its apps land in Other without a data rewrite.
     * Other itself cannot be deleted. Returns true when removed.
     */
    public static boolean removeCategory(Context c, String name) {
        if (OTHER.equals(name)) {
            return false;
        }
        List<String> cats = categories(c);
        boolean removed = false;
        for (int i = 0; i < cats.size(); i++) {
            if (cats.get(i).equals(name)) {
                cats.remove(i);
                removed = true;
                break;
            }
        }
        if (!removed) {
            return false;
        }
        saveOrder(c, cats);
        return true;
    }

    /** Store the user's membership choice for one app key, in home order. */
    public static void setMembership(Context c, String key, Set<String> cats) {
        SharedPreferences p = prefs(c);
        JSONObject obj;
        try {
            obj = new JSONObject(p.getString(KEY_MEMBERS, "{}"));
        } catch (JSONException e) {
            obj = new JSONObject();
        }
        JSONArray arr = new JSONArray();
        for (String n : categories(c)) {
            if (cats.contains(n)) {
                arr.put(n);
            }
        }
        try {
            obj.put(key, arr);
        } catch (JSONException ignored) {
            return;
        }
        p.edit().putString(KEY_MEMBERS, obj.toString()).apply();
    }
}
