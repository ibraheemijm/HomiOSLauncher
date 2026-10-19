
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.EditText;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;

public class MainActivity extends Activity {

    private LinearLayout root;
    private int white = Color.rgb(238, 243, 255);
    private int muted = Color.rgb(145, 160, 190);
    private int cyan = Color.rgb(0, 230, 220);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(8, 12, 28));
        getWindow().setNavigationBarColor(Color.rgb(8, 12, 28));
        getWindow().getDecorView().setSystemUiVisibility(0);
        showHome();
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setFontFeatureSettings("kern");
        return t;
    }

    private void showHome() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 28, 24, 18);
        root.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(8, 12, 30),
                      Color.rgb(17, 27, 55),
                      Color.rgb(5, 16, 31)}
        ));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);

        TextView brand = text("H O M I   O S", 14, cyan);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(brand, params(-1, 40));

        TextView status = text("●  SYSTEM ONLINE", 10,
            Color.rgb(100, 220, 180));
        root.addView(status, params(-1, 24));

        TextView clock = text(
            new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date()), 58, white);
        clock.setTypeface(Typeface.create("sans-serif-light",
            Typeface.NORMAL));
        root.addView(clock, params(-1, 85));

        TextView date = text(
            new SimpleDateFormat("EEEE, d MMMM",
                Locale.getDefault()).format(new Date()), 15, muted);
        root.addView(date, params(-1, 35));

        View line = new View(this);
        line.setBackgroundColor(Color.argb(90, 0, 230, 220));
        LinearLayout.LayoutParams lp = params(-1, 1);
        lp.setMargins(12, 18, 12, 22);
        root.addView(line, lp);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(white);
        search.setHintTextColor(muted);
        search.setHint("⌕   Search your apps");
        search.setTextSize(15);
        search.setPadding(18, 8, 18, 8);
        search.setBackground(shape(Color.rgb(30, 42, 68), 28));
        root.addView(search, params(-1, 54));

        TextView title = text("YOUR APPLICATIONS", 12, cyan);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tp = params(-1, 55);
        tp.setMargins(4, 12, 4, 0);
        root.addView(title, tp);

        addAppButton("▦", "ALL APPS", () -> showApps());
        addAppButton("◉", "PHONE", () -> launchApp("com.google.android.dialer"));
        addAppButton("✉", "MESSAGES", () -> launchApp("com.google.android.apps.messaging"));
        addAppButton("◎", "CAMERA", () -> launchApp("com.android.camera"));

        TextView footer = text("DESIGNED FOR YOUR DIGITAL WORLD", 9, muted);
        LinearLayout.LayoutParams fp = params(-1, 55);
        fp.setMargins(0, 16, 0, 0);
        root.addView(footer, fp);

        setContentView(scroll);

        search.setOnEditorActionListener((v, actionId, event) -> {
            String q = search.getText().toString().trim().toLowerCase();
            if (!q.isEmpty()) {
                showApps(q);
            }
            return true;
        });
    }

    private void addAppButton(String symbol, String label, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(16, 8, 16, 8);
        row.setBackground(shape(Color.argb(170, 28, 42, 67), 20));

        TextView icon = text(symbol, 24, cyan);
        row.addView(icon, params(48, 48));

        TextView name = text(label, 13, white);
        name.setGravity(Gravity.CENTER_VERTICAL);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(name, new LinearLayout.LayoutParams(0, -1, 1));

        TextView arrow = text("›", 26, muted);
        row.addView(arrow, params(28, 48));

        LinearLayout.LayoutParams lp = params(-1, 64);
        lp.setMargins(0, 6, 0, 6);
        root.addView(row, lp);
        row.setOnClickListener(v -> action.run());
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private void launchApp(String packageName) {
        try {
            Intent intent = getPackageManager()
                .getLaunchIntentForPackage(packageName);
            if (intent != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "App not found on this phone",
                    Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open app",
                Toast.LENGTH_SHORT).show();
        }
    }

    private void showApps() {
        showApps("");
    }

    private void showApps(String filter) {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(20, 25, 20, 20);
        page.setBackgroundColor(Color.rgb(8, 12, 30));

        TextView heading = text("ALL APPLICATIONS", 22, cyan);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        page.addView(heading, params(-1, 65));

        TextView back = text("‹   BACK TO HOME", 14, white);
        back.setBackground(shape(Color.rgb(30, 42, 68), 18));
        page.addView(back, params(-1, 48));
        back.setOnClickListener(v -> showHome());

        ScrollView listScroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        listScroll.addView(list);
        page.addView(listScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        PackageManager pm = getPackageManager();
        try {
            List<ApplicationInfo> apps = pm.getInstalledApplications(0);
            for (ApplicationInfo app : apps) {
                String name = pm.getApplicationLabel(app).toString();
                if (name.toLowerCase().contains(filter)) {
                    TextView item = text("◈    " + name + "    ›", 15, white);
                    item.setGravity(Gravity.CENTER_VERTICAL);
                    item.setPadding(18, 0, 18, 0);
                    item.setBackground(shape(Color.rgb(25, 37, 60), 16));
                    LinearLayout.LayoutParams ip = params(-1, 58);
                    ip.setMargins(0, 5, 0, 5);
                    list.addView(item, ip);
                    item.setOnClickListener(v -> {
                        Intent launch = pm.getLaunchIntentForPackage(
                            app.packageName);
                        if (launch != null) startActivity(launch);
                        else Toast.makeText(this, "Cannot launch this app",
                            Toast.LENGTH_SHORT).show();
                    });
                }
            }
        } catch (Exception ignored) { }

        setContentView(page);
    }

    @Override
    public void onBackPressed() {
        showHome();
    }
}
