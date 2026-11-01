package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.ApiModels.CreateUserHabitRequest;
import com.aula.delta_mobile_app.model.ApiModels.UserHabitResponse;
import com.aula.delta_mobile_app.model.ApiModels.UserHabitScheduleResponse;
import com.aula.delta_mobile_app.model.ApiModels.UpdateUserHabitRequest;
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
import java.util.Map;
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
                if (response.code() >= 500) return uncertainCreate();
                if (!response.isSuccessful()) return ApiErrors.from(response);
                UserHabitResponse value = response.body();
                if (response.code() != 201 || !owned(value, profile.id) || value.id <= 0) return uncertainCreate();
                RoutineData.Habit habit;
                try {
                    habit = RoutineData.from(Collections.singletonList(value), clock).habits.get(0);
                } catch (JsonParseException | IllegalArgumentException | IllegalStateException invalidSchedule) {
                    value.frequency = request.frequency;
                    value.daysOfWeek = request.daysOfWeek;
                    value.schedule = request.schedule;
                    habit = RoutineData.from(Collections.singletonList(value), clock).habits.get(0);
                }
                if (habit.habitId != form.habitId) return uncertainCreate();
                if (!habit.scheduleComplete || !Objects.equals(habit.frequency, request.frequency) || !matchesSchedule(form, habit)) {
                    value.frequency = request.frequency;
                    value.daysOfWeek = request.daysOfWeek;
                    value.schedule = request.schedule;
                    habit = RoutineData.from(Collections.singletonList(value), clock).habits.get(0);
                }
                if (!habit.scheduleComplete || !Objects.equals(habit.frequency, request.frequency) || !matchesSchedule(form, habit)) return uncertainCreate();
                if (scheduleStore != null) scheduleStore.save(profile.id, habit.id, request.frequency, request.daysOfWeek, request.schedule);
                return ScreenState.success(habit);
            } catch (IOException unavailable) {
                return started ? uncertainCreate() : ApiErrors.network();
            } catch (JsonParseException | IllegalArgumentException | IllegalStateException invalid) {
                return started ? uncertainCreate() : invalidBody();
            }
        }, callback);
    }

    private static boolean matchesSchedule(RoutineFormState form, RoutineData.Habit habit) {
        for (DayOfWeek day : DayOfWeek.values()) {
            List<LocalTime> expected = new ArrayList<>();
            RoutineFormState.DayDraft draft = form.days.get(day);
            for (int i = 0; i < draft.activeCount(); i++) expected.add(LocalTime.parse(draft.times.get(i)));
            if (!new HashSet<>(expected).equals(new HashSet<>(habit.schedule.getOrDefault(day, Collections.emptyList())))) return false;
        }
        return true;
    }

    @Override
    public void update(RoutineFormState form, Consumer<ScreenState<RoutineData.Habit>> callback) {
        if (form == null || form.mode != RoutineFormState.Mode.EDIT || form.id == null || form.userId == null || form.effectiveFrom == null || !form.validate().valid()) {
            callback.accept(ScreenState.error("Confira os campos do formulário.", 400, 0));
            return;
        }
        access.resource(() -> {
            boolean started = false;
            try {
                Profile profile = access.currentProfile();
                if (!identified(profile)) return ScreenState.error("Não foi possível identificar sua conta.");
                if (!Objects.equals(profile.id, form.userId)) return ScreenState.error("Este hábito não pertence à conta atual.", 403, 0);
                Response<UserHabitResponse> current = access.api().getUserHabit(profile.id, form.id).execute();
                if (!current.isSuccessful()) return ApiErrors.from(current);
                if (!owned(current.body(), profile.id) || !Objects.equals(form.id, current.body().id))
                    return ScreenState.error("Não foi possível confirmar o vínculo deste hábito.", 403, 0);
                UpdateUserHabitRequest request = new UpdateUserHabitRequest();
                request.habitId = form.habitId;
                request.frequency = Integer.valueOf(form.frequency.trim());
                request.daysOfWeek = days(form);
                request.schedule = schedule(form);
                request.effectiveFrom = form.effectiveFrom.toString();
                Call<UserHabitResponse> call = access.api().updateUserHabit(profile.id, form.id, request);
                started = true;
                Response<UserHabitResponse> response = call.execute();
                if (response.code() >= 500) return uncertain();
                if (!response.isSuccessful()) return ApiErrors.from(response);
                UserHabitResponse value = response.body();
                if (response.code() != 200 || !owned(value, profile.id) || !Objects.equals(form.id, value.id)) return uncertain();
                if (value.effectiveFrom != null && !Objects.equals(request.effectiveFrom, value.effectiveFrom)) return uncertain();
                boolean responseHasSchedule = value.frequency != null && value.daysOfWeek != null && !value.daysOfWeek.isEmpty()
                    && value.schedule != null && !value.schedule.isEmpty();
                RoutineData.Habit habit;
                try {
                    habit = RoutineData.from(Collections.singletonList(value), clock).habits.get(0);
                } catch (JsonParseException | IllegalArgumentException | IllegalStateException invalidSchedule) {
                    if (responseHasSchedule) return uncertain();
                    value.frequency = request.frequency;
                    value.daysOfWeek = request.daysOfWeek;
                    value.schedule = request.schedule;
                    habit = RoutineData.from(Collections.singletonList(value), clock).habits.get(0);
                }
                if (!matches(form, habit)) {
                    if (responseHasSchedule) return uncertain();
                    value.frequency = request.frequency;
                    value.daysOfWeek = request.daysOfWeek;
                    value.schedule = request.schedule;
                    habit = RoutineData.from(Collections.singletonList(value), clock).habits.get(0);
                }
                if (!matches(form, habit)) return uncertain();
                if (scheduleStore != null) scheduleStore.save(profile.id, habit.id, request.frequency, request.daysOfWeek, request.schedule);
                return ScreenState.success(habit);
            } catch (IOException unavailable) {
                return started ? uncertain() : ApiErrors.network();
            } catch (JsonParseException | IllegalArgumentException | IllegalStateException invalid) {
                return started ? uncertain() : invalidBody();
            }
        }, callback);
    }

    @Override
    public void delete(int id, int userId, Consumer<ScreenState<Void>> callback) {
        if (id <= 0 || userId <= 0) {
            callback.accept(ScreenState.error("Não foi possível identificar o hábito.", 400, 0));
            return;
        }
        access.resource(() -> {
            boolean started = false;
            try {
                Profile profile = access.currentProfile();
                if (!identified(profile)) return ScreenState.error("Não foi possível identificar sua conta.");
                if (!Objects.equals(profile.id, userId)) return ScreenState.error("Este hábito não pertence à conta atual.", 403, 0);
                Response<UserHabitResponse> current = access.api().getUserHabit(profile.id, id).execute();
                if (!current.isSuccessful()) return ApiErrors.from(current);
                if (!owned(current.body(), profile.id) || !Objects.equals(id, current.body().id))
                    return ScreenState.error("Não foi possível confirmar o vínculo deste hábito.", 403, 0);
                Call<Void> call = access.api().deleteUserHabit(profile.id, id);
                started = true;
                Response<Void> response = call.execute();
                if (response.code() >= 500) return uncertain();
                if (!response.isSuccessful()) return ApiErrors.from(response);
                if (response.code() != 204) return uncertain();
                if (scheduleStore != null) scheduleStore.remove(profile.id, id);
                return ScreenState.success(null);
            } catch (IOException unavailable) {
                return started ? uncertain() : ApiErrors.network();
            } catch (JsonParseException | IllegalArgumentException | IllegalStateException invalid) {
                return started ? uncertain() : invalidBody();
            }
        }, callback);
    }

    private static <T> ScreenState<T> uncertain() {
        return ScreenState.error("Não foi possível confirmar o resultado da alteração.", UNCERTAIN, 0);
    }

    private static <T> ScreenState<T> uncertainCreate() {
        return ScreenState.error("Não foi possível salvar o hábito. Tente novamente.", UNCERTAIN, 0);
    }

    private static List<Integer> days(RoutineFormState form) {
        List<Integer> values = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) if (form.days.get(day).selected) values.add(day.getValue());
        return values;
    }

    private static List<UserHabitScheduleResponse> schedule(RoutineFormState form) {
        List<UserHabitScheduleResponse> values = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            RoutineFormState.DayDraft draft = form.days.get(day);
            if (!draft.selected) continue;
            UserHabitScheduleResponse item = new UserHabitScheduleResponse();
            item.dayOfWeek = day.getValue();
            item.startTimes = new ArrayList<>(draft.times.subList(0, draft.activeCount()));
            values.add(item);
        }
        return values;
    }

    private static boolean matches(RoutineFormState form, RoutineData.Habit habit) {
        boolean complete = form.effectiveFrom == null ? habit.scheduleComplete : habit.scheduleCompleteAt(form.effectiveFrom);
        Integer frequency = form.effectiveFrom == null ? habit.frequency : habit.frequencyAt(form.effectiveFrom);
        if (!complete || habit.habitId != form.habitId || !Objects.equals(frequency, Integer.valueOf(form.frequency.trim()))) return false;
        for (DayOfWeek day : DayOfWeek.values()) {
            List<LocalTime> expected = new ArrayList<>();
            RoutineFormState.DayDraft draft = form.days.get(day);
            for (int i = 0; i < draft.activeCount(); i++) expected.add(LocalTime.parse(draft.times.get(i)));
            Map<DayOfWeek, List<LocalTime>> schedule = form.effectiveFrom == null ? habit.schedule : habit.scheduleAt(form.effectiveFrom);
            if (!new HashSet<>(expected).equals(new HashSet<>(schedule.getOrDefault(day, Collections.emptyList())))) return false;
        }
        return true;
    }

    interface Access {
        ApiService api();

        Profile currentProfile() throws IOException;

        <T> void resource(Callable<ScreenState<T>> operation, Consumer<ScreenState<T>> callback);
    }

    private final Access access;
    private final Clock clock;
    private final RoutineScheduleStore scheduleStore;

    public RoutineRepository(AuthRepository auth) {
        this(auth, Clock.systemDefaultZone());
    }

    public RoutineRepository(AuthRepository auth, Clock clock) {
        AuthRepository value = Objects.requireNonNull(auth);
        access = authenticated(value);
        this.clock = Objects.requireNonNull(clock);
        scheduleStore = new RoutineScheduleStore(value.applicationContext());
    }

    RoutineRepository(Access access, Clock clock) {
        this(access, clock, null);
    }

    RoutineRepository(Access access, Clock clock, RoutineScheduleStore scheduleStore) {
        this.access = Objects.requireNonNull(access);
        this.clock = Objects.requireNonNull(clock);
        this.scheduleStore = scheduleStore;
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
            if (scheduleStore != null) scheduleStore.apply(user, values);
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
            if (scheduleStore != null) scheduleStore.apply(user, Collections.singletonList(value));
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
