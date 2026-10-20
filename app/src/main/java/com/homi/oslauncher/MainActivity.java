package com.homi.oslauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.ClipData;
import android.view.DragEvent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.widget.FrameLayout;
import android.net.Uri;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<AppItem> allApps = new ArrayList<>();

    private LinearLayout page;
    private LinearLayout content;
    private TextView clockView;
    private TextView dateView;
    private EditText appSearch;
    private ScrollView scrollView;
    private ImageView wallpaperView;
    private FrameLayout rootLayout;

    private SharedPreferences prefs;
    private boolean showingApps = false;
    private String currentQuery = "";

    private int accent;
    private int background;
    private int cardColor;
    private int clockSize;
    private Uri wallpaperUri;
    private int systemBottomInset = 0;
    private float gestureStartX = 0f;
    private float gestureStartY = 0f;
    private static final int REQUEST_WALLPAPER = 4201;
    private static final String FAVORITES_KEY = "favorite_apps";

    private final int[] accents = {
            Color.rgb(0, 220, 255),
            Color.rgb(170, 100, 255),
            Color.rgb(80, 230, 150),
            Color.rgb(255, 185, 70),
            Color.rgb(255, 100, 130)
    };

    private final String[] accentNames = {
            "سماوي", "بنفسجي", "أخضر", "ذهبي", "وردي"
    };

    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            if (clockView != null) {
                clockView.setText(
                        new SimpleDateFormat("HH:mm", Locale.getDefault())
                                .format(new Date())
                );
            }

            if (dateView != null) {
                dateView.setText(
                        new SimpleDateFormat(
                                "EEEE، d MMMM",
                                new Locale("ar")
                        ).format(new Date())
                );
            }

            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("homi_settings", MODE_PRIVATE);
        loadSettings();
        loadApps();
        showHome();
    }

    private void loadSettings() {
        int accentIndex = prefs.getInt("accent", 0);
        accentIndex = Math.max(0, Math.min(accentIndex, accents.length - 1));
        accent = prefs.getInt("custom_accent", accents[accentIndex]);
        String savedWallpaper = prefs.getString("wallpaper_uri", null);
        wallpaperUri = savedWallpaper == null ? null : Uri.parse(savedWallpaper);

        clockSize = prefs.getInt("clock_size", 58);

        boolean light = prefs.getBoolean("light_background", false);

        if (light) {
            background = Color.rgb(239, 243, 249);
            cardColor = Color.WHITE;
        } else {
            background = Color.rgb(10, 14, 25);
            cardColor = Color.rgb(23, 30, 47);
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        int surfaceColor = color;
        if (wallpaperUri != null && color == cardColor) {
            surfaceColor = Color.argb(166, Color.red(color), Color.green(color), Color.blue(color));
        }
        drawable.setColor(surfaceColor);
        drawable.setCornerRadius(dp(radius));
        if (wallpaperUri != null) {
            drawable.setStroke(dp(1), Color.argb(54, 255, 255, 255));
        }
        return drawable;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);

        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return view;
    }

    private TextView button(String value, Runnable action) {
        TextView view = text(value, 15, accent, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(14), dp(12), dp(14), dp(12));
        view.setBackground(rounded(cardColor, 16));
        view.setClickable(true);
        view.setFocusable(true);

        view.setOnClickListener(v -> action.run());

        return view;
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private void loadApps() {
        allApps.clear();

        PackageManager pm = getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> results = pm.queryIntentActivities(intent, 0);

        for (ResolveInfo info : results) {
            if (info.activityInfo == null) continue;

            String packageName = info.activityInfo.packageName;

            // استبعاد المشغّل نفسه من درج التطبيقات.
            if (packageName.equals(getPackageName())) continue;

            AppItem item = new AppItem();
            item.info = info;
            item.label = info.loadLabel(pm).toString();
            item.packageName = packageName;
            item.activityName = info.activityInfo.name;
            item.icon = info.loadIcon(pm);

            allApps.add(item);
        }

        Collections.sort(allApps, new Comparator<AppItem>() {
            @Override
            public int compare(AppItem a, AppItem b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        applySavedAppOrder();
    }

    private void applySavedAppOrder() {
        String saved = prefs.getString("app_order", "");
        if (saved.isEmpty()) return;
        String[] packages = saved.split("\\|");
        Map<String, Integer> order = new HashMap<>();
        for (int i = 0; i < packages.length; i++) order.put(packages[i], i);
        Collections.sort(allApps, (a, b) -> {
            int ai = order.containsKey(a.packageName) ? order.get(a.packageName) : Integer.MAX_VALUE;
            int bi = order.containsKey(b.packageName) ? order.get(b.packageName) : Integer.MAX_VALUE;
            if (ai != bi) return Integer.compare(ai, bi);
            return a.label.compareToIgnoreCase(b.label);
        });
    }

    private void saveAppOrder() {
        StringBuilder saved = new StringBuilder();
        for (AppItem app : allApps) {
            if (saved.length() > 0) saved.append('|');
            saved.append(app.packageName);
        }
        prefs.edit().putString("app_order", saved.toString()).apply();
    }

    private void moveAppTo(AppItem dragged, AppItem target) {
        int from = allApps.indexOf(dragged);
        int to = allApps.indexOf(target);
        if (from < 0 || to < 0 || from == to) return;
        AppItem moving = allApps.remove(from);
        allApps.add(to, moving);
        saveAppOrder();
        showHome();
        Toast.makeText(this, "تم حفظ ترتيب التطبيقات", Toast.LENGTH_SHORT).show();
    }

    private void createBase(String title) {
        FrameLayout root = new FrameLayout(this);
        rootLayout = root;
        root.setBackgroundColor(background);

        wallpaperView = new ImageView(this);
        wallpaperView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(wallpaperView, new FrameLayout.LayoutParams(-1, -1));

        page = vertical();
        page.setPadding(dp(18), dp(14), dp(18), dp(18));
        page.setBackgroundColor(Color.TRANSPARENT);
        applyPageBackground();

        if (title != null && !title.isEmpty()) {
            TextView heading = text(title, 22, accent, true);
            heading.setPadding(0, dp(8), 0, dp(16));
            page.addView(heading);
        }

        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        content = vertical();
        scrollView.addView(content);

        page.addView(scrollView, new LinearLayout.LayoutParams(
                -1, 0, 1
        ));

        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        // Narrow edge gesture target; configurable from launcher settings.
        View edgeGesture = new View(this);
        edgeGesture.setBackgroundColor(Color.TRANSPARENT);
        edgeGesture.setContentDescription("فتح لوحة النظام بالسحب من حافة الشاشة");
        FrameLayout.LayoutParams edgeParams = new FrameLayout.LayoutParams(
                dp(26), -1, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        root.addView(edgeGesture, edgeParams);
        edgeGesture.setOnTouchListener((view, event) -> {
            int side = prefs.getInt("panel_gesture_side", 0); // 0 right, 1 left, 2 off
            if (side == 2) return false;
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                gestureStartX = event.getX();
                gestureStartY = event.getY();
                return true;
            }
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                float dx = event.getX() - gestureStartX;
                float dy = event.getY() - gestureStartY;
                boolean inward = side == 0 ? dx < -dp(55) : dx > dp(55);
                if (inward && Math.abs(dx) > Math.abs(dy) * 1.2f) {
                    startActivity(new Intent(this, SystemPanelActivity.class));
                }
                return true;
            }
            return true;
        });

        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset = insets.getSystemWindowInsetTop();
            int bottomInset = insets.getSystemWindowInsetBottom();
            systemBottomInset = bottomInset;
            page.setPadding(dp(18), dp(14) + topInset,
                    dp(18), dp(18) + bottomInset);
            for (int i = 0; i < root.getChildCount(); i++) {
                View child = root.getChildAt(i);
                if ("homi_dock".equals(child.getTag())
                        && child.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams dockParams =
                            (FrameLayout.LayoutParams) child.getLayoutParams();
                    dockParams.bottomMargin = dp(12) + bottomInset;
                    child.setLayoutParams(dockParams);
                }
            }
            return insets;
        });
        setContentView(root);
    }

    private void showHome() {
        showingApps = false;
        currentQuery = "";
        createBase("");

        // iOS-inspired home screen: compact header, clock widget, four-column icons and a fixed glass dock.
        LinearLayout topBar = horizontal();
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("HOMI", 17, textColor(), true);
        brand.setLetterSpacing(0.16f);
        topBar.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView settings = text("⚙", 22, textColor(), true);
        settings.setGravity(Gravity.CENTER);
        settings.setBackground(rounded(cardColor, 24));
        settings.setPadding(dp(12), dp(7), dp(12), dp(7));
        settings.setOnClickListener(v -> showSettings());
        TextView foldersButton = text("▦", 21, textColor(), true);
        foldersButton.setGravity(Gravity.CENTER);
        foldersButton.setBackground(rounded(cardColor, 24));
        foldersButton.setPadding(dp(12), dp(7), dp(12), dp(7));
        foldersButton.setContentDescription("إنشاء مجلد تطبيقات");
        foldersButton.setOnClickListener(v -> showCreateFolderDialog());
        LinearLayout.LayoutParams foldersButtonParams =
                new LinearLayout.LayoutParams(-2, -2);
        foldersButtonParams.setMargins(0, 0, dp(8), 0);
        topBar.addView(foldersButton, foldersButtonParams);
        topBar.addView(settings);
        content.addView(topBar);

        addSpace(content, 16);

        LinearLayout widget = vertical();
        widget.setPadding(dp(18), dp(15), dp(18), dp(17));
        widget.setBackground(rounded(cardColor, 25));
        TextView date = text(new SimpleDateFormat("EEEE، d MMMM", new Locale("ar"))
                .format(new Date()), 14, secondaryTextColor(), true);
        dateView = date;
        date.setGravity(Gravity.CENTER);
        widget.addView(date);

        clockView = text("--:--", Math.min(clockSize + 8, 78), textColor(), true);
        clockView.setGravity(Gravity.CENTER);
        clockView.setPadding(0, dp(2), 0, dp(1));
        widget.addView(clockView);

        TextView subtitle = text("مساحتك. بطريقتك.", 12, accent, false);
        subtitle.setGravity(Gravity.CENTER);
        widget.addView(subtitle);
        content.addView(widget);

        // Clean home screen: use folders instead of listing every installed app.
        addSpace(content, 14);
        TextView library = text("⌕   فتح مكتبة التطبيقات", 15, textColor(), true);
        library.setGravity(Gravity.CENTER);
        library.setPadding(dp(16), dp(15), dp(16), dp(15));
        library.setBackground(rounded(cardColor, 20));
        library.setOnClickListener(v -> {
            currentQuery = "";
            showApps();
        });
        content.addView(library, new LinearLayout.LayoutParams(-1, -2));

        addSpace(content, 16);
        addFolderSection(content);
        addSpace(content, 12);

        // Keep the dock visible while the home screen content scrolls.
        scrollView.setClipToPadding(false);
        scrollView.setPadding(0, 0, 0, dp(112));
        addGlassDock();

        handler.removeCallbacks(clockRunnable);
        handler.post(clockRunnable);
    }

    private JSONArray readFolders() {
        try {
            return new JSONArray(prefs.getString("app_folders", "[]"));
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private void saveFolders(JSONArray folders) {
        prefs.edit().putString("app_folders", folders.toString()).apply();
    }

    private void addFolderSection(LinearLayout parent) {
        JSONArray folders = readFolders();
        if (folders.length() == 0) return;
        TextView heading = text("مجلداتي", 14, secondaryTextColor(), true);
        heading.setPadding(dp(4), dp(2), dp(4), dp(6));
        parent.addView(heading);
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < folders.length(); i++) {
            JSONObject folder = folders.optJSONObject(i);
            if (folder == null) continue;
            String name = folder.optString("name", "مجلد");
            JSONArray packages = folder.optJSONArray("apps");
            int count = packages == null ? 0 : packages.length();
            TextView tile = text("▣  " + name + "\n" + count + " تطبيقات",
                    12, textColor(), true);
            tile.setGravity(Gravity.CENTER);
            tile.setMaxLines(2);
            tile.setPadding(dp(12), dp(10), dp(12), dp(10));
            tile.setBackground(rounded(cardColor, 18));
            tile.setOnClickListener(v -> showFolder(folder));
            tile.setOnLongClickListener(v -> {
                showFolderOptions(folder);
                return true;
            });
            LinearLayout.LayoutParams tileParams =
                    new LinearLayout.LayoutParams(0, -2, 1);
            tileParams.setMargins(dp(3), 0, dp(3), 0);
            row.addView(tile, tileParams);
        }
        parent.addView(row);
    }

    private void showCreateFolderDialog() {
        if (allApps.isEmpty()) {
            Toast.makeText(this, "لا توجد تطبيقات لإنشاء مجلد", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[allApps.size()];
        boolean[] checked = new boolean[allApps.size()];
        for (int i = 0; i < allApps.size(); i++) labels[i] = allApps.get(i).label;
        new AlertDialog.Builder(this)
                .setTitle("اختر تطبيقات المجلد")
                .setMultiChoiceItems(labels, checked,
                        (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("التالي", (dialog, which) -> {
                    JSONArray selected = new JSONArray();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) selected.put(allApps.get(i).packageName);
                    }
                    if (selected.length() == 0) {
                        Toast.makeText(this, "اختر تطبيقاً واحداً على الأقل", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    EditText nameInput = new EditText(this);
                    nameInput.setSingleLine(true);
                    nameInput.setHint("اسم المجلد");
                    nameInput.setTextColor(textColor());
                    nameInput.setHintTextColor(secondaryTextColor());
                    nameInput.setPadding(dp(14), dp(10), dp(14), dp(10));
                    new AlertDialog.Builder(this)
                            .setTitle("اسم المجلد")
                            .setView(nameInput)
                            .setNegativeButton("إلغاء", null)
                            .setPositiveButton("حفظ", (d, w) -> {
                                String name = nameInput.getText().toString().trim();
                                if (name.isEmpty()) name = "مجلد جديد";
                                JSONArray folders = readFolders();
                                JSONObject folder = new JSONObject();
                                try {
                                    folder.put("id", "folder_" + System.currentTimeMillis());
                                    folder.put("name", name);
                                    folder.put("apps", selected);
                                    folders.put(folder);
                                    saveFolders(folders);
                                    showHome();
                                    Toast.makeText(this, "تم إنشاء المجلد", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Toast.makeText(this, "تعذر حفظ المجلد", Toast.LENGTH_SHORT).show();
                                }
                            }).show();
                }).show();
    }

    private void showFolder(JSONObject folder) {
        JSONArray packages = folder.optJSONArray("apps");
        List<AppItem> folderApps = new ArrayList<>();
        if (packages != null) {
            for (int i = 0; i < packages.length(); i++) {
                String packageName = packages.optString(i);
                for (AppItem app : allApps) {
                    if (app.packageName.equals(packageName)) {
                        folderApps.add(app);
                        break;
                    }
                }
            }
        }

        LinearLayout panel = vertical();
        panel.setPadding(dp(4), dp(4), dp(4), dp(2));

        final AlertDialog[] folderDialog = new AlertDialog[1];
        ScrollView folderScroll = new ScrollView(this);
        folderScroll.setFillViewport(true);
        folderScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        GridLayout folderGrid = new GridLayout(this);
        folderGrid.setColumnCount(3);
        folderGrid.setUseDefaultMargins(false);

        if (folderApps.isEmpty()) {
            TextView empty = text("هذا المجلد فارغ حالياً", 14, secondaryTextColor(), false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(12), dp(30), dp(12), dp(30));
            folderGrid.addView(empty, new GridLayout.LayoutParams());
        } else {
            for (AppItem app : folderApps) {
                LinearLayout tile = vertical();
                tile.setGravity(Gravity.CENTER);
                tile.setPadding(dp(4), dp(10), dp(4), dp(10));
                tile.setBackground(rounded(cardColor, 18));
                tile.setClickable(true);
                tile.setFocusable(true);

                ImageView icon = new ImageView(this);
                icon.setImageDrawable(app.icon);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                LinearLayout.LayoutParams iconParams =
                        new LinearLayout.LayoutParams(dp(46), dp(46));
                iconParams.gravity = Gravity.CENTER;
                tile.addView(icon, iconParams);

                TextView label = text(app.label, 11, textColor(), false);
                label.setGravity(Gravity.CENTER);
                label.setMaxLines(2);
                label.setPadding(dp(2), dp(6), dp(2), 0);
                tile.addView(label, new LinearLayout.LayoutParams(-1, dp(38)));

                tile.setContentDescription("فتح " + app.label);
                tile.setOnClickListener(v -> {
                    if (folderDialog[0] != null) folderDialog[0].dismiss();
                    launchApp(app);
                });

                GridLayout.LayoutParams tileParams = new GridLayout.LayoutParams();
                tileParams.width = 0;
                tileParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                tileParams.setMargins(dp(3), dp(3), dp(3), dp(3));
                folderGrid.addView(tile, tileParams);
            }
        }

        folderScroll.addView(folderGrid);
        panel.addView(folderScroll, new LinearLayout.LayoutParams(-1, dp(300)));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(folder.optString("name", "مجلد"))
                .setView(panel)
                .setNeutralButton("إدارة المجلد", null)
                .setNegativeButton("إغلاق", null)
                .create();
        folderDialog[0] = dialog;
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener(v -> {
                    dialog.dismiss();
                    showFolderOptions(folder);
                }));
        dialog.show();
    }

    private void showFolderOptions(JSONObject folder) {
        String[] options = {"إعادة تسمية", "تعديل التطبيقات", "حذف المجلد"};
        new AlertDialog.Builder(this)
                .setTitle(folder.optString("name", "مجلد"))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        EditText input = new EditText(this);
                        input.setText(folder.optString("name", ""));
                        input.setSingleLine(true);
                        new AlertDialog.Builder(this).setTitle("اسم المجلد")
                                .setView(input).setNegativeButton("إلغاء", null)
                                .setPositiveButton("حفظ", (d, w) -> {
                                    String name = input.getText().toString().trim();
                                    if (name.isEmpty()) name = "مجلد";
                                    try {
                                        folder.put("name", name);
                                        persistFolderUpdate(folder);
                                    } catch (Exception e) {
                                        Toast.makeText(this, "تعذر إعادة تسمية المجلد", Toast.LENGTH_SHORT).show();
                                    }
                                }).show();
                    } else if (which == 1) {
                        showEditFolderApps(folder);
                    } else {
                        new AlertDialog.Builder(this).setTitle("حذف المجلد؟")
                                .setMessage("سيُحذف المجلد فقط، ولن تُحذف التطبيقات من الهاتف.")
                                .setNegativeButton("إلغاء", null)
                                .setPositiveButton("حذف", (d, w) -> {
                                    JSONArray old = readFolders();
                                    JSONArray updated = new JSONArray();
                                    String id = folder.optString("id", "");
                                    for (int i = 0; i < old.length(); i++) {
                                        JSONObject item = old.optJSONObject(i);
                                        if (item != null && !item.optString("id", "").equals(id)) updated.put(item);
                                    }
                                    saveFolders(updated);
                                    showHome();
                                }).show();
                    }
                }).show();
    }

    private void showEditFolderApps(JSONObject folder) {
        String[] labels = new String[allApps.size()];
        boolean[] checked = new boolean[allApps.size()];
        Set<String> existing = new HashSet<>();
        JSONArray apps = folder.optJSONArray("apps");
        if (apps != null) for (int i = 0; i < apps.length(); i++) existing.add(apps.optString(i));
        for (int i = 0; i < allApps.size(); i++) {
            labels[i] = allApps.get(i).label;
            checked[i] = existing.contains(allApps.get(i).packageName);
        }
        new AlertDialog.Builder(this).setTitle("تعديل تطبيقات المجلد")
                .setMultiChoiceItems(labels, checked,
                        (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("حفظ", (dialog, which) -> {
                    JSONArray updated = new JSONArray();
                    for (int i = 0; i < checked.length; i++) if (checked[i]) updated.put(allApps.get(i).packageName);
                    try {
                        folder.put("apps", updated);
                        persistFolderUpdate(folder);
                    } catch (Exception e) {
                        Toast.makeText(this, "تعذر تحديث المجلد", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private void persistFolderUpdate(JSONObject changed) {
        JSONArray old = readFolders();
        JSONArray updated = new JSONArray();
        String id = changed.optString("id", "");
        for (int i = 0; i < old.length(); i++) {
            JSONObject item = old.optJSONObject(i);
            if (item != null) updated.put(item.optString("id", "").equals(id) ? changed : item);
        }
        saveFolders(updated);
        showHome();
    }

    private void addGlassDock() {
        if (rootLayout == null || allApps.isEmpty()) return;
        LinearLayout dock = horizontal();
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(8), dp(9), dp(8), dp(9));
        dock.setBackground(rounded(cardColor, 30));
        dock.setElevation(dp(12));

        List<AppItem> dockApps = chooseDockApps();
        for (AppItem app : dockApps) {
            LinearLayout slot = vertical();
            slot.setGravity(Gravity.CENTER);
            slot.setPadding(dp(3), dp(1), dp(3), dp(1));
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(app.icon);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            slot.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));
            slot.setContentDescription(app.label);
            slot.setClickable(true);
            slot.setFocusable(true);
            slot.setOnClickListener(v -> launchApp(app));
            dock.addView(slot, new LinearLayout.LayoutParams(0, -2, 1));
        }

        FrameLayout.LayoutParams dockParams = new FrameLayout.LayoutParams(
                -1, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dockParams.setMargins(dp(14), 0, dp(14), dp(12) + systemBottomInset);
        dock.setTag("homi_dock");
        rootLayout.addView(dock, dockParams);
    }

    private List<AppItem> chooseDockApps() {
        List<AppItem> selected = new ArrayList<>();
        Set<String> favoritePackages = getFavoritePackages();
        String storedFavorites = prefs.getString(FAVORITES_KEY, "");
        if (!storedFavorites.isEmpty()) {
            for (String packageName : storedFavorites.split("\\|")) {
                if (packageName.trim().isEmpty()) continue;
                for (AppItem app : allApps) {
                    if (app.packageName.equals(packageName.trim()) && !selected.contains(app)) {
                        selected.add(app);
                        break;
                    }
                }
                if (selected.size() >= 4) break;
            }
        }
        String[][] priorities = {
                {"phone", "الهاتف", "dialer", "اتصال"},
                {"message", "الرسائل", "sms", "messaging"},
                {"camera", "الكاميرا", "كاميرا"},
                {"chrome", "browser", "متصفح", "المتصفح"}
        };
        for (String[] terms : priorities) {
            if (selected.size() >= 4) break;
            for (AppItem app : allApps) {
                if (selected.contains(app)) continue;
                String label = app.label.toLowerCase(Locale.ROOT);
                String pkg = app.packageName.toLowerCase(Locale.ROOT);
                boolean match = false;
                for (String term : terms) {
                    if (label.contains(term) || pkg.contains(term)) {
                        match = true;
                        break;
                    }
                }
                if (match) {
                    selected.add(app);
                    break;
                }
            }
        }
        for (AppItem app : allApps) {
            if (selected.size() >= 4) break;
            if (!selected.contains(app)) selected.add(app);
        }
        return selected;
    }

    private Set<String> getFavoritePackages() {
        Set<String> favorites = new java.util.LinkedHashSet<>();
        String stored = prefs.getString(FAVORITES_KEY, "");
        if (!stored.isEmpty()) {
            for (String value : stored.split("\\|")) {
                if (!value.trim().isEmpty()) favorites.add(value.trim());
            }
        }
        return favorites;
    }

    private void toggleFavorite(AppItem app) {
        Set<String> favorites = new java.util.LinkedHashSet<>(getFavoritePackages());
        boolean removed;
        if (favorites.contains(app.packageName)) {
            favorites.remove(app.packageName);
            removed = true;
        } else {
            favorites.add(app.packageName);
            removed = false;
        }
        StringBuilder stored = new StringBuilder();
        for (String favorite : favorites) {
            if (stored.length() > 0) stored.append('|');
            stored.append(favorite);
        }
        prefs.edit().putString(FAVORITES_KEY, stored.toString()).apply();
        Toast.makeText(this,
                removed ? "أزيل من شريط التطبيقات" : "أضيف إلى شريط التطبيقات",
                Toast.LENGTH_SHORT).show();
        if (showingApps) showApps(); else showHome();
    }

    private void showApps() {
        showingApps = true;
        handler.removeCallbacks(clockRunnable);
        createBase("جميع التطبيقات");

        LinearLayout searchBox = vertical();
        searchBox.setPadding(0, 0, 0, dp(12));

        appSearch = new EditText(this);
        appSearch.setSingleLine(true);
        appSearch.setTextSize(15);
        appSearch.setTextColor(textColor());
        appSearch.setHintTextColor(secondaryTextColor());
        appSearch.setHint("ابحث باسم التطبيق...");
        appSearch.setPadding(dp(14), dp(12), dp(14), dp(12));
        appSearch.setBackground(rounded(cardColor, 14));
        appSearch.setImeOptions(EditorInfo.IME_ACTION_DONE);

        searchBox.addView(appSearch);
        content.addView(searchBox);

        GridLayout results = new GridLayout(this);
        results.setColumnCount(Math.max(3, Math.min(6, prefs.getInt("drawer_columns", 5))));
        results.setUseDefaultMargins(false);
        content.addView(results, new LinearLayout.LayoutParams(-1, -2));

        Runnable updateResults = () -> {
            results.removeAllViews();

            String query = appSearch.getText().toString()
                    .trim().toLowerCase(Locale.ROOT);

            int count = 0;

            for (AppItem app : allApps) {
                if (!app.label.toLowerCase(Locale.ROOT).contains(query)
                        && !app.packageName.toLowerCase(Locale.ROOT).contains(query)) {
                    continue;
                }

                GridLayout.LayoutParams tileParams = new GridLayout.LayoutParams();
                tileParams.width = 0;
                tileParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                tileParams.setMargins(dp(1), dp(3), dp(1), dp(3));
                results.addView(createAppTile(app, false), tileParams);
                count++;
            }

            if (count == 0) {
                TextView empty = text(
                        "لا توجد تطبيقات مطابقة",
                        15,
                        secondaryTextColor(),
                        false
                );
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(0, dp(28), 0, dp(28));
                GridLayout.LayoutParams emptyParams = new GridLayout.LayoutParams();
                emptyParams.width = -1;
                emptyParams.columnSpec = GridLayout.spec(0,
                        Math.max(3, Math.min(6, prefs.getInt("drawer_columns", 5))));
                results.addView(empty, emptyParams);
            }
        };

        appSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                currentQuery = s.toString();
                updateResults.run();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        appSearch.setText(currentQuery);
        updateResults.run();

        addSpace(content, 16);

        content.addView(button("←   العودة إلى الرئيسية", this::showHome));
    }

    private View createAppTile(AppItem app, boolean compact) {
        LinearLayout tile = vertical();
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(3), dp(8), dp(3), dp(8));
        tile.setClickable(true);
        tile.setFocusable(true);

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(app.icon);

        int iconSize = compact
                ? Math.max(36, Math.min(68, prefs.getInt("home_icon_size", 52)))
                : Math.max(36, Math.min(68, prefs.getInt("drawer_icon_size", 56)));
        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(iconSize), dp(iconSize));
        iconParams.gravity = Gravity.CENTER;
        tile.addView(icon, iconParams);

        int labelColor = wallpaperUri != null ? Color.WHITE : textColor();
        TextView label = text(app.label, 11, labelColor, false);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        if (wallpaperUri != null) {
            label.setShadowLayer(dp(2), 0, dp(1), Color.argb(170, 0, 0, 0));
        }
        label.setPadding(0, dp(6), 0, 0);
        if (compact && !prefs.getBoolean("home_labels", true)) {
            label.setVisibility(View.GONE);
        }

        tile.addView(label, new LinearLayout.LayoutParams(
                -1, compact && !prefs.getBoolean("home_labels", true) ? 0 : dp(36)
        ));

        tile.setOnClickListener(v -> launchApp(app));
        if (compact) {
            tile.setOnLongClickListener(v -> {
                ClipData dragData = ClipData.newPlainText("package", app.packageName);
                View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
                if (android.os.Build.VERSION.SDK_INT >= 24) {
                    v.startDragAndDrop(dragData, shadow, app, 0);
                } else {
                    v.startDrag(dragData, shadow, app, 0);
                }
                return true;
            });
            tile.setOnDragListener((target, event) -> {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                    case DragEvent.ACTION_DRAG_ENTERED:
                    case DragEvent.ACTION_DRAG_EXITED:
                    case DragEvent.ACTION_DRAG_LOCATION:
                        return true;
                    case DragEvent.ACTION_DROP:
                        Object local = event.getLocalState();
                        if (local instanceof AppItem) moveAppTo((AppItem) local, app);
                        return true;
                    case DragEvent.ACTION_DRAG_ENDED:
                        target.setAlpha(1f);
                        return true;
                    default:
                        return false;
                }
            });
            tile.setContentDescription(app.label + "؛ اضغط مطولاً واسحب لإعادة الترتيب");
        } else {
            tile.setOnLongClickListener(v -> {
                toggleFavorite(app);
                return true;
            });
            tile.setContentDescription(app.label + "؛ اضغط مطولاً لإضافته إلى الشريط السفلي أو إزالته");
        }

        return tile;
    }

    private View createAppRow(AppItem app) {
        LinearLayout row = horizontal();
        row.setPadding(dp(12), dp(9), dp(12), dp(9));
        row.setBackground(rounded(cardColor, 16));

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(app.icon);

        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(44), dp(44));
        iconParams.setMargins(0, 0, dp(12), 0);
        row.addView(icon, iconParams);

        LinearLayout labels = vertical();

        TextView name = text(app.label, 15, textColor(), true);
        labels.addView(name);

        TextView packageLabel = text(
                app.packageName,
                10,
                secondaryTextColor(),
                false
        );
        labels.addView(packageLabel);

        row.addView(labels, new LinearLayout.LayoutParams(
                0, -2, 1
        ));

        TextView open = text("فتح", 13, accent, true);
        open.setGravity(Gravity.CENTER);
        row.addView(open);

        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> launchApp(app));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(8);
        row.setLayoutParams(params);

        return row;
    }

    private void launchApp(AppItem app) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setClassName(app.packageName, app.activityName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "تعذر فتح التطبيق",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void showSettings() {
        String[] options = {
                "تغيير اللون الرئيسي",
                "تغيير حجم الساعة",
                "اختيار صورة من الهاتف",
                "تبديل الوضع الفاتح/الداكن",
                "إزالة صورة الخلفية",
                "طريقة تثبيت التطبيقات في الشريط",
                "أعمدة الشاشة الرئيسية",
                "حجم أيقونات الرئيسية",
                "إظهار أسماء التطبيقات",
                "أعمدة مكتبة التطبيقات",
                "حجم أيقونات المكتبة",
                "إعادة تحميل التطبيقات",
                "إعادة ضبط تخصيصات المشغل",
                "لوحة النظام والإشعارات",
                "جهة السحب لفتح لوحة النظام"
        };

        new AlertDialog.Builder(this)
                .setTitle("تخصيص Homi OS")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        chooseAccent();
                    } else if (which == 1) {
                        chooseClockSize();
                    } else if (which == 2) {
                        chooseWallpaper();
                    } else if (which == 3) {
                        toggleBackground();
                    } else if (which == 4) {
                        if (wallpaperUri == null) {
                            Toast.makeText(this, "لا توجد صورة خلفية مخصصة", Toast.LENGTH_SHORT).show();
                        } else {
                            prefs.edit().remove("wallpaper_uri").remove("custom_accent").apply();
                            loadSettings();
                            showHome();
                            Toast.makeText(this, "تمت إزالة الخلفية المخصصة", Toast.LENGTH_SHORT).show();
                        }
                    } else if (which == 5) {
                        new AlertDialog.Builder(this)
                                .setTitle("تخصيص الشريط السفلي")
                                .setMessage("اضغط مطولاً على أي تطبيق في الرئيسية أو مكتبة التطبيقات لإضافته إلى الشريط السفلي أو إزالته. تُحفظ اختياراتك تلقائياً.")
                                .setPositiveButton("حسناً", null)
                                .show();
                    } else if (which == 6) {
                        chooseGridColumns(true);
                    } else if (which == 7) {
                        chooseIconSize(true);
                    } else if (which == 8) {
                        prefs.edit().putBoolean("home_labels",
                                !prefs.getBoolean("home_labels", true)).apply();
                        showHome();
                        Toast.makeText(this,
                                prefs.getBoolean("home_labels", true)
                                        ? "تم إظهار أسماء التطبيقات" : "تم إخفاء أسماء التطبيقات",
                                Toast.LENGTH_SHORT).show();
                    } else if (which == 9) {
                        chooseGridColumns(false);
                    } else if (which == 10) {
                        chooseIconSize(false);
                    } else if (which == 11) {
                        loadApps();
                        showHome();
                        Toast.makeText(
                                this,
                                "تم تحديث قائمة التطبيقات",
                                Toast.LENGTH_SHORT
                        ).show();
                    } else if (which == 13) {
                        startActivity(new Intent(this, SystemPanelActivity.class));
                    } else if (which == 14) {
                        String[] sides = {"من اليمين إلى اليسار", "من اليسار إلى اليمين", "تعطيل السحب الجانبي"};
                        int current = prefs.getInt("panel_gesture_side", 0);
                        new AlertDialog.Builder(this)
                                .setTitle("جهة السحب لفتح لوحة النظام")
                                .setSingleChoiceItems(sides, Math.max(0, Math.min(2, current)), (d, selected) -> {
                                    prefs.edit().putInt("panel_gesture_side", selected).apply();
                                    d.dismiss();
                                    Toast.makeText(this, selected == 0 ? "الفتح من الحافة اليمنى" :
                                            selected == 1 ? "الفتح من الحافة اليسرى" : "تم تعطيل السحب الجانبي",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("إغلاق", null)
                                .show();
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("إعادة ضبط التخصيصات؟")
                                .setMessage("سيتم إعادة الألوان والأحجام وترتيب التطبيقات والمفضلة إلى الوضع الافتراضي. ستبقى مجلداتك محفوظة.")
                                .setNegativeButton("إلغاء", null)
                                .setPositiveButton("إعادة الضبط", (d, w) -> {
                                    prefs.edit()
                                            .remove("accent")
                                            .remove("custom_accent")
                                            .remove("clock_size")
                                            .remove("wallpaper_uri")
                                            .remove("light_background")
                                            .remove("home_columns")
                                            .remove("drawer_columns")
                                            .remove("home_icon_size")
                                            .remove("drawer_icon_size")
                                            .remove("home_labels")
                                            .remove("app_order")
                                            .remove(FAVORITES_KEY)
                                            .apply();
                                    loadSettings();
                                    loadApps();
                                    showHome();
                                    Toast.makeText(this, "تمت إعادة التخصيصات مع الحفاظ على المجلدات", Toast.LENGTH_SHORT).show();
                                }).show();
                    }
                })
                .setNegativeButton("إغلاق", null)
                .show();
    }

    private void chooseGridColumns(boolean home) {
        String key = home ? "home_columns" : "drawer_columns";
        String[] choices = {"3 أعمدة", "4 أعمدة", "5 أعمدة", "6 أعمدة"};
        int saved = prefs.getInt(key, home ? 4 : 5);
        int selected = Math.max(0, Math.min(3, saved - 3));
        new AlertDialog.Builder(this)
                .setTitle(home ? "أعمدة الشاشة الرئيسية" : "أعمدة مكتبة التطبيقات")
                .setSingleChoiceItems(choices, selected, (dialog, which) -> {
                    prefs.edit().putInt(key, which + 3).apply();
                    dialog.dismiss();
                    if (home) showHome(); else showApps();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void chooseIconSize(boolean home) {
        String[] choices = {"صغيرة", "متوسطة", "كبيرة"};
        String key = home ? "home_icon_size" : "drawer_icon_size";
        int current = prefs.getInt(key, home ? 52 : 56);
        int selected = current < 48 ? 0 : current < 60 ? 1 : 2;
        new AlertDialog.Builder(this)
                .setTitle(home ? "حجم أيقونات الرئيسية" : "حجم أيقونات المكتبة")
                .setSingleChoiceItems(choices, selected, (dialog, which) -> {
                    int[] sizes = {44, 54, 64};
                    prefs.edit().putInt(key, sizes[which]).apply();
                    dialog.dismiss();
                    if (home) showHome(); else showApps();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void chooseAccent() {
        int selected = prefs.getInt("accent", 0);

        new AlertDialog.Builder(this)
                .setTitle("اختر اللون الرئيسي")
                .setSingleChoiceItems(
                        accentNames,
                        selected,
                        (dialog, which) -> {
                            prefs.edit().putInt("accent", which).remove("custom_accent").apply();
                            loadSettings();
                            dialog.dismiss();
                            showHome();
                        }
                )
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void chooseClockSize() {
        String[] sizes = {
                "صغيرة", "متوسطة", "كبيرة"
        };

        int selected;
        if (clockSize <= 44) {
            selected = 0;
        } else if (clockSize <= 60) {
            selected = 1;
        } else {
            selected = 2;
        }

        new AlertDialog.Builder(this)
                .setTitle("حجم الساعة")
                .setSingleChoiceItems(
                        sizes,
                        selected,
                        (dialog, which) -> {
                            int[] values = {40, 58, 72};
                            clockSize = values[which];

                            prefs.edit()
                                    .putInt("clock_size", clockSize)
                                    .apply();

                            dialog.dismiss();
                            showHome();
                        }
                )
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void chooseWallpaper() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_WALLPAPER);
        } catch (Exception e) {
            Toast.makeText(this, "تعذر فتح معرض الصور", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_WALLPAPER || resultCode != RESULT_OK
                || data == null || data.getData() == null) return;

        Uri selected = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(
                    selected, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
            // The provider may not support persistent URI permissions.
        }

        wallpaperUri = selected;
        prefs.edit().putString("wallpaper_uri", selected.toString()).apply();
        int sampledColor = sampleAccentColor(selected);
        if (sampledColor != Color.TRANSPARENT) {
            prefs.edit().putInt("custom_accent", sampledColor).apply();
        }
        loadSettings();
        if (showingApps) showApps(); else showHome();
        Toast.makeText(this, "تم تحديث الخلفية واللون الرئيسي", Toast.LENGTH_SHORT).show();
    }

    private void applyPageBackground() {
        if (wallpaperView != null && wallpaperUri != null) {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                Bitmap bitmap = BitmapFactory.decodeStream(
                        getContentResolver().openInputStream(wallpaperUri), null, options);
                if (bitmap != null) {
                    wallpaperView.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                    wallpaperView.setVisibility(View.VISIBLE);
                    if (android.os.Build.VERSION.SDK_INT >= 31) {
                        wallpaperView.setRenderEffect(
                                android.graphics.RenderEffect.createBlurEffect(
                                        dp(10), dp(10),
                                        android.graphics.Shader.TileMode.CLAMP));
                    }
                    return;
                }
            } catch (Exception ignored) {
                // Fall back to the selected solid background.
            }
        }
        if (wallpaperView != null) {
            wallpaperView.setImageDrawable(null);
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                wallpaperView.setRenderEffect(null);
            }
            wallpaperView.setVisibility(View.GONE);
        }
        if (page != null) page.setBackgroundColor(Color.TRANSPARENT);
    }

    private int sampleAccentColor(Uri uri) {
        Bitmap bitmap = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 8;
            bitmap = BitmapFactory.decodeStream(
                    getContentResolver().openInputStream(uri), null, options);
            if (bitmap == null) return Color.TRANSPARENT;

            int[] bins = new int[4096];
            long[] red = new long[4096];
            long[] green = new long[4096];
            long[] blue = new long[4096];
            int stepX = Math.max(1, bitmap.getWidth() / 48);
            int stepY = Math.max(1, bitmap.getHeight() / 48);

            for (int y = 0; y < bitmap.getHeight(); y += stepY) {
                for (int x = 0; x < bitmap.getWidth(); x += stepX) {
                    int pixel = bitmap.getPixel(x, y);
                    if (Color.alpha(pixel) < 100) continue;
                    int r = Color.red(pixel), g = Color.green(pixel), b = Color.blue(pixel);
                    int bucket = ((r >> 4) << 8) | ((g >> 4) << 4) | (b >> 4);
                    bins[bucket]++;
                    red[bucket] += r;
                    green[bucket] += g;
                    blue[bucket] += b;
                }
            }

            int best = -1;
            int bestScore = -1;
            for (int i = 0; i < bins.length; i++) {
                if (bins[i] == 0) continue;
                int avgR = (int)(red[i] / bins[i]);
                int avgG = (int)(green[i] / bins[i]);
                int avgB = (int)(blue[i] / bins[i]);
                int saturation = Math.max(avgR, Math.max(avgG, avgB))
                        - Math.min(avgR, Math.min(avgG, avgB));
                int score = bins[i] * (saturation + 24);
                if (score > bestScore) {
                    bestScore = score;
                    best = i;
                }
            }
            if (best < 0) return Color.TRANSPARENT;
            return Color.rgb((int)(red[best] / bins[best]),
                    (int)(green[best] / bins[best]),
                    (int)(blue[best] / bins[best]));
        } catch (Exception e) {
            return Color.TRANSPARENT;
        } finally {
            if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        }
    }

    private void toggleBackground() {
        boolean current = prefs.getBoolean(
                "light_background",
                false
        );

        prefs.edit()
                .putBoolean("light_background", !current)
                .apply();

        loadSettings();
        showHome();
    }

    private void addSpace(LinearLayout parent, int height) {
        View space = new View(this);
        parent.addView(space, new LinearLayout.LayoutParams(
                1, dp(height)
        ));
    }

    private int textColor() {
        return prefs != null
                && prefs.getBoolean("light_background", false)
                ? Color.rgb(25, 30, 45)
                : Color.rgb(240, 244, 255);
    }

    private int secondaryTextColor() {
        return prefs != null
                && prefs.getBoolean("light_background", false)
                ? Color.rgb(95, 105, 125)
                : Color.rgb(155, 168, 190);
    }

    @Override
    public void onBackPressed() {
        if (showingApps) {
            showHome();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh the visible grid only when installed apps actually changed.
        Set<String> before = new HashSet<>();
        for (AppItem app : allApps) before.add(app.packageName);
        loadApps();
        Set<String> after = new HashSet<>();
        for (AppItem app : allApps) after.add(app.packageName);

        if (!before.equals(after)) {
            if (showingApps) showApps(); else showHome();
        } else if (!showingApps) {
            handler.removeCallbacks(clockRunnable);
            handler.post(clockRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(clockRunnable);
    }

    private static class AppItem {
        ResolveInfo info;
        String label;
        String packageName;
        String activityName;
        android.graphics.drawable.Drawable icon;
    }
}
