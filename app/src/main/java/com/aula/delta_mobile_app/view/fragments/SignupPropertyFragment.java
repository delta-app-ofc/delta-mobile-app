package com.aula.delta_mobile_app.view.fragments;

import static com.aula.delta_mobile_app.viewmodel.SignupViewModel.isResidentialClassification;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aula.delta_mobile_app.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.radiobutton.MaterialRadioButton;

public class SignupPropertyFragment extends AuthFlowFragment {
    public SignupPropertyFragment() {
        super(R.layout.fragment_signup_property);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupHeader(view, 3, R.string.auth_step3_title, R.string.auth_step3_subtitle, R.string.auth_step_description);
        MaterialButton classification = view.findViewById(R.id.classification);
        ChipGroup types = view.findViewById(R.id.property_type);
        EditText name = view.findViewById(R.id.property_name), area = view.findViewById(R.id.built_area);
        restore(name, "propertyName");
        restore(area, "builtArea");
        String[] typeLabels = getResources().getStringArray(R.array.auth_property_types);
        android.content.Context context = SelectionHelper.context(requireContext());
        for (int i = 1; i < typeLabels.length; i++) {
            final int index = i;
            Chip chip = SelectionHelper.chip(context, typeLabels[i]);
            int[][] states = {new int[] {android.R.attr.state_checked}, new int[] {}};
            int blue = androidx.core.content.ContextCompat.getColor(context, R.color.auth_blue);
            int white = androidx.core.content.ContextCompat.getColor(context, R.color.auth_white);
            chip.setChipBackgroundColor(new android.content.res.ColorStateList(states, new int[] {white, blue}));
            chip.setTextColor(new android.content.res.ColorStateList(states, new int[] {blue, white}));
            chip.setCheckedIconTint(android.content.res.ColorStateList.valueOf(blue));
            chip.setChipStrokeWidth(SelectionHelper.dp(context, 1));
            chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(white));
            types.addView(chip);
            chip.setChecked(flow().getInt("propertyIndex", 0) == i);
            chip.setOnCheckedChangeListener((button, checked) -> {
                if (checked) {
                    flow().putInt("propertyIndex", index);
                    flow().putString("propertyType", typeLabels[index]);
                }
            });
        }
        if (!isResidentialClassification(flow().getInt("classificationIndex", 0))) {
            flow().remove("classificationIndex");
            flow().remove("classification");
        }
        updateClassification(classification);
        classification.setOnClickListener(v -> chooseClassification(classification));
        view.findViewById(R.id.step_continue).setOnClickListener(v -> {
            String error = registration().propertyError();
            if (error != null) {
                name.setError(error);
                return;
            }
            navigate(R.id.signupAddressFragment);
        });
    }

    private void updateClassification(MaterialButton field) {
        int selected = flow().getInt("classificationIndex", 0);
        String[] labels = getResources().getStringArray(R.array.auth_classifications);
        field.setText(isResidentialClassification(selected) && selected < labels.length ? labels[selected] : "Selecionar classificação residencial");
    }

    private void chooseClassification(MaterialButton field) {
        android.content.Context context = SelectionHelper.context(requireContext());
        LinearLayout content = SelectionHelper.content(context, "Classificação residencial", "Escolha a categoria usada na tarifa de água da sua residência.");
        android.widget.RadioGroup choices = new android.widget.RadioGroup(context);
        String[] labels = getResources().getStringArray(R.array.auth_classifications);
        for (int i = 1; i < labels.length; i++) {
            if (!isResidentialClassification(i)) continue;
            MaterialRadioButton option = new MaterialRadioButton(context);
            option.setId(View.generateViewId());
            option.setTag(i);
            option.setText(labels[i]);
            option.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.auth_navy));
            option.setButtonTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.auth_blue)));
            option.setMinHeight(SelectionHelper.dp(context, 56));
            option.setPadding(0, SelectionHelper.dp(context, 8), 0, SelectionHelper.dp(context, 8));
            choices.addView(option, new android.widget.RadioGroup.LayoutParams(-1, -2));
            option.setChecked(flow().getInt("classificationIndex", 0) == i);
        }
        content.addView(choices);
        MaterialButton confirm = new MaterialButton(context);
        confirm.setText(R.string.auth_confirm);
        confirm.setCornerRadius(SelectionHelper.dp(context, 18));
        content.addView(confirm, new LinearLayout.LayoutParams(-1, SelectionHelper.dp(context, 56)));
        confirm.setEnabled(choices.getCheckedRadioButtonId() != -1);
        choices.setOnCheckedChangeListener((group, checkedId) -> confirm.setEnabled(checkedId != -1));
        MaterialButton cancel = new MaterialButton(context, null, androidx.appcompat.R.attr.borderlessButtonStyle);
        cancel.setText(R.string.auth_cancel);
        content.addView(cancel, new LinearLayout.LayoutParams(-1, -2));
        BottomSheetDialog dialog = SelectionHelper.show(this, context, content);
        confirm.setOnClickListener(v -> {
            View selected = choices.findViewById(choices.getCheckedRadioButtonId());
            if (selected == null) return;
            int index = (Integer) selected.getTag();
            if (!isResidentialClassification(index)) return;
            flow().putInt("classificationIndex", index);
            flow().putString("classification", labels[index]);
            updateClassification(field);
            dialog.dismiss();
        });
        cancel.setOnClickListener(v -> dialog.dismiss());
    }
}
