package app.wazeytm.extension.waze.settings;

import android.app.Activity;
import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.net.Uri;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static app.wazeytm.extension.waze.settings.MorpheSettings.text;

/**
 * Settings page of the Morphe patches. Opened from the Waze main settings.
 * The layout is created in code and uses the Waze color resources.
 */
@SuppressWarnings("deprecation")
public final class MorpheSettingsActivity extends Activity {
    private static final String AUTHOR = "Pilotkosinus";
    private static final String REPOSITORY_URL = "https://github.com/Pilotkosinus/morphe-waze-patches";

    private final Set<MorpheSettings.Feature> changedRestartFeatures = EnumSet.noneOf(MorpheSettings.Feature.class);
    private final Map<MorpheSettings.Feature, View> rows = new EnumMap<>(MorpheSettings.Feature.class);
    private final Map<MorpheSettings.Feature, Switch> toggles = new EnumMap<>(MorpheSettings.Feature.class);
    private View restartBanner;

    private int backgroundColor;
    private int surfaceColor;
    private int textColor;
    private int secondaryTextColor;
    private int primaryColor;
    private int dividerColor;
    private Typeface regularFont = Typeface.DEFAULT;
    private Typeface boldFont = Typeface.DEFAULT_BOLD;

    public static void start(Context context) {
        Intent intent = new Intent(context, MorpheSettingsActivity.class);
        if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Replaced by the patch with the (obfuscated) AppCompatDelegate class of Waze,
     * which holds the night mode that the user selected in the Waze settings.
     */
    private static String getNightModeClassName() {
        return "";
    }

    @Override
    protected void attachBaseContext(Context base) {
        // Waze applies its own day/night setting with AppCompatDelegate.setDefaultNightMode().
        // This activity is no AppCompat activity, so apply the mode here.
        Boolean night = getWazeNightMode();
        if (night != null) {
            Configuration configuration = new Configuration(base.getResources().getConfiguration());
            configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                    | (night ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
            base = base.createConfigurationContext(configuration);
        }
        super.attachBaseContext(base);
    }

    /**
     * @return True for night, false for day, null to follow the system.
     */
    private static Boolean getWazeNightMode() {
        try {
            String className = getNightModeClassName();
            if (className.isEmpty()) return null;
            // AppCompatDelegate.sDefaultNightMode is its only static non final int field.
            for (Field field : Class.forName(className).getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (field.getType() == int.class && Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)) {
                    field.setAccessible(true);
                    int mode = field.getInt(null);
                    if (mode == 2) return true;  // MODE_NIGHT_YES
                    if (mode == 1) return false; // MODE_NIGHT_NO
                    return null;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadColors();
        loadFonts();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(backgroundColor);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });

        root.addView(createHeader());

        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(8), 0, dp(16));
        scrollView.addView(content);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        content.addView(createSectionTitle(text("Patches", "Patches")));
        for (MorpheSettings.Feature feature : MorpheSettings.Feature.values()) {
            content.addView(createFeatureRow(feature));
            content.addView(createDivider());
        }

        updateOptionRows();

        TextView footer = createText(text(
                "Schalter wirken nur auf Patches, die beim Patchen eingebaut wurden. "
                        + "Nicht eingebaute Patches lassen sich hier nicht aktivieren.",
                "Switches only affect patches that were applied when patching. "
                        + "Patches that were not applied cannot be enabled here."), 13, secondaryTextColor);
        footer.setPadding(dp(16), dp(16), dp(16), 0);
        content.addView(footer);
        content.addView(createCredits());

        restartBanner = createRestartBanner();
        restartBanner.setVisibility(View.GONE);
        root.addView(restartBanner);

        setContentView(root);
        if (Build.VERSION.SDK_INT >= 23) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            boolean light = isLight(backgroundColor);
            int flags = getWindow().getDecorView().getSystemUiVisibility();
            if (light) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            else flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    private View createHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(surfaceColor);
        header.setPadding(dp(4), 0, dp(16), 0);
        header.setMinimumHeight(dp(56));

        TextView back = createText("←", 24, textColor);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription(text("Zurück", "Back"));
        back.setOnClickListener(v -> finish());
        back.setBackground(selectableBackground());
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = createText(text("Morphe Patch Einstellungen", "Morphe patch settings"), 20, textColor);
        title.setTypeface(boldFont);
        title.setPadding(dp(12), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return header;
    }

    private View createSectionTitle(String label) {
        TextView view = createText(label.toUpperCase(), 13, primaryColor);
        view.setTypeface(boldFont);
        view.setPadding(dp(16), dp(16), dp(16), dp(8));
        return view;
    }

    private View createFeatureRow(MorpheSettings.Feature feature) {
        final boolean included = feature.isIncluded();

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(surfaceColor);
        // Options of a patch are indented below it.
        int indent = feature.getParent() == null ? dp(16) : dp(40);
        row.setPadding(indent, dp(12), dp(16), dp(12));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView title = createText(feature.getTitle(), feature.getParent() == null ? 16 : 15, textColor);
        title.setTypeface(boldFont);
        texts.addView(title);

        TextView description = createText(feature.getDescription(), 14, secondaryTextColor);
        description.setPadding(0, dp(2), 0, 0);
        texts.addView(description);

        final String status;
        if (!included) {
            status = text("Nicht eingebaut – beim Patchen nicht ausgewählt", "Not included – not selected when patching");
        } else if (feature.requiresRestart) {
            status = text("Änderung wirkt nach Neustart von Waze", "Change applies after restarting Waze");
        } else {
            status = text("Änderung wirkt sofort", "Change applies immediately");
        }
        TextView statusView = createText(status, 12, included ? primaryColor : secondaryTextColor);
        statusView.setPadding(0, dp(4), 0, 0);
        texts.addView(statusView);

        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch toggle = new Switch(this);
        toggle.setChecked(included && MorpheSettings.isEnabled(feature));
        toggle.setEnabled(included);
        toggle.setContentDescription(feature.getTitle());
        int[][] states = {new int[]{android.R.attr.state_checked}, new int[]{}};
        toggle.setThumbTintList(new ColorStateList(states, new int[]{primaryColor, Color.rgb(0xF1, 0xF1, 0xF1)}));
        toggle.setTrackTintList(new ColorStateList(states, new int[]{primaryColor, secondaryTextColor}));
        toggle.setOnCheckedChangeListener((button, checked) -> {
            MorpheSettings.setEnabled(feature, checked);
            updateOptionRows();
            if (feature.requiresRestart) {
                if (!changedRestartFeatures.remove(feature)) changedRestartFeatures.add(feature);
                restartBanner.setVisibility(changedRestartFeatures.isEmpty() ? View.GONE : View.VISIBLE);
            }
        });
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        toggleParams.leftMargin = dp(12);
        row.addView(toggle, toggleParams);

        if (included) {
            row.setBackground(selectableBackground());
            row.setOnClickListener(v -> {
                if (toggle.isEnabled()) toggle.toggle();
            });
        } else {
            row.setAlpha(0.6f);
        }
        rows.put(feature, row);
        toggles.put(feature, toggle);
        return row;
    }

    /**
     * Options only work while their patch is enabled. Disable them otherwise.
     */
    private void updateOptionRows() {
        for (Map.Entry<MorpheSettings.Feature, Switch> entry : toggles.entrySet()) {
            MorpheSettings.Feature feature = entry.getKey();
            MorpheSettings.Feature parent = feature.getParent();
            if (parent == null || !feature.isIncluded()) continue;
            Switch parentToggle = toggles.get(parent);
            boolean active = parentToggle != null && parentToggle.isChecked();
            entry.getValue().setEnabled(active);
            View row = rows.get(feature);
            if (row != null) row.setAlpha(active ? 1f : 0.5f);
        }
    }

    /**
     * "Made by Pilotkosinus" and a GitHub icon that opens the repository.
     */
    private View createCredits() {
        LinearLayout credits = new LinearLayout(this);
        credits.setOrientation(LinearLayout.VERTICAL);
        credits.setGravity(Gravity.CENTER_HORIZONTAL);
        credits.setPadding(dp(16), dp(32), dp(16), dp(24));

        TextView author = createText(text("Erstellt von " + AUTHOR, "Made by " + AUTHOR), 14, secondaryTextColor);
        author.setGravity(Gravity.CENTER);
        credits.addView(author);

        ImageView github = new ImageView(this);
        int iconId = getResources().getIdentifier("morphe_github_icon", "drawable", getPackageName());
        if (iconId != 0) {
            github.setImageResource(iconId);
            github.setColorFilter(textColor);
        } else {
            github.setImageResource(android.R.drawable.ic_menu_info_details);
        }
        github.setPadding(dp(8), dp(8), dp(8), dp(8));
        github.setBackground(selectableBackground());
        github.setContentDescription(text("GitHub-Repository öffnen", "Open GitHub repository"));
        github.setOnClickListener(v -> openRepository());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(48), dp(48));
        params.topMargin = dp(4);
        credits.addView(github, params);
        return credits;
    }

    private void openRepository() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(REPOSITORY_URL)));
        } catch (ActivityNotFoundException ignored) {
        }
    }

    private View createRestartBanner() {
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setBackgroundColor(surfaceColor);
        banner.setPadding(dp(16), dp(12), dp(16), dp(12));

        TextView message = createText(text(
                "Neustart von Waze erforderlich, damit die Änderung wirkt.",
                "Restart Waze to apply the change."), 14, textColor);
        banner.addView(message, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button restart = new Button(this);
        restart.setText(text("Neu starten", "Restart"));
        restart.setAllCaps(false);
        restart.setTextColor(Color.WHITE);
        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setColor(primaryColor);
        buttonBackground.setCornerRadius(dp(20));
        restart.setBackground(buttonBackground);
        restart.setPadding(dp(16), 0, dp(16), 0);
        restart.setOnClickListener(v -> restartWaze());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
        params.leftMargin = dp(12);
        banner.addView(restart, params);
        return banner;
    }

    private void restartWaze() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent == null) return;
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        finishAffinity();
        startActivity(intent);
        Runtime.getRuntime().exit(0);
    }

    private View createDivider() {
        View divider = new View(this);
        divider.setBackgroundColor(dividerColor);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1) / 2)));
        return divider;
    }

    private TextView createText(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(color);
        view.setTypeface(regularFont);
        return view;
    }

    private android.graphics.drawable.Drawable selectableBackground() {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true);
        return getDrawable(value.resourceId);
    }

    private void loadColors() {
        surfaceColor = wazeColor("background_default", Color.WHITE);
        backgroundColor = wazeColor("surface_default", Color.rgb(0xF2, 0xF2, 0xF2));
        textColor = wazeColor("content_default", Color.rgb(0x20, 0x20, 0x20));
        secondaryTextColor = wazeColor("content_p2", Color.rgb(0x70, 0x70, 0x70));
        primaryColor = wazeColor("primary", Color.rgb(0x1A, 0x73, 0xE8));
        dividerColor = wazeColor("separator_default", Color.rgb(0xE0, 0xE0, 0xE0));
    }

    /**
     * Uses the font of Waze (Proxima Nova) if available.
     */
    private void loadFonts() {
        Typeface regular = wazeFont("proximanova");
        if (regular != null) regularFont = regular;
        Typeface bold = wazeFont("proximanova_bold");
        if (bold != null) boldFont = bold;
    }

    private Typeface wazeFont(String name) {
        try {
            int id = getResources().getIdentifier(name, "font", getPackageName());
            if (id != 0) return getResources().getFont(id);
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Resolves a Waze color resource by name, so the page follows the Waze palette and night mode.
     */
    private int wazeColor(String name, int fallback) {
        try {
            int id = getResources().getIdentifier(name, "color", getPackageName());
            if (id != 0) return getColor(id);
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static boolean isLight(int color) {
        return (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000 > 128;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
