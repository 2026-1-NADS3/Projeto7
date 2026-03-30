package com.example.aplicativo_maya; 

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class FooterNavigationView extends FrameLayout {

    private static final long ANIM_DURATION = 150;
    private static final float ICON_RISE_DP = 13f;
    private static final int TAB_COUNT = 4;

    private ImageView ballView;
    private final ImageView[] tabIcons = new ImageView[TAB_COUNT];
    private int selectedIndex = 0;
    private float iconRisePx;
    private OnTabSelectedListener listener;

    public interface OnTabSelectedListener {
        void onTabSelected(int index);
    }

    public FooterNavigationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        iconRisePx = ICON_RISE_DP * getResources().getDisplayMetrics().density;
        LayoutInflater.from(context).inflate(R.layout.activity_footer_navigation, this, true);

        ballView = findViewById(R.id.footer_ball);

        int[] containerIds = {R.id.tab_home, R.id.tab_atividades, R.id.tab_perfil, R.id.tab_mural};
        int[] iconIds = {R.id.icon_home, R.id.icon_atividades, R.id.icon_perfil, R.id.icon_mural};

        for (int i = 0; i < TAB_COUNT; i++) {
            final int index = i;
            tabIcons[i] = findViewById(iconIds[i]);
            findViewById(containerIds[i]).setOnClickListener(v -> {
                if (index != selectedIndex) {
                    animateToTab(index);
                    if (listener != null) listener.onTabSelected(index);
                }
            });
        }

        post(() -> {
            float tabWidth = (float) getWidth() / TAB_COUNT;
            float targetX = (tabWidth * selectedIndex + tabWidth / 2f) - (ballView.getWidth() / 2f);
            ballView.setTranslationX(targetX);
            tabIcons[selectedIndex].setTranslationY(-iconRisePx);
        });
    }

    /** Muda o tab selecionado visualmente sem disparar o listener (use no onResume da Activity). */
    public void setSelectedTab(int index) {
        if (index < 0 || index >= TAB_COUNT) return;
        final int newIndex = index;
        post(() -> {
            tabIcons[selectedIndex].setTranslationY(0);
            float tabWidth = (float) getWidth() / TAB_COUNT;
            float targetX = (tabWidth * newIndex + tabWidth / 2f) - (ballView.getWidth() / 2f);
            ballView.setTranslationX(targetX);
            tabIcons[newIndex].setTranslationY(-iconRisePx);
            selectedIndex = newIndex;
        });
    }

    private void animateToTab(int index) {
        float tabWidth = (float) getWidth() / TAB_COUNT;
        float targetX = (tabWidth * index + tabWidth / 2f) - (ballView.getWidth() / 2f);

        ballView.animate().translationX(targetX).setDuration(ANIM_DURATION)
                .setInterpolator(new DecelerateInterpolator()).start();

        tabIcons[selectedIndex].animate().translationY(0).setDuration(ANIM_DURATION).start();
        tabIcons[index].animate().translationY(-iconRisePx).setDuration(ANIM_DURATION).start();

        selectedIndex = index;
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.listener = listener;
    }
}