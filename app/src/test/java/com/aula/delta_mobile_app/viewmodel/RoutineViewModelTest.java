package com.aula.delta_mobile_app.viewmodel;

import static org.junit.Assert.*;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;

import com.aula.delta_mobile_app.model.ApiModels.UserHabitResponse;
import com.aula.delta_mobile_app.model.ApiModels.UserHabitScheduleResponse;
import com.aula.delta_mobile_app.model.RoutineData;
import com.aula.delta_mobile_app.model.RoutineUiState;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.RoutineActions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class RoutineViewModelTest {
    private static final LocalDate INITIAL = LocalDate.of(2026, 1, 31);
    private final Gateway repository = new Gateway();
    private final List<Runnable> main = new ArrayList<>();
    private final List<RoutineUiState> published = new ArrayList<>();
    private final MutableClock clock = new MutableClock();
    private final ViewModelStore store = new ViewModelStore();
    private final Observer<RoutineUiState> observer = published::add;
    private MutableLiveData<Boolean> session;
    private RoutineViewModel viewModel;
    private ViewModelProvider provider;
    private boolean authenticated = true;
    private int creations;

    private static final class Gateway implements RoutineActions {
        final List<Consumer<ScreenState<RoutineData>>> callbacks = new ArrayList<>();

        @Override
        public void load(Consumer<ScreenState<RoutineData>> callback) {
            callbacks.add(callback);
        }

        @Override
        public void detail(int id, Consumer<ScreenState<RoutineData.Habit>> callback) {
            throw new AssertionError("Detail belongs to a later stage");
        }
    }

    private static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-02-01T01:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("America/Sao_Paulo");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Before
    public void setUp() {
        Thread mainThread = Thread.currentThread();
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override
            public void executeOnDiskIO(Runnable action) {
                action.run();
            }

            @Override
            public void postToMainThread(Runnable action) {
                action.run();
            }

            @Override
            public boolean isMainThread() {
                return Thread.currentThread() == mainThread;
            }
        });
        session = new MutableLiveData<>(true);
        provider = new ViewModelProvider(store, new ViewModelProvider.Factory() {
            @Override
            public <T extends ViewModel> T create(Class<T> modelClass) {
                creations++;
                return modelClass.cast(new RoutineViewModel(repository, clock, main::add, session, () -> authenticated));
            }
        });
        viewModel = provider.get(RoutineViewModel.class);
        viewModel.state().observeForever(observer);
    }

    @After
    public void tearDown() {
        try {
            viewModel.state().removeObserver(observer);
            store.clear();
        } finally {
            ArchTaskExecutor.getInstance().setDelegate(null);
        }
    }

    @Test
    public void initialSelectionUsesTheInjectedClockZoneWithoutLoadingAutomatically() {
        assertEquals(INITIAL, state().selectedDate);
        assertEquals(YearMonth.of(2026, 1), state().displayedMonth);
        assertEquals(ScreenState.Status.IDLE, state().reading.status);
        assertNull(state().data);
        assertNull(state().day);
        assertFalse(state().stale);
        assertTrue(repository.callbacks.isEmpty());
        clock.now = clock.now.plusSeconds(86400);
        assertEquals(INITIAL, state().selectedDate);
    }

    @Test
    public void monthChangesClampTheCurrentDayAndHandleLeapYearsAndYearBoundaries() {
        viewModel.changeMonth(1);
        assertEquals(LocalDate.of(2026, 2, 28), state().selectedDate);
        viewModel.changeMonth(1);
        assertEquals(LocalDate.of(2026, 3, 28), state().selectedDate);
        viewModel.selectDate(LocalDate.of(2024, 1, 31));
        viewModel.showMonth(YearMonth.of(2024, 2));
        assertEquals(LocalDate.of(2024, 2, 29), state().selectedDate);
        viewModel.selectDate(LocalDate.of(2026, 12, 31));
        viewModel.changeMonth(1);
        assertEquals(LocalDate.of(2027, 1, 31), state().selectedDate);
        viewModel.changeMonth(-1);
        assertEquals(LocalDate.of(2026, 12, 31), state().selectedDate);
        assertTrue(repository.callbacks.isEmpty());
    }

    @Test
    public void selectingADateSynchronizesTheDisplayedMonthWithoutPublishingDuplicates() {
        LocalDate date = LocalDate.of(2028, 2, 29);
        viewModel.selectDate(date);
        assertEquals(date, state().selectedDate);
        assertEquals(YearMonth.of(2028, 2), state().displayedMonth);
        int count = published.size();
        viewModel.selectDate(date);
        viewModel.showMonth(YearMonth.from(date));
        assertEquals(count, published.size());
    }

    @Test
    public void loadingIsPublishedAndEquivalentRequestsAreDeduplicated() {
        viewModel.load();
        assertEquals(ScreenState.Status.LOADING, state().reading.status);
        assertTrue(state().reading.busy());
        assertNull(state().day);
        viewModel.load();
        viewModel.refresh();
        viewModel.retry();
        assertEquals(1, repository.callbacks.size());
    }

    @Test
    public void successfulCollectionIsProjectedAndSelectionDoesNotFetchAgain() {
        RoutineData data = data(73, 1);
        load(data);
        assertSame(data, state().data);
        assertEquals(new BigDecimal("80"), state().day.totalLiters);
        assertFalse(state().stale);
        viewModel.selectDate(INITIAL.plusDays(1));
        assertTrue(state().day.confirmedEmpty());
        viewModel.showMonth(YearMonth.of(2026, 2));
        viewModel.selectDate(LocalDate.of(2026, 2, 7));
        viewModel.load();
        assertEquals(new BigDecimal("80"), state().day.totalLiters);
        assertEquals(1, repository.callbacks.size());
    }

    @Test
    public void emptyCollectionIsSuccessWithConfirmedZeroInsteadOfANewGlobalStatus() {
        load(RoutineData.from(Collections.emptyList(), clock));
        assertEquals(ScreenState.Status.SUCCESS, state().reading.status);
        assertTrue(state().day.confirmedEmpty());
        assertEquals(BigDecimal.ZERO, state().day.totalLiters);
    }

    @Test
    public void changingSelectionDuringLoadingProjectsTheResponseOnTheLatestDay() {
        viewModel.load();
        viewModel.selectDate(LocalDate.of(2026, 2, 7));
        reply(0, ScreenState.success(data(73, 1)));
        assertEquals(LocalDate.of(2026, 2, 7), state().day.date);
        assertEquals(new BigDecimal("80"), state().day.totalLiters);
        assertEquals(1, repository.callbacks.size());
    }

    @Test
    public void refreshRetainsPreviousDataAndMarksItStaleUntilSuccess() {
        RoutineData previous = data(73, 1);
        load(previous);
        RoutineUiState old = state();
        viewModel.refresh();
        assertEquals(ScreenState.Status.LOADING, state().reading.status);
        assertNull(state().reading.data);
        assertSame(previous, state().data);
        assertTrue(state().stale);
        viewModel.selectDate(LocalDate.of(2026, 2, 7));
        assertEquals(new BigDecimal("80"), state().day.totalLiters);
        RoutineData replacement = data(73, 4);
        reply(1, ScreenState.success(replacement));
        assertSame(replacement, state().data);
        assertFalse(state().stale);
        assertEquals(new BigDecimal("15"), state().day.totalLiters);
        assertEquals(INITIAL, old.selectedDate);
        assertEquals(new BigDecimal("80"), old.day.totalLiters);
    }

    @Test
    public void refreshFailurePreservesStaleProjectionAndRetryUpdatesIt() {
        RoutineData previous = data(73, 1);
        load(previous);
        viewModel.refresh();
        reply(1, ScreenState.error("Serviço indisponível.", 503, 0));
        assertEquals(ScreenState.Status.ERROR, state().reading.status);
        assertEquals(503, state().reading.code);
        assertEquals("Serviço indisponível.", state().reading.message);
        assertEquals("", state().reading.displayMessage());
        assertSame(previous, state().data);
        assertTrue(state().stale);
        viewModel.retry();
        reply(2, ScreenState.success(data(73, 4)));
        assertEquals(new BigDecimal("15"), state().day.totalLiters);
        assertFalse(state().stale);
    }

    @Test
    public void initialFailureCanRetryAndRetryDoesNothingOutsideError() {
        viewModel.retry();
        assertTrue(repository.callbacks.isEmpty());
        viewModel.load();
        reply(0, ScreenState.error("Sem conexão."));
        assertNull(state().day);
        viewModel.retry();
        reply(1, ScreenState.success(data(73, 1)));
        viewModel.retry();
        assertEquals(2, repository.callbacks.size());
        assertEquals(ScreenState.Status.SUCCESS, state().reading.status);
    }

    @Test
    public void retryAfterIsPreservedAndAllLoadCommandsRespectTheDeadline() {
        viewModel.load();
        long retryAt = clock.millis() + 60000;
        reply(0, ScreenState.error("Aguarde.", 429, retryAt));
        assertEquals(retryAt, state().reading.retryAt);
        viewModel.retry();
        viewModel.refresh();
        viewModel.load();
        assertEquals(1, repository.callbacks.size());
        clock.now = Instant.ofEpochMilli(retryAt);
        viewModel.retry();
        assertEquals(2, repository.callbacks.size());
        reply(1, ScreenState.success(data(73, 1)));
        assertEquals(ScreenState.Status.SUCCESS, state().reading.status);
    }

    @Test
    public void invalidateDropsCachedDataAndDiscardsOutOfOrderReplies() {
        viewModel.load();
        viewModel.invalidate();
        assertEquals(ScreenState.Status.IDLE, state().reading.status);
        assertNull(state().data);
        viewModel.load();
        reply(1, ScreenState.success(data(73, 4)));
        RoutineUiState latest = state();
        reply(0, ScreenState.success(data(73, 1)));
        assertSame(latest, state());
        assertEquals(new BigDecimal("15"), state().day.totalLiters);
    }

    @Test
    public void duplicateCompletionCannotOverwriteSuccessOrANewerRefresh() {
        load(data(73, 1));
        RoutineUiState success = state();
        reply(0, ScreenState.error("Duplicada."));
        assertSame(success, state());
        viewModel.refresh();
        reply(0, ScreenState.error("Antiga."));
        assertEquals(ScreenState.Status.LOADING, state().reading.status);
        reply(1, ScreenState.success(data(73, 4)));
        assertEquals(new BigDecimal("15"), state().day.totalLiters);
    }

    @Test
    public void callbackIsPublishedOnlyAfterTheInjectedMainDispatcherRuns() throws Exception {
        viewModel.load();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        RoutineData data = data(73, 1);
        Thread worker = new Thread(() -> {
            try {
                repository.callbacks.get(0).accept(ScreenState.success(data));
            } catch (Throwable unexpected) {
                failure.set(unexpected);
            }
        });
        worker.start();
        worker.join(5000);
        assertFalse(worker.isAlive());
        assertNull(failure.get());
        assertEquals(ScreenState.Status.LOADING, state().reading.status);
        assertEquals(1, main.size());
        drain();
        assertEquals(ScreenState.Status.SUCCESS, state().reading.status);
        assertSame(data, state().data);
    }

    @Test
    public void logoutClearsCachedDataAndRejectsQueuedResults() {
        load(data(73, 1));
        viewModel.refresh();
        repository.callbacks.get(1).accept(ScreenState.success(data(73, 4)));
        authenticated = false;
        session.setValue(false);
        assertNull(state().data);
        assertNull(state().day);
        RoutineUiState loggedOut = state();
        drain();
        assertSame(loggedOut, state());
        viewModel.load();
        assertEquals(401, state().reading.code);
        assertEquals(2, repository.callbacks.size());
    }

    @Test
    public void accountSignalInvalidatesTheOldRequestEvenWhenBothAccountsAreSignedIn() {
        viewModel.load();
        session.setValue(true);
        assertNull(state().data);
        viewModel.load();
        reply(0, ScreenState.success(data(73, 1)));
        assertEquals(ScreenState.Status.LOADING, state().reading.status);
        reply(1, ScreenState.success(data(94, 4)));
        assertEquals(94, state().data.habits.get(0).userId);
        assertEquals(new BigDecimal("15"), state().day.totalLiters);
    }

    @Test
    public void accountChangeDropsPreviouslyPublishedCollectionButKeepsCalendarSelection() {
        load(data(73, 1));
        LocalDate selected = LocalDate.of(2026, 2, 7);
        viewModel.selectDate(selected);
        session.setValue(false);
        session.setValue(true);
        assertNull(state().data);
        assertNull(state().day);
        assertEquals(selected, state().selectedDate);
        viewModel.load();
        reply(1, ScreenState.success(data(94, 4)));
        assertEquals(94, state().data.habits.get(0).userId);
    }

    @Test
    public void sessionCheckRejectsResultsBeforeTheLogoutSignalArrives() {
        viewModel.load();
        authenticated = false;
        reply(0, ScreenState.success(data(73, 1)));
        assertEquals(ScreenState.Status.ERROR, state().reading.status);
        assertEquals(401, state().reading.code);
        assertNull(state().data);
    }

    @Test
    public void authenticationAndAuthorizationErrorsDoNotRetainProtectedData() {
        for (int code : new int[] {401, 403}) {
            viewModel.load();
            reply(repository.callbacks.size() - 1, ScreenState.success(data(73, 1)));
            viewModel.refresh();
            reply(repository.callbacks.size() - 1, ScreenState.error("Acesso indisponível.", code, 0));
            assertEquals(code, state().reading.code);
            assertNull(state().data);
            assertNull(state().day);
        }
    }

    @Test
    public void invalidTerminalResultsBecomeRetryableErrorsInsteadOfInfiniteLoading() {
        List<ScreenState<RoutineData>> invalid = Arrays.asList(null, ScreenState.idle(), ScreenState.loading(), ScreenState.success(null));
        for (ScreenState<RoutineData> result : invalid) {
            viewModel.load();
            reply(repository.callbacks.size() - 1, result);
            assertEquals(ScreenState.Status.ERROR, state().reading.status);
            assertFalse(state().reading.busy());
            assertNull(state().data);
        }
    }

    @Test
    public void retainedProviderInstancePreservesSelectionAndAnOngoingRequest() {
        LocalDate selected = LocalDate.of(2028, 2, 29);
        viewModel.selectDate(selected);
        viewModel.load();
        clock.now = clock.now.plusSeconds(86400);
        RoutineViewModel retained = provider.get(RoutineViewModel.class);
        assertSame(viewModel, retained);
        assertEquals(selected, retained.state().getValue().selectedDate);
        retained.load();
        assertEquals(1, creations);
        assertEquals(1, repository.callbacks.size());
    }

    @Test
    public void clearingRemovesSessionObserverAndDiscardsQueuedOrLateCallbacks() {
        viewModel.load();
        repository.callbacks.get(0).accept(ScreenState.success(data(73, 1)));
        RoutineUiState before = state();
        int publications = published.size();
        assertTrue(session.hasObservers());
        store.clear();
        assertFalse(session.hasObservers());
        drain();
        assertSame(before, state());
        repository.callbacks.get(0).accept(ScreenState.success(data(73, 4)));
        assertTrue(main.isEmpty());
        viewModel.load();
        viewModel.refresh();
        viewModel.retry();
        viewModel.invalidate();
        viewModel.selectDate(INITIAL.plusDays(1));
        viewModel.changeMonth(1);
        session.setValue(false);
        assertEquals(1, repository.callbacks.size());
        assertEquals(publications, published.size());
    }

    @Test
    public void clearingTheStoreCreatesANewInstanceWithoutPromisingProcessRestoration() {
        viewModel.selectDate(LocalDate.of(2030, 1, 1));
        store.clear();
        RoutineViewModel fresh = provider.get(RoutineViewModel.class);
        assertNotSame(viewModel, fresh);
        assertEquals(INITIAL, fresh.state().getValue().selectedDate);
        assertEquals(ScreenState.Status.IDLE, fresh.state().getValue().reading.status);
        assertEquals(2, creations);
    }

    @Test
    public void logoutDuringLoadingPublicationPreventsStartingTheInvalidatedRequest() {
        Observer<RoutineUiState> logout = value -> {
            if (value.reading.busy()) session.setValue(false);
        };
        viewModel.state().observeForever(logout);
        try {
            viewModel.load();
            assertTrue(repository.callbacks.isEmpty());
            assertNull(state().data);
            assertEquals(ScreenState.Status.IDLE, state().reading.status);
        } finally {
            viewModel.state().removeObserver(logout);
        }
    }

    @Test
    public void calendarWeekCanSelectAdjacentMonthDaysWithoutReloading() {
        load(data(73, 1));
        viewModel.selectDate(LocalDate.of(2026, 8, 1));
        LocalDate monday = state().selectedDate.minusDays(state().selectedDate.getDayOfWeek().getValue() - 1);
        assertEquals(LocalDate.of(2026, 7, 27), monday);
        viewModel.selectDate(monday);
        assertEquals(YearMonth.of(2026, 7), state().displayedMonth);
        viewModel.selectDate(monday.plusDays(6));
        assertEquals(LocalDate.of(2026, 8, 2), state().selectedDate);
        assertEquals(YearMonth.of(2026, 8), state().displayedMonth);
        assertEquals(1, repository.callbacks.size());
    }

    @Test
    public void datePickerUtcSelectionDoesNotShiftTheChosenCalendarDay() {
        load(data(73, 1));
        long utcSelection = Instant.parse("2028-02-29T00:00:00Z").toEpochMilli();
        viewModel.selectDate(Instant.ofEpochMilli(utcSelection).atZone(java.time.ZoneOffset.UTC).toLocalDate());
        assertEquals(LocalDate.of(2028, 2, 29), state().selectedDate);
        assertEquals(YearMonth.of(2028, 2), state().displayedMonth);
        assertEquals(1, repository.callbacks.size());
    }

    @Test
    public void todayActionChangesSelectionWithoutLosingTheLoadedSnapshot() {
        RoutineData snapshot = data(73, 1);
        load(snapshot);
        viewModel.selectDate(LocalDate.of(2028, 2, 29));
        clock.now = clock.now.plusSeconds(86400);
        viewModel.selectDate(LocalDate.now(clock));
        assertEquals(LocalDate.of(2026, 2, 1), state().selectedDate);
        assertSame(snapshot, state().data);
        assertEquals(1, repository.callbacks.size());
    }

    @Test
    public void supportedCalendarEndpointsKeepProjectionAndCanReturnToTheCurrentMonth() {
        load(data(73, 1));
        viewModel.selectDate(LocalDate.of(1900, 1, 1));
        assertEquals(YearMonth.of(1900, 1), state().displayedMonth);
        viewModel.selectDate(LocalDate.of(2100, 12, 31));
        assertEquals(YearMonth.of(2100, 12), state().displayedMonth);
        viewModel.showMonth(YearMonth.from(INITIAL));
        assertEquals(INITIAL, state().selectedDate);
        assertEquals(1, repository.callbacks.size());
    }

    private RoutineUiState state() {
        return viewModel.state().getValue();
    }

    private RoutineData data(int user, int type) {
        UserHabitResponse value = new UserHabitResponse();
        value.id = 105;
        value.userId = user;
        value.habitId = type;
        value.frequency = 1;
        value.daysOfWeek = Collections.singletonList(6);
        UserHabitScheduleResponse schedule = new UserHabitScheduleResponse();
        schedule.dayOfWeek = 6;
        schedule.startTimes = Collections.singletonList("07:00");
        value.schedule = Collections.singletonList(schedule);
        return RoutineData.from(Collections.singletonList(value), clock);
    }

    private void load(RoutineData data) {
        viewModel.load();
        reply(repository.callbacks.size() - 1, ScreenState.success(data));
    }

    private void reply(int request, ScreenState<RoutineData> result) {
        repository.callbacks.get(request).accept(result);
        drain();
    }

    private void drain() {
        while (!main.isEmpty()) main.remove(0).run();
    }
}
