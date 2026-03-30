package com.example.aplicativo_maya;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;

public class HomepageFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_homepage, container, false);

        Button btnConsultar = view.findViewById(R.id.btnConsultar);
        if (btnConsultar != null) {
            btnConsultar.setOnClickListener(v -> {
                // Navega para a aba de exercícios via footer (sem criar nova Activity)
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showFragment(1);
                }
            });
        }

        return view;
    }
}