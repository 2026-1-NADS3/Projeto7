package com.example.aplicativo_maya;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;

public class ExercicioFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_exercicio, container, false);

        // Botões OPEN que abrem a lista de exercícios (tela de detalhe — Activity normal)
        int[] btnIds = {R.id.btnExe1, R.id.btnExe2, R.id.btnExe3};
        for (int id : btnIds) {
            Button btn = view.findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), ListaExercicioActivity.class);
                    startActivity(intent);
                });
            }
        }

        return view;
    }
}