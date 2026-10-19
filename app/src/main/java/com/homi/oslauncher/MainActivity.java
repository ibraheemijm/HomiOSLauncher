package com.homi.oslauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
import java.util.Locale;

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

    private SharedPreferences prefs;
    private boolean showingApps = false;
    private String currentQuery = "";

    private int accent;
    private int background;
    private int cardColor;
    private int clockSize;
    private Uri wallpaperUri;
    private static final int REQUEST_WALLPAPER = 4201;

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
            surfaceColor = Color.argb(190, Color.red(color), Color.green(color), Color.blue(color));
        }
        drawable.setColor(surfaceColor);
        drawable.setCornerRadius(dp(radius));
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
    }

    private void createBase(String title) {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(background);

        wallpaperView = new ImageView(this);
        wallpaperView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(wallpaperView, new FrameLayout.LayoutParams(-1, -1));

        page = vertical();
        page.setPadding(dp(18), dp(14), dp(18), dp(18));
        page.setBackgroundColor(Color.TRANSPARENT);
        applyPageBackground();

        TextView heading = text(title, 22, accent, true);
        heading.setPadding(0, dp(8), 0, dp(16));
        page.addView(heading);

        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        content = vertical();
        scrollView.addView(content);

        page.addView(scrollView, new LinearLayout.LayoutParams(
                -1, 0, 1
        ));

        root.addView(page, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
    }

    private void showHome() {
        showingApps = false;
        currentQuery = "";
        createBase("HOMI OS");

        LinearLayout hero = vertical();
        hero.setPadding(dp(18), dp(20), dp(18), dp(22));
        hero.setBackground(rounded(cardColor, 24));

        TextView brand = text("H O M I", 14, accent, true);
        brand.setGravity(Gravity.CENTER);
        hero.addView(brand);

        clockView = text("--:--", clockSize, textColor(), true);
        clockView.setGravity(Gravity.CENTER);
        clockView.setPadding(0, dp(8), 0, dp(4));
        hero.addView(clockView);

        dateView = text("", 15, secondaryTextColor(), false);
        dateView.setGravity(Gravity.CENTER);
        hero.addView(dateView);

        TextView status = text("واجهة ذكية • مساحة شخصية", 12, accent, false);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(12), 0, 0);
        hero.addView(status);

        content.addView(hero);

        addSpace(content, 18);

        TextView quickTitle = text("تطبيقاتك", 18, textColor(), true);
        content.addView(quickTitle);

        addSpace(content, 10);

        GridLayout quickGrid = new GridLayout(this);
        quickGrid.setColumnCount(4);
        quickGrid.setUseDefaultMargins(false);

        int quickCount = Math.min(4, allApps.size());

        for (int i = 0; i < quickCount; i++) {
            AppItem app = allApps.get(i);
            quickGrid.addView(createAppTile(app, true),
                    new GridLayout.LayoutParams());
        }

        content.addView(quickGrid);

        addSpace(content, 18);

        EditText homeSearch = new EditText(this);
        homeSearch.setSingleLine(true);
        homeSearch.setTextSize(15);
        homeSearch.setTextColor(textColor());
        homeSearch.setHintTextColor(secondaryTextColor());
        homeSearch.setHint("ابحث عن تطبيق...");
        homeSearch.setPadding(dp(16), dp(12), dp(16), dp(12));
        homeSearch.setBackground(rounded(cardColor, 16));
        homeSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

        homeSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                currentQuery = homeSearch.getText().toString();
                showApps();
                return true;
            }
            return false;
        });

        content.addView(homeSearch);

        addSpace(content, 12);

        content.addView(button("▦   جميع التطبيقات", () -> {
            currentQuery = "";
            showApps();
        }));

        addSpace(content, 10);

        content.addView(button("⚙   تخصيص الواجهة", this::showSettings));

        addSpace(content, 18);

        TextView footer = text(
                "HOMI OS  •  إصدار تجريبي",
                12,
                secondaryTextColor(),
                false
        );
        footer.setGravity(Gravity.CENTER);
        content.addView(footer);

        handler.removeCallbacks(clockRunnable);
        handler.post(clockRunnable);
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
        results.setColumnCount(5);
        results.setUseDefaultMargins(false);
        content.addView(results, new LinearLayout.LayoutParams(-1, -2));

        Runnable updateResults = () -> {
            results.removeAllViews();

            String query = appSearch.getText().toString()
                    .trim().toLowerCase(Locale.ROOT);

            int count = 0;

            for (AppItem app : allApps) {
                if (!app.label.toLowerCase(Locale.ROOT).contains(query)) {
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
                emptyParams.columnSpec = GridLayout.spec(0, 5);
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

        int iconSize = compact ? 48 : 56;
        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(iconSize), dp(iconSize));
        iconParams.gravity = Gravity.CENTER;
        tile.addView(icon, iconParams);

        TextView label = text(app.label, 11, textColor(), false);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        label.setPadding(0, dp(6), 0, 0);

        tile.addView(label, new LinearLayout.LayoutParams(
                -1, dp(36)
        ));

        tile.setOnClickListener(v -> launchApp(app));

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
                "إعادة تحميل التطبيقات"
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
                    } else {
                        loadApps();
                        showHome();
                        Toast.makeText(
                                this,
                                "تم تحديث قائمة التطبيقات",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .setNegativeButton("إغلاق", null)
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

        // تحديث قائمة التطبيقات عند الرجوع إلى المشغّل.
        loadApps();

        if (!showingApps) {
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
