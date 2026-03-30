package com.example.aplicativo_maya;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    private FooterNavigationView footer;
    private static final String[] TAGS = {"home", "exercicio", "perfil", "mural"};
    private int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        footer = findViewById(R.id.footer_navigation);

        if (savedInstanceState == null) {
            // 1ª transação: adiciona todos os fragments
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new HomepageFragment(), TAGS[0])
                    .add(R.id.fragment_container, new ExercicioFragment(), TAGS[1])
                    .add(R.id.fragment_container, new PerfilFragment(), TAGS[2])
                    .add(R.id.fragment_container, new MuralFragment(), TAGS[3])
                    .commitNow(); // commita antes de esconder

            // 2ª transação: esconde todos exceto o home
            getSupportFragmentManager().beginTransaction()
                    .hide(getSupportFragmentManager().findFragmentByTag(TAGS[1]))
                    .hide(getSupportFragmentManager().findFragmentByTag(TAGS[2]))
                    .hide(getSupportFragmentManager().findFragmentByTag(TAGS[3]))
                    .commitNow();
        }

        footer.setSelectedTab(0);
        footer.setOnTabSelectedListener(this::showFragment);

        ViewCompat.setOnApplyWindowInsetsListener(footer, (view, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), bottom);
            return insets;
        });
    }

    public void showFragment(int index) {
        if (index == currentIndex) {
            Fragment current = getSupportFragmentManager().findFragmentByTag(TAGS[index]);
            if (current != null && !current.isHidden()) return;
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_NONE);

        Fragment current = getSupportFragmentManager().findFragmentByTag(TAGS[currentIndex]);
        Fragment next    = getSupportFragmentManager().findFragmentByTag(TAGS[index]);

        if (current != null) ft.hide(current);
        if (next != null)    ft.show(next);

        ft.commitNow();
        currentIndex = index;
        footer.setSelectedTab(index);
    }
}