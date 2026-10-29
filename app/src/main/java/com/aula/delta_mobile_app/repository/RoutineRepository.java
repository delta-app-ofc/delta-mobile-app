package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.ApiModels.UserHabitResponse;
import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.model.RoutineData;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.network.ApiErrors;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import retrofit2.Response;

public final class RoutineRepository implements RoutineActions {
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
