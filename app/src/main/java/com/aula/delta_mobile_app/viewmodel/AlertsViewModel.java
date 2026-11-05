package com.aula.delta_mobile_app.viewmodel;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.*;
import com.aula.delta_mobile_app.model.AlertModels.*;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.*;
public final class AlertsViewModel extends AndroidViewModel {
    private final MutableLiveData<ScreenState<Page<Alert>>> state=new MutableLiveData<>(ScreenState.idle());
    private final AlertsRepository repository;
    private final Handler main=new Handler(Looper.getMainLooper());
    private String severity; private int page; private int generation; private boolean cleared;
    public AlertsViewModel(Application app) {super(app);repository=new AlertsRepository(AuthRepository.getInstance(app));}
    public LiveData<ScreenState<Page<Alert>>> state(){return state;}
    public int page(){return page;}
    public String severity(){return severity;}
    public void load(String filter,int number) {
        severity=filter;page=Math.max(0,number);int request=++generation;state.setValue(ScreenState.loading());
        repository.load(severity,page,value->main.post(()->{if(!cleared && request==generation)state.setValue(value);}));
    }
    public void dismiss(String id) {
        if(state.getValue().busy())return;
        int request=++generation;state.setValue(ScreenState.loading());
        repository.dismiss(id,result->main.post(()->{
            if(cleared || request!=generation)return;
            if(result.status==ScreenState.Status.ERROR)state.setValue(ScreenState.error(result.message));
            else load(severity,page);
        }));
    }
    public void refresh(){load(severity,page);}
    @Override protected void onCleared(){cleared=true;generation++;main.removeCallbacksAndMessages(null);}
}
