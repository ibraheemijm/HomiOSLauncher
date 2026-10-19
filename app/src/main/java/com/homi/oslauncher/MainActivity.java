package com.homi.oslauncher;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.os.Handler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;

public class MainActivity extends Activity {

    private int white = Color.rgb(238, 244, 255);
    private int muted = Color.rgb(139, 158, 190);
    private int cyan = Color.rgb(70, 226, 238);
    private int panel = Color.rgb(22, 34, 58);
    private Handler handler = new Handler();
    private TextView clock;
    private Runnable clockTask;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(true);
        return t;
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(
            w < 0 ? w : dp(w), h < 0 ? h : dp(h));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(7, 12, 26));
        getWindow().setNavigationBarColor(Color.rgb(7, 12, 26));
        showHome();
    }

    private void updateClock() {
        if (clock != null) {
            clock.setText(new SimpleDateFormat(
                "HH:mm", Locale.getDefault()).format(new Date()));
        }
    }

    private void stopClock() {
        if (clockTask != null) handler.removeCallbacks(clockTask);
    }

    private void startClock() {
        stopClock();
        clockTask = new Runnable() {
            @Override
            public void run() {
                updateClock();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(clockTask);
    }

    @Override
    protected void onDestroy() {
        stopClock();
        super.onDestroy();
    }

    private void showHome() {
        stopClock();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(10), dp(22), dp(12));
        root.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] {
                Color.rgb(7, 12, 28),
                Color.rgb(17, 31, 58),
                Color.rgb(5, 14, 29)
            }));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView brand = text("HOMI OS", 14, cyan);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(42), 1));

        TextView status = text("● ONLINE", 10, Color.rgb(100, 220, 180));
        header.addView(status, lp(-2, 42));
        root.addView(header, lp(-1, 42));

        // Flexible space keeps the clock away from the top.
        root.addView(new View(this),
            new LinearLayout.LayoutParams(1, 0, 1.0f));

        TextView greeting = text("YOUR WORLD. YOUR SPACE.", 11, muted);
        root.addView(greeting, lp(-1, 28));

        clock = text("--:--", 64, white);
        clock.setTypeface(Typeface.create(
            "sans-serif-light", Typeface.NORMAL));
        clock.setIncludeFontPadding(false);
        root.addView(clock, lp(-1, 82));
        startClock();

        TextView date = text(new SimpleDateFormat(
            "EEEE, d MMMM", Locale.getDefault()).format(new Date()),
            15, muted);
        root.addView(date, lp(-1, 32));

        View line = new View(this);
        line.setBackgroundColor(Color.argb(100, 70, 226, 238));
        LinearLayout.LayoutParams lineParams = lp(-1, 1);
        lineParams.setMargins(dp(12), dp(16), dp(12), dp(18));
        root.addView(line, lineParams);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(white);
        search.setHintTextColor(muted);
        search.setHint("⌕   Search applications");
        search.setTextSize(15);
        search.setPadding(dp(18), 0, dp(18), 0);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setBackground(shape(panel, 20));
        root.addView(search, lp(-1, 52));

        TextView title = text("QUICK ACCESS", 12, cyan);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams titleParams = lp(-1, 48);
        titleParams.topMargin = dp(8);
        root.addView(title, titleParams);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        grid.setUseDefaultMargins(false);

        addTile(grid, "▦", "ALL APPS",
            "Browse installed applications", () -> showApps(""));
        addTile(grid, "☎", "PHONE",
            "Make a call", () -> launchApp(
                "com.google.android.dialer", "com.android.phone"));
        addTile(grid, "✉", "MESSAGES",
            "Your conversations", () -> launchApp(
                "com.google.android.apps.messaging",
                "com.android.mms"));
        addTile(grid, "◎", "CAMERA",
            "Capture a moment", () -> launchApp(
                "com.android.camera", "com.google.android.GoogleCamera"));

        root.addView(grid, lp(-1, 188));

        root.addView(new View(this),
            new LinearLayout.LayoutParams(1, 0, 0.7f));

        TextView footer = text("H O M I   •   FUTURE, SIMPLIFIED",
            10, muted);
        root.addView(footer, lp(-1, 34));

        setContentView(root);

        search.setOnEditorActionListener((v, actionId, event) -> {
            String q = search.getText().toString().trim();
            showApps(q);
            return true;
        });
    }

    private void addTile(GridLayout grid, String symbol, String label,
                         String subtitle, Runnable action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(14), dp(10), dp(10), dp(10));
        tile.setBackground(shape(
            Color.argb(210, 24, 39, 65), 18));

        TextView icon = text(symbol, 25, cyan);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(shape(
            Color.rgb(32, 53, 82), 15));
        tile.addView(icon, lp(44, 44));

        TextView name = text(label, 12, white);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams nameParams = lp(-1, 25);
        nameParams.topMargin = dp(5);
        tile.addView(name, nameParams);

        TextView detail = text(subtitle, 10, muted);
        detail.setGravity(Gravity.CENTER_VERTICAL);
        tile.addView(detail, lp(-1, 20));

        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = dp(178);
        p.columnSpec = GridLayout.spec(
            GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(4), dp(4), dp(4), dp(4));
        grid.addView(tile, p);
        tile.setOnClickListener(v -> action.run());
    }

    private void launchApp(String... packageNames) {
        PackageManager pm = getPackageManager();
        for (String packageName : packageNames) {
            Intent intent = pm.getLaunchIntentForPackage(packageName);
            if (intent != null) {
                startActivity(intent);
                return;
            }
        }
        Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show();
    }

    private void showApps(String filter) {
        stopClock();

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(18), dp(20), dp(16));
        page.setBackgroundColor(Color.rgb(7, 12, 28));

        TextView heading = text("ALL APPLICATIONS", 22, cyan);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        page.addView(heading, lp(-1, 60));

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(white);
        search.setHintTextColor(muted);
        search.setHint("Search applications");
        search.setText(filter);
        search.setBackground(shape(panel, 18));
        search.setPadding(dp(14), 0, dp(14), 0);
        page.addView(search, lp(-1, 50));

        TextView back = text("‹   BACK TO HOME", 14, white);
        back.setBackground(shape(panel, 16));
        LinearLayout.LayoutParams bp = lp(-1, 46);
        bp.topMargin = dp(10);
        page.addView(back, bp);
        back.setOnClickListener(v -> showHome());

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        page.addView(scroll,
            new LinearLayout.LayoutParams(-1, 0, 1));

        Runnable populate = () -> {
            list.removeAllViews();
            String q = search.getText().toString()
                .trim().toLowerCase(Locale.ROOT);
            PackageManager pm = getPackageManager();

            try {
                List<ApplicationInfo> apps =
                    pm.getInstalledApplications(0);
                for (ApplicationInfo app : apps) {
                    String name =
                        pm.getApplicationLabel(app).toString();
                    if (!name.toLowerCase(Locale.ROOT).contains(q))
                        continue;

                    TextView item = text("◈    " + name + "    ›",
                        15, white);
                    item.setGravity(Gravity.CENTER_VERTICAL);
                    item.setPadding(dp(16), 0, dp(16), 0);
                    item.setBackground(shape(panel, 15));
                    LinearLayout.LayoutParams ip = lp(-1, 56);
                    ip.setMargins(0, dp(4), 0, dp(4));
                    list.addView(item, ip);

                    item.setOnClickListener(v -> {
                        Intent launch =
                            pm.getLaunchIntentForPackage(app.packageName);
                        if (launch != null) {
                            startActivity(launch);
                        } else {
                            Toast.makeText(this,
                                "Cannot launch this app",
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                Toast.makeText(this, "Unable to list apps",
                    Toast.LENGTH_SHORT).show();
            }
        };

        populate.run();
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s,
                int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s,
                int start, int before, int count) {
                populate.run();
            }
            @Override
            public void afterTextChanged(
                android.text.Editable s) { }
        });

        setContentView(page);
    }

    @Override
    public void onBackPressed() {
        showHome();
    }
}
