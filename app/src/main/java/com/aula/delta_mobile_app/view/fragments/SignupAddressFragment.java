package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.*;

import androidx.annotation.*;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.ScreenState;

public class SignupAddressFragment extends AuthFlowFragment {
    public SignupAddressFragment() {
        super(R.layout.fragment_signup_address);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupHeader(view, 4, R.string.auth_step4_title, R.string.auth_step4_subtitle, R.string.auth_step4_description);
        EditText zip = view.findViewById(R.id.zip), state = view.findViewById(R.id.state), city = view.findViewById(R.id.city);
        restore(zip, "zip");
        TextView cepStatus = addStatus(view);
        TextView status = addStatus(view);
        TextView next = view.findViewById(R.id.step_continue);
        next.setText("Concluir cadastro");
        registration().postalCode.observe(getViewLifecycleOwner(), value -> {
            state.setText(flow().getString("state", ""));
            city.setText(flow().getString("city", ""));
            cepStatus.setText(value.status == ScreenState.Status.SUCCESS && !flow().containsKey("regionId") ? "Localidade sem região tarifária configurada. A"
                + " conclusão depende da configuração da" + " plataforma." : value.displayMessage());
        });
        registration().completion.observe(getViewLifecycleOwner(), value -> {
            showLoading(value.busy());
            showStatus(status, next, value);
            if (value.busy()) status.setText("");
            zip.setEnabled(!value.busy());
            if (value.status == ScreenState.Status.SUCCESS) {
                if (value.data.recoveredExisting)
                    Toast.makeText(requireContext(), "Seu cadastro já estava concluído. Os dados" + " existentes foram mantidos.", Toast.LENGTH_LONG).show();
                registration().consumeCompletion();
                openAuthenticated();
            }
        });
        cepStatus.setOnClickListener(v -> registration().getCep(text(zip).replaceAll("\\D", ""), true));
        zip.addTextChangedListener(new TextWatcher() {
            boolean formatting;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable value) {
                if (formatting) return;
                String digits = value.toString().replaceAll("\\D", "");
                if (digits.length() > 8) digits = digits.substring(0, 8);
                String formatted = digits.length() > 5 ? digits.substring(0, 5) + "-" + digits.substring(5) : digits;
                if (!formatted.contentEquals(value)) {
                    formatting = true;
                    zip.setText(formatted);
                    zip.setSelection(formatted.length());
                    formatting = false;
                }
                registration().getCep(digits, false);
            }
        });
        next.setOnClickListener(v -> registration().finish());
        registration().getCep(text(zip).replaceAll("\\D", ""), false);
    }
}
