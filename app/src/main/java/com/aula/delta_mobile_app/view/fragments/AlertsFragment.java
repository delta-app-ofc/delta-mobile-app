package com.aula.delta_mobile_app.view.fragments;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.*;
import androidx.lifecycle.ViewModelProvider;
import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.model.AlertModels.Alert;
import com.aula.delta_mobile_app.viewmodel.AlertsViewModel;
public final class AlertsFragment extends AuthFlowFragment {
    public AlertsFragment(){super(R.layout.fragment_alerts);}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved) {
        super.onViewCreated(view,saved);
        AlertsViewModel model=new ViewModelProvider(this).get(AlertsViewModel.class);
        selectedSeverity=model.severity();
        int[] ids={R.id.alert_all,R.id.alert_high,R.id.alert_medium,R.id.alert_low};
        String[] filters={null,"high","medium","low"};
        view.findViewById(R.id.alert_retry).setOnClickListener(v->model.refresh());
        view.findViewById(R.id.alert_previous).setOnClickListener(v->model.load(selectedSeverity,model.page()-1));
        view.findViewById(R.id.alert_next).setOnClickListener(v->model.load(selectedSeverity,model.page()+1));
        for(int i=0;i<ids.length;i++){final String filter=filters[i];view.findViewById(ids[i]).setOnClickListener(v->{selectedSeverity=filter;model.load(filter,0);});}
        model.state().observe(getViewLifecycleOwner(),state->{
            TextView status=view.findViewById(R.id.alert_status);
            LinearLayout list=view.findViewById(R.id.alert_list);list.removeAllViews();
            for(int id:ids)view.findViewById(id).setEnabled(!state.busy());
            view.findViewById(R.id.alert_previous).setEnabled(false);view.findViewById(R.id.alert_next).setEnabled(false);
            status.setText(state.status==ScreenState.Status.ERROR ? state.message : state.busy()?getString(R.string.alert_loading):"");
            if(state.status!=ScreenState.Status.SUCCESS)return;
            if(state.data.content.isEmpty())status.setText(R.string.alert_empty);
            for(Alert alert:state.data.content){
                TextView text=new TextView(requireContext());
                String level="high".equals(alert.severity)?getString(R.string.alert_high):"medium".equals(alert.severity)?getString(R.string.alert_medium):getString(R.string.alert_low);
                text.setText(level+" — "+(alert.title==null?getString(R.string.alert_title):alert.title)+"\n"+(alert.message==null?"":alert.message));list.addView(text);
                Button dismiss=new Button(requireContext());dismiss.setText(R.string.alert_dismiss);dismiss.setMinHeight((int)(48*getResources().getDisplayMetrics().density));
                dismiss.setOnClickListener(v->model.dismiss(alert.id));list.addView(dismiss);
            }
            view.findViewById(R.id.alert_previous).setEnabled(model.page()>0);
            view.findViewById(R.id.alert_next).setEnabled(model.page()+1<state.data.page.totalPages);
        });
        if(model.state().getValue().status==ScreenState.Status.IDLE)model.load(null,0);
    }
    private String selectedSeverity;
}
