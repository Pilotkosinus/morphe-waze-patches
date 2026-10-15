package app.wazeytm.extension.waze.assistant;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import app.wazeytm.extension.waze.settings.MorpheSettings;

/**
 * Round map button in the style of the Waze button pack buttons
 * (56dp circle, 3dp shadow, Waze background color).
 * <p>
 * The view has a transparent margin for the shadow. It is offset with translation, so the
 * circle lines up with the other buttons of the column (portrait) or row (landscape).
 * If the feature is disabled the view measures 0x0, so changes apply without a restart.
 */
@SuppressLint("ViewConstructor")
public final class AssistantButtonView extends View implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int BUTTON_SIZE_DP = 56;
    private static final int SHADOW_MARGIN_DP = 6;
    private static final int ICON_SIZE_DP = 28;
    private static final float SHADOW_ELEVATION_DP = 3f;
    private static final float PRESSED_SCALE = 0.9f;

    private static final List<WeakReference<AssistantButtonView>> attachedViews = new ArrayList<>();

    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Drawable icon;
    private final float density;

    public AssistantButtonView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;

        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setColor(wazeColor(context, "background_high_contrast",
                wazeColor(context, "background_default", Color.WHITE)));
        float elevation = SHADOW_ELEVATION_DP * density;
        circlePaint.setShadowLayer(elevation, 0, elevation / 2, Color.argb(70, 0, 0, 0));
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        icon = loadIcon(context);

        setClickable(true);
        setFocusable(true);
        setContentDescription("Google Assistant");
        setOnClickListener(v -> AssistantLauncher.launch(v.getContext()));
    }

    private static boolean isFeatureEnabled() {
        return MorpheSettings.isEnabled(MorpheSettings.Feature.GOOGLE_ASSISTANT_BUTTON);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!isFeatureEnabled()) {
            setMeasuredDimension(0, 0);
            return;
        }
        int size = dp(BUTTON_SIZE_DP + 2 * SHADOW_MARGIN_DP);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        int margin = dp(SHADOW_MARGIN_DP);
        // Portrait: column aligned to the end. Landscape: row aligned to the top.
        baseTranslationX = landscape ? -margin : margin;
        baseTranslationY = -margin;
        updateTranslation();
    }

    private int baseTranslationX;
    private int baseTranslationY;

    /**
     * Moves the button with the same offset that Waze applies to its own map buttons.
     */
    private void updateTranslation() {
        setTranslationX(baseTranslationX + AssistantButtonHook.getPackOffsetX());
        setTranslationY(baseTranslationY + AssistantButtonHook.getPackOffsetY());
    }

    static void applyPackOffset() {
        synchronized (attachedViews) {
            Iterator<WeakReference<AssistantButtonView>> iterator = attachedViews.iterator();
            while (iterator.hasNext()) {
                AssistantButtonView view = iterator.next().get();
                if (view == null) iterator.remove();
                else view.updateTranslation();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isFeatureEnabled()) return;
        float center = getWidth() / 2f;
        float radius = dp(BUTTON_SIZE_DP) / 2f;
        canvas.drawCircle(center, getHeight() / 2f, radius, circlePaint);

        if (icon != null) {
            int half = dp(ICON_SIZE_DP) / 2;
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            icon.setBounds(cx - half, cy - half, cx + half, cy + half);
            icon.draw(canvas);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                animate().scaleX(PRESSED_SCALE).scaleY(PRESSED_SCALE).setDuration(100).start();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        synchronized (attachedViews) {
            attachedViews.add(new WeakReference<>(this));
        }
        MorpheSettings.registerListener(this);
        updateVisibility();
    }

    @Override
    protected void onDetachedFromWindow() {
        MorpheSettings.unregisterListener(this);
        synchronized (attachedViews) {
            Iterator<WeakReference<AssistantButtonView>> iterator = attachedViews.iterator();
            while (iterator.hasNext()) {
                AssistantButtonView view = iterator.next().get();
                if (view == null || view == this) iterator.remove();
            }
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        // The settings are changed in another activity. Update when the map is shown again.
        if (visibility == VISIBLE) updateVisibility();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (MorpheSettings.Feature.GOOGLE_ASSISTANT_BUTTON.key.equals(key)) updateVisibility();
    }

    private void updateVisibility() {
        boolean enabled = isFeatureEnabled();
        setEnabled(enabled);
        setImportantForAccessibility(enabled ? IMPORTANT_FOR_ACCESSIBILITY_YES : IMPORTANT_FOR_ACCESSIBILITY_NO);
        requestLayout();
        invalidate();
    }

    private static Drawable loadIcon(Context context) {
        try {
            int id = context.getResources().getIdentifier("google_assistant_24px", "drawable", context.getPackageName());
            if (id != 0) return context.getDrawable(id);
        } catch (Exception ignored) {
        }
        Drawable fallback = context.getDrawable(android.R.drawable.ic_btn_speak_now);
        if (fallback != null) fallback.setTint(wazeColor(context, "content_default", Color.DKGRAY));
        return fallback;
    }

    private static int wazeColor(Context context, String name, int fallback) {
        try {
            int id = context.getResources().getIdentifier(name, "color", context.getPackageName());
            if (id != 0) return context.getColor(id);
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private int dp(float value) {
        return Math.round(value * density);
    }
}
