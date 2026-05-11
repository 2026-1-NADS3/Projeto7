package com.example.aplicativo_maya;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class HamburgerMenuView extends FrameLayout {

    private boolean isMenuOpen = false;
    private View menuContainer;
    private ImageView btnHamburger;
    private View overlayDim;
    private OnMenuItemClickListener listener;

    public interface OnMenuItemClickListener {
        void onItemClick(int viewId);
    }

    public HamburgerMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_hamburger_menu, this, true);
        menuContainer = findViewById(R.id.menu_container);
        btnHamburger  = findViewById(R.id.btn_hamburger);
        overlayDim    = findViewById(R.id.menu_overlay_dim);

        post(() -> {
            ViewGroup.LayoutParams lp = menuContainer.getLayoutParams();
            lp.width = (int) (getWidth() * 0.35f);
            menuContainer.setLayoutParams(lp);
        });

        Animation animIn  = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
        Animation animOut = AnimationUtils.loadAnimation(context, R.anim.slide_out_right);
        animIn.setInterpolator(new AccelerateDecelerateInterpolator());
        animOut.setInterpolator(new AccelerateDecelerateInterpolator());

        btnHamburger.setOnClickListener(v -> {
            Log.d("MAYA_DEBUG", "Botão clicado. Estado: " + isMenuOpen);
            if (isMenuOpen) closeMenu(animOut); else openMenu(animIn);
        });

        overlayDim.setOnClickListener(v -> closeMenu(animOut));

        setupItem(R.id.menu_item_perfil, animOut);
        setupItem(R.id.menu_item_acessibilidade, animOut);
        setupItem(R.id.menu_item_logout, animOut);
    }

    private void setupItem(int id, Animation animOut) {
        View item = findViewById(id);
        if (item != null) {
            item.setOnClickListener(v -> {
                applyPulseAnimation(v);
                v.postDelayed(() -> {
                    if (listener != null) listener.onItemClick(id);
                    closeMenu(animOut);
                }, 200);
            });
        }
    }

    private void applyPulseAnimation(View view) {
        ScaleAnimation pulse = new ScaleAnimation(1f, 0.9f, 1f, 0.9f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        pulse.setDuration(100);
        pulse.setRepeatCount(1);
        pulse.setRepeatMode(Animation.REVERSE);
        view.startAnimation(pulse);
    }

    private void openMenu(Animation animIn) {
        isMenuOpen = true;
        menuContainer.setVisibility(View.VISIBLE);
        menuContainer.startAnimation(animIn);
        overlayDim.setAlpha(0f);
        overlayDim.setVisibility(View.VISIBLE);
        overlayDim.animate()
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        btnHamburger.bringToFront();
    }

    private void closeMenu(Animation animOut) {
        isMenuOpen = false;
        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationEnd(Animation animation) {
                menuContainer.setVisibility(View.GONE);
            }
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
        });
        menuContainer.startAnimation(animOut);
        overlayDim.animate()
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> overlayDim.setVisibility(View.GONE))
                .start();
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        this.listener = listener;
    }
}