package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.ApiModels.CreateUserHabitRequest;
import com.aula.delta_mobile_app.model.ApiModels.UserHabitResponse;
import com.aula.delta_mobile_app.model.ApiModels.UserHabitScheduleResponse;
import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.model.RoutineData;
import com.aula.delta_mobile_app.model.RoutineFormState;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.network.ApiErrors;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import retrofit2.Call;
import retrofit2.Response;

public final class RoutineRepository implements RoutineActions {
    public static final int UNCERTAIN = -7;

    @Override
    public void create(RoutineFormState form, Consumer<ScreenState<RoutineData.Habit>> callback) {
        if (form == null || form.mode != RoutineFormState.Mode.CREATE || !form.validate().valid()) {
            callback.accept(ScreenState.error("Confira os campos do formulário.", 400, 0));
            return;
        }
        access.resource(() -> {
            boolean started = false;
            try {
                Profile profile = access.currentProfile();
                if (!identified(profile)) return ScreenState.error("Não foi possível identificar sua conta.");
                CreateUserHabitRequest request = new CreateUserHabitRequest();
                request.habitId = form.habitId;
                request.frequency = Integer.valueOf(form.frequency.trim());
                request.daysOfWeek = new ArrayList<>();
                request.schedule = new ArrayList<>();
                for (DayOfWeek day : DayOfWeek.values()) {
                    RoutineFormState.DayDraft draft = form.days.get(day);
                    if (!draft.selected) continue;
                    request.daysOfWeek.add(day.getValue());
                    UserHabitScheduleResponse item = new UserHabitScheduleResponse();
                    item.dayOfWeek = day.getValue();
                    item.startTimes = new ArrayList<>(draft.times.subList(0, draft.activeCount()));
                    request.schedule.add(item);
                }
                Call<UserHabitResponse> call = access.api().createUserHabit(profile.id, request);
                started = true;
                Response<UserHabitResponse> response = call.execute();
                if (response.code() >= 500) return uncertain();
                if (!response.isSuccessful()) return ApiErrors.from(response);
                UserHabitResponse value = response.body();
                if (response.code() != 201 || !owned(value, profile.id) || value.id <= 0) return uncertain();
                RoutineData.Habit habit = RoutineData.from(Collections.singletonList(value), clock).habits.get(0);
                if (!habit.scheduleComplete || habit.habitId != form.habitId || !Objects.equals(habit.frequency, request.frequency)) return uncertain();
                for (DayOfWeek day : DayOfWeek.values()) {
                    List<LocalTime> expected = new ArrayList<>();
                    RoutineFormState.DayDraft draft = form.days.get(day);
                    for (int i = 0; i < draft.activeCount(); i++) expected.add(LocalTime.parse(draft.times.get(i)));
                    if (!new HashSet<>(expected).equals(new HashSet<>(habit.schedule.getOrDefault(day, Collections.emptyList())))) return uncertain();
                }
                return ScreenState.success(habit);
            } catch (IOException unavailable) {
                return started ? uncertain() : ApiErrors.network();
            } catch (JsonParseException | IllegalArgumentException | IllegalStateException invalid) {
                return started ? uncertain() : invalidBody();
            }
        }, callback);
    }

    private static <T> ScreenState<T> uncertain() {
        return ScreenState.error("Não foi possível confirmar o resultado da criação.", UNCERTAIN, 0);
    }

    interface Access {
        ApiService api();

        Profile currentProfile() throws IOException;

        <T> void resource(Callable<ScreenState<T>> operation, Consumer<ScreenState<T>> callback);
    }

    private final Access access;
    private final Clock clock;

    public RoutineRepository(AuthRepository auth) {
        this(auth, Clock.systemDefaultZone());
    }

    public RoutineRepository(AuthRepository auth, Clock clock) {
        this(authenticated(Objects.requireNonNull(auth)), clock);
    }

    RoutineRepository(Access access, Clock clock) {
        this.access = Objects.requireNonNull(access);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void load(Consumer<ScreenState<RoutineData>> callback) {
        read(() -> {
            Profile profile = access.currentProfile();
            if (!identified(profile)) return ScreenState.error("Não foi possível identificar sua conta.");
            int user = profile.id;
            Response<List<UserHabitResponse>> response = access.api().listUserHabits(user).execute();
            if (!response.isSuccessful()) return ApiErrors.from(response);
            List<UserHabitResponse> values = response.body();
            if (values == null) return invalidBody();
            for (UserHabitResponse value : values)
                if (!owned(value, user)) return invalidBody();
            return ScreenState.success(RoutineData.from(values, clock));
        }, callback);
    }

    @Override
    public void detail(int id, Consumer<ScreenState<RoutineData.Habit>> callback) {
        read(() -> {
            Profile profile = access.currentProfile();
            if (!identified(profile)) return ScreenState.error("Não foi possível identificar sua conta.");
            int user = profile.id;
            Response<UserHabitResponse> response = access.api().getUserHabit(user, id).execute();
            if (!response.isSuccessful()) return ApiErrors.from(response);
            UserHabitResponse value = response.body();
            if (!owned(value, user) || !Objects.equals(id, value.id)) return invalidBody();
            return ScreenState.success(RoutineData.from(Collections.singletonList(value), clock).habits.get(0));
        }, callback);
    }

    private <T> void read(Callable<ScreenState<T>> operation, Consumer<ScreenState<T>> callback) {
        Objects.requireNonNull(callback);
        access.resource(() -> {
            try {
                return operation.call();
            } catch (IOException unavailable) {
                return ApiErrors.network();
            } catch (JsonParseException | IllegalArgumentException | IllegalStateException invalid) {
                return invalidBody();
            }
        }, callback);
    }

    private static boolean identified(Profile profile) {
        return profile != null && profile.id != null && profile.id > 0;
    }

    private static boolean owned(UserHabitResponse value, int user) {
        return value != null && value.id != null && value.habitId != null && Objects.equals(user, value.userId);
    }

    private static <T> ScreenState<T> invalidBody() {
        return ScreenState.error("O servidor retornou dados de rotina incompatíveis com a consulta.");
    }

    private static Access authenticated(AuthRepository auth) {
        return new Access() {
            @Override
            public ApiService api() {
                return auth.api();
            }

            @Override
            public Profile currentProfile() throws IOException {
                return auth.currentProfile();
            }

            @Override
            public <T> void resource(Callable<ScreenState<T>> operation, Consumer<ScreenState<T>> callback) {
                auth.resource(operation, callback);
            }
        };
    }
}
