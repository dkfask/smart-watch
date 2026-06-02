package com.smartwatch.monitor.ui.care;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.smartwatch.monitor.R;

public class CareHubFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_care_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.card_patients).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.patientListFragment));
        view.findViewById(R.id.card_devices).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.deviceListFragment));
    }
}
