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
import com.aula.delta_mobile_app.model.RoutineFormState;
import com.aula.delta_mobile_app.model.RoutineFormValidator;
import com.aula.delta_mobile_app.model.RoutineUiState;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.RoutineActions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
        final List<Consumer<ScreenState<RoutineData.Habit>>> posts = new ArrayList<>();

        @Override
        public void create(RoutineFormState form, Consumer<ScreenState<RoutineData.Habit>> callback) {
            assertTrue(form.valid());
            posts.add(callback);
        }
        final List<Consumer<ScreenState<RoutineData>>> callbacks = new ArrayList<>();
        final List<Integer> detailIds = new ArrayList<>();
        final List<Consumer<ScreenState<RoutineData.Habit>>> details = new ArrayList<>();

        @Override
        public void load(Consumer<ScreenState<RoutineData>> callback) {
            callbacks.add(callback);
        }

        @Override
        public void detail(int id, Consumer<ScreenState<RoutineData.Habit>> callback) {
            detailIds.add(id);
            details.add(callback);
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

    @Test
    public void openingDetailsCapturesRecordOwnerDayAndExecutionWithoutReloadingCollection() {
        RoutineData collection = data(73, 1);
        load(collection);
        assertTrue(viewModel.openDetails(105, LocalTime.of(7, 0)));
        assertEquals(105, state().detailTarget.id);
        assertEquals(73, state().detailTarget.userId);
        assertEquals(INITIAL, state().detailTarget.date);
        assertEquals(LocalTime.of(7, 0), state().detailTarget.startTime);
        assertEquals(ScreenState.Status.LOADING, state().detail.status);
        assertNull(state().details);
        assertSame(collection, state().data);
        assertEquals(ScreenState.Status.SUCCESS, state().reading.status);
        assertEquals(Collections.singletonList(105), repository.detailIds);
        assertEquals(1, repository.callbacks.size());
    }

    @Test
    public void duplicateDetailOpeningAndRetryOutsideErrorDoNotStartEquivalentRequests() {
        load(data(73, 1));
        viewModel.retryDetail();
        viewModel.openDetails(105, LocalTime.of(7, 0));
        viewModel.openDetails(105, LocalTime.of(7, 0));
        viewModel.retryDetail();
        assertEquals(1, repository.details.size());
        replyDetail(0, ScreenState.success(habit(105, 73, 1, "07:00")));
        viewModel.openDetails(105, LocalTime.of(7, 0));
        viewModel.retryDetail();
        assertEquals(1, repository.details.size());
    }

    @Test
    public void staleUnknownOrUnscheduledItemsCannotBeOpened() {
        assertFalse(viewModel.openDetails(105, LocalTime.of(7, 0)));
        load(data(73, 1));
        assertFalse(viewModel.openDetails(106, LocalTime.of(7, 0)));
        assertFalse(viewModel.openDetails(105, LocalTime.of(8, 0)));
        assertFalse(viewModel.openDetails(105, null));
        viewModel.refresh();
        assertFalse(viewModel.openDetails(105, LocalTime.of(7, 0)));
        reply(1, ScreenState.error("Sem conexão."));
        assertFalse(viewModel.openDetails(105, LocalTime.of(7, 0)));
        assertTrue(repository.details.isEmpty());
    }

    @Test
    public void selectingAnotherRecordClearsOldContentAndRejectsItsLateResponse() {
        load(RoutineData.from(Arrays.asList(response(105, 73, 1, "07:00"), response(106, 73, 4, "07:00")), clock));
        viewModel.openDetails(105, LocalTime.of(7, 0));
        replyDetail(0, ScreenState.success(habit(105, 73, 1, "07:00")));
        assertEquals(105, state().details.habit.id);
        viewModel.openDetails(106, LocalTime.of(7, 0));
        assertNull(state().details);
        assertEquals(106, state().detailTarget.id);
        replyDetail(1, ScreenState.success(habit(106, 73, 4, "07:00")));
        RoutineUiState current = state();
        replyDetail(0, ScreenState.success(habit(105, 73, 1, "07:00")));
        assertSame(current, state());
        assertEquals(106, state().details.habit.id);
        assertEquals(new BigDecimal("15"), state().details.dailyLiters);
    }

    @Test
    public void selectingAnotherExecutionOfTheSameHabitUsesANewContextAndSingleDailyTotal() {
        UserHabitResponse value = response(105, 73, 1, "07:00", "20:00");
        RoutineData collection = RoutineData.from(Collections.singletonList(value), clock);
        load(collection);
        viewModel.openDetails(105, LocalTime.of(7, 0));
        viewModel.openDetails(105, LocalTime.of(20, 0));
        replyDetail(0, ScreenState.success(collection.habits.get(0)));
        assertEquals(ScreenState.Status.LOADING, state().detail.status);
        replyDetail(1, ScreenState.success(collection.habits.get(0)));
        assertEquals(LocalTime.of(20, 0), state().detailTarget.startTime);
        assertTrue(state().details.selectedExecutionPresent);
        assertEquals(Integer.valueOf(2), state().details.executionCount);
        assertEquals(new BigDecimal("80"), state().details.executionLiters);
        assertEquals(new BigDecimal("160"), state().details.dailyLiters);
        assertEquals(state().day.totalLiters, state().details.dailyLiters);
        assertEquals(Arrays.asList(105, 105), repository.detailIds);
    }

    @Test
    public void detailReportsChangedSchedulesWithoutInventingTheSelectedExecution() {
        load(data(73, 1));
        viewModel.openDetails(105, LocalTime.of(7, 0));
        replyDetail(0, ScreenState.success(habit(105, 73, 1, "20:00")));
        assertEquals(ScreenState.Status.SUCCESS, state().detail.status);
        assertFalse(state().details.selectedExecutionPresent);
        assertEquals(Collections.singletonList(LocalTime.of(20, 0)), state().details.times);
        assertEquals(new BigDecimal("80"), state().details.dailyLiters);
        UserHabitResponse changedDay = response(105, 73, 1, "07:00");
        changedDay.daysOfWeek = Collections.singletonList(1);
        changedDay.schedule.get(0).dayOfWeek = 1;
        viewModel.closeDetails();
        viewModel.openDetails(105, LocalTime.of(7, 0));
        replyDetail(1, ScreenState.success(RoutineData.from(Collections.singletonList(changedDay), clock).habits.get(0)));
        assertEquals(Integer.valueOf(0), state().details.executionCount);
        assertEquals(BigDecimal.ZERO, state().details.dailyLiters);
        assertFalse(state().details.selectedExecutionPresent);
    }

    @Test
    public void legacyDetailAndUnknownEstimateNeverProduceAnInventedDailyValue() {
        load(data(73, 1));
        viewModel.openDetails(105, LocalTime.of(7, 0));
        UserHabitResponse legacy = response(105, 73, 1, "07:00");
        legacy.schedule = null;
        replyDetail(0, ScreenState.success(RoutineData.from(Collections.singletonList(legacy), clock).habits.get(0)));
        assertFalse(state().details.scheduleComplete);
        assertNull(state().details.executionCount);
        assertNull(state().details.dailyLiters);
        assertFalse(state().details.selectedExecutionPresent);
        viewModel.closeDetails();
        viewModel.openDetails(105, LocalTime.of(7, 0));
        replyDetail(1, ScreenState.success(habit(105, 73, 99, "07:00")));
        assertTrue(state().details.selectedExecutionPresent);
        assertNull(state().details.executionLiters);
        assertNull(state().details.dailyLiters);
    }

    @Test
    public void detailErrorsPreserveContextAndRetryIndependentlyFromTheCollection() {
        RoutineData collection = data(73, 1);
        load(collection);
        viewModel.openDetails(105, LocalTime.of(7, 0));
        RoutineUiState.DetailTarget target = state().detailTarget;
        replyDetail(0, ScreenState.error("Não encontrado.", 404, 0));
        assertSame(target, state().detailTarget);
        assertEquals(404, state().detail.code);
        assertEquals("Não encontrado.", state().detail.message);
        assertNull(state().details);
        assertSame(collection, state().data);
        viewModel.retryDetail();
        replyDetail(1, ScreenState.error("Sem conexão."));
        viewModel.retryDetail();
        replyDetail(2, ScreenState.success(habit(105, 73, 1, "07:00")));
        assertEquals(ScreenState.Status.SUCCESS, state().detail.status);
        assertSame(target, state().detailTarget);
        assertEquals(Arrays.asList(105, 105, 105), repository.detailIds);
        assertEquals(1, repository.callbacks.size());
    }

    @Test
    public void detailRetryRespectsRetryAfterWithoutBlockingCalendarData() {
        load(data(73, 1));
        viewModel.openDetails(105, LocalTime.of(7, 0));
        long retryAt = clock.millis() + 60000;
        replyDetail(0, ScreenState.error("Aguarde.", 429, retryAt));
        viewModel.retryDetail();
        viewModel.openDetails(105, LocalTime.of(7, 0));
        assertEquals(1, repository.details.size());
        assertEquals(retryAt, state().detail.retryAt);
        assertEquals(ScreenState.Status.SUCCESS, state().reading.status);
        clock.now = Instant.ofEpochMilli(retryAt);
        viewModel.retryDetail();
        assertEquals(2, repository.details.size());
    }

    @Test
    public void closingDetailsPreservesCalendarAndRejectsRepliesAfterReopening() {
        RoutineData collection = data(73, 1);
        load(collection);
        viewModel.openDetails(105, LocalTime.of(7, 0));
        viewModel.closeDetails();
        assertNull(state().detailTarget);
        assertNull(state().details);
        assertSame(collection, state().data);
        assertEquals(INITIAL, state().selectedDate);
        viewModel.openDetails(105, LocalTime.of(7, 0));
        replyDetail(0, ScreenState.success(habit(105, 73, 4, "07:00")));
        assertEquals(ScreenState.Status.LOADING, state().detail.status);
        replyDetail(1, ScreenState.success(habit(105, 73, 1, "07:00")));
        assertEquals(1, state().details.habit.habitId);
    }

    @Test
    public void changingDayOrRefreshingInvalidatesPendingDetails() {
        load(data(73, 1));
        viewModel.openDetails(105, LocalTime.of(7, 0));
        viewModel.selectDate(LocalDate.of(2026, 2, 7));
        replyDetail(0, ScreenState.success(habit(105, 73, 1, "07:00")));
        assertNull(state().detailTarget);
        viewModel.openDetails(105, LocalTime.of(7, 0));
        viewModel.refresh();
        replyDetail(1, ScreenState.success(habit(105, 73, 1, "07:00")));
        assertNull(state().details);
        assertNull(state().detailTarget);
        assertEquals(ScreenState.Status.LOADING, state().reading.status);
        assertEquals(LocalDate.of(2026, 2, 7), state().selectedDate);
    }

    @Test
    public void accountChangesInvalidateDetailRequestsEvenForTheSameRecordId() {
        load(data(73, 1));
        viewModel.openDetails(105, LocalTime.of(7, 0));
        session.setValue(true);
        viewModel.load();
        reply(1, ScreenState.success(data(94, 4)));
        viewModel.openDetails(105, LocalTime.of(7, 0));
        replyDetail(0, ScreenState.success(habit(105, 73, 1, "07:00")));
        assertEquals(ScreenState.Status.LOADING, state().detail.status);
        replyDetail(1, ScreenState.success(habit(105, 94, 4, "07:00")));
        assertEquals(94, state().details.habit.userId);
        assertEquals(94, state().detailTarget.userId);
    }

    @Test
    public void detailRejectsInvalidTerminalBodiesOrAnotherOwnerAndRecord() {
        load(data(73, 1));
        List<ScreenState<RoutineData.Habit>> invalid = Arrays.asList(null, ScreenState.idle(), ScreenState.loading(), ScreenState.success(null),
            ScreenState.success(habit(106, 73, 1, "07:00")), ScreenState.success(habit(105, 74, 1, "07:00")));
        for (ScreenState<RoutineData.Habit> result : invalid) {
            viewModel.openDetails(105, LocalTime.of(7, 0));
            replyDetail(repository.details.size() - 1, result);
            assertEquals(ScreenState.Status.ERROR, state().detail.status);
            assertNull(state().details);
            assertFalse(state().detail.busy());
        }
    }

    @Test
    public void detailCallbacksRequireTheMainDispatcherAndAreIgnoredAfterLogoutOrClear() {
        load(data(73, 1));
        viewModel.openDetails(105, LocalTime.of(7, 0));
        repository.details.get(0).accept(ScreenState.success(habit(105, 73, 1, "07:00")));
        assertEquals(ScreenState.Status.LOADING, state().detail.status);
        authenticated = false;
        session.setValue(false);
        drain();
        assertNull(state().detailTarget);
        authenticated = true;
        session.setValue(true);
        viewModel.load();
        reply(1, ScreenState.success(data(73, 1)));
        viewModel.openDetails(105, LocalTime.of(7, 0));
        repository.details.get(1).accept(ScreenState.success(habit(105, 73, 1, "07:00")));
        RoutineUiState previous = state();
        store.clear();
        drain();
        assertSame(previous, state());
        repository.details.get(1).accept(ScreenState.success(habit(105, 73, 1, "07:00")));
        assertTrue(main.isEmpty());
        assertFalse(viewModel.openDetails(105, LocalTime.of(7, 0)));
    }

    @Test
    public void providerRetentionKeepsDetailContextAndDoesNotRepeatItsRequest() {
        load(data(73, 1));
        viewModel.openDetails(105, LocalTime.of(7, 0));
        RoutineUiState.DetailTarget target = state().detailTarget;
        RoutineViewModel retained = provider.get(RoutineViewModel.class);
        assertSame(viewModel, retained);
        assertSame(target, retained.state().getValue().detailTarget);
        retained.openDetails(105, LocalTime.of(7, 0));
        assertEquals(1, repository.details.size());
        replyDetail(0, ScreenState.success(habit(105, 73, 1, "07:00")));
        retained.closeDetails();
        assertEquals(INITIAL, retained.state().getValue().selectedDate);
        assertEquals(1, repository.callbacks.size());
    }

    @Test
    public void closingDuringDetailLoadingPublicationPreventsStartingTheInvalidatedGet() {
        load(data(73, 1));
        Observer<RoutineUiState> close = value -> {
            if (value.detail.busy()) viewModel.closeDetails();
        };
        viewModel.state().observeForever(close);
        try {
            assertFalse(viewModel.openDetails(105, LocalTime.of(7, 0)));
            assertTrue(repository.details.isEmpty());
            assertNull(state().detailTarget);
        } finally {
            viewModel.state().removeObserver(close);
        }
    }

    @Test
    public void detailAuthorizationFailureRemovesPreviouslyLoadedAccountData() {
        load(data(73, 1));
        viewModel.openDetails(105, LocalTime.of(7, 0));
        replyDetail(0, ScreenState.error("Acesso indisponível.", 403, 0));
        assertEquals(403, state().detail.code);
        assertEquals(403, state().reading.code);
        assertNull(state().data);
        assertNull(state().details);
    }

    @Test
    public void creatingAFormDoesNotStartReadsOrExposeAPersistedResult() {
        assertTrue(viewModel.beginCreateForm());
        assertEquals(RoutineFormState.Mode.CREATE, state().form.mode);
        assertNull(state().form.id);
        assertFalse(state().form.dirty);
        assertEquals(INITIAL, state().selectedDate);
        assertTrue(repository.callbacks.isEmpty());
        assertTrue(repository.details.isEmpty());
    }

    @Test
    public void invalidDraftTextAndErrorsArePreservedAndRevalidatedOnCorrection() {
        viewModel.beginCreateForm();
        assertFalse(viewModel.validateForm());
        assertEquals(RoutineFormValidator.Error.REQUIRED, state().form.errors.get(RoutineFormState.TYPE));
        fillForm();
        viewModel.setFormTime(DayOfWeek.MONDAY, 0, "7:");
        assertEquals("7:", state().form.days.get(DayOfWeek.MONDAY).times.get(0));
        assertEquals(RoutineFormValidator.Error.INVALID_TIME, state().form.errors.get(RoutineFormState.timeKey(DayOfWeek.MONDAY, 0)));
        viewModel.setFormTime(DayOfWeek.MONDAY, 0, "07:00");
        assertTrue(viewModel.validateForm());
        assertTrue(state().form.valid());
        assertTrue(repository.callbacks.isEmpty());
    }

    @Test
    public void formReductionAndDayDeselectionKeepExcessInputInTheViewModel() {
        viewModel.beginCreateForm();
        fillForm();
        viewModel.setFormQuantity(DayOfWeek.MONDAY, "2");
        viewModel.setFormTime(DayOfWeek.MONDAY, 1, "20:00");
        viewModel.setFormQuantity(DayOfWeek.MONDAY, "1");
        viewModel.selectFormDay(DayOfWeek.MONDAY, false);
        viewModel.selectFormDay(DayOfWeek.MONDAY, true);
        viewModel.setFormQuantity(DayOfWeek.MONDAY, "2");
        assertEquals(Arrays.asList("07:00", "20:00"), state().form.days.get(DayOfWeek.MONDAY).times);
    }

    @Test
    public void editFormRequiresAVerifiedDetailAndSeedsItsStableIdentityOnlyOnce() {
        assertFalse(viewModel.beginEditForm());
        load(data(73, 1));
        viewModel.openDetails(105, LocalTime.of(7, 0));
        assertFalse(viewModel.beginEditForm());
        replyDetail(0, ScreenState.success(habit(105, 73, 1, "07:00", "20:00")));
        assertTrue(viewModel.beginEditForm());
        assertEquals(RoutineFormState.Mode.EDIT, state().form.mode);
        assertEquals(Integer.valueOf(105), state().form.id);
        assertEquals(Integer.valueOf(73), state().form.userId);
        assertEquals("2", state().form.frequency);
        assertEquals(Arrays.asList("07:00", "20:00"), state().form.days.get(DayOfWeek.SATURDAY).times);
        assertNull(state().detailTarget);
        viewModel.setFormFrequency("003");
        replyDetail(0, ScreenState.success(habit(105, 73, 4, "08:00")));
        assertEquals("003", state().form.frequency);
        assertEquals(Integer.valueOf(1), state().form.habitId);
        assertEquals(1, repository.details.size());
    }

    @Test
    public void unrelatedRefreshAndNetworkErrorsDoNotOverwriteTheDraft() {
        load(data(73, 1));
        viewModel.beginCreateForm();
        fillForm();
        viewModel.setFormFrequency("0005");
        RoutineFormState draft = state().form;
        viewModel.refresh();
        reply(1, ScreenState.error("Sem conexão."));
        assertSame(draft, state().form);
        viewModel.retry();
        reply(2, ScreenState.success(data(73, 4)));
        assertSame(draft, state().form);
        viewModel.selectDate(LocalDate.of(2028, 2, 29));
        assertSame(draft, state().form);
    }

    @Test
    public void closingDirtyFormRequiresExplicitDiscardAndCancelKeepsEveryField() {
        viewModel.beginCreateForm();
        fillForm();
        long token = state().form.token;
        viewModel.requestFormClose(token);
        assertTrue(state().form.confirmDiscard);
        viewModel.cancelFormDiscard(token);
        assertFalse(state().form.confirmDiscard);
        assertEquals("07:00", state().form.days.get(DayOfWeek.MONDAY).times.get(0));
        viewModel.requestFormClose(token);
        viewModel.discardForm(token);
        assertNull(state().form);
        assertEquals(INITIAL, state().selectedDate);
    }

    @Test
    public void untouchedFormClosesWithoutConfirmationEvenAfterValidation() {
        viewModel.beginCreateForm();
        viewModel.validateForm();
        assertFalse(state().form.dirty);
        viewModel.requestFormClose(state().form.token);
        assertNull(state().form);
    }

    @Test
    public void anotherEditorCannotReplaceAnActiveDraftAndOldConfirmationCannotCloseANewOne() {
        viewModel.beginCreateForm();
        long previous = state().form.token;
        fillForm();
        assertFalse(viewModel.beginCreateForm());
        assertFalse(viewModel.beginEditForm());
        viewModel.discardForm(previous);
        assertTrue(viewModel.beginCreateForm());
        long current = state().form.token;
        assertTrue(current > previous);
        viewModel.requestFormClose(previous);
        viewModel.cancelFormDiscard(previous);
        viewModel.discardForm(previous);
        assertEquals(current, state().form.token);
        assertFalse(state().form.confirmDiscard);
    }

    @Test
    public void providerRetentionPreservesDraftErrorsAndTheDiscardConfirmation() {
        viewModel.beginCreateForm();
        fillForm();
        viewModel.setFormFrequency("invalid");
        viewModel.validateForm();
        viewModel.requestFormClose(state().form.token);
        RoutineFormState draft = state().form;
        RoutineViewModel retained = provider.get(RoutineViewModel.class);
        assertSame(viewModel, retained);
        assertSame(draft, retained.state().getValue().form);
        assertTrue(draft.confirmDiscard);
        assertEquals("invalid", draft.frequency);
        assertEquals(RoutineFormValidator.Error.INVALID_NUMBER, draft.errors.get(RoutineFormState.FREQUENCY));
    }

    @Test
    public void sessionChangesAndAuthorizationFailureClearTheDraftWithoutCrossAccountReuse() {
        viewModel.beginCreateForm();
        fillForm();
        session.setValue(true);
        assertNull(state().form);
        viewModel.beginCreateForm();
        viewModel.load();
        reply(0, ScreenState.error("Sem acesso.", 403, 0));
        assertNull(state().form);
        authenticated = false;
        assertFalse(viewModel.beginCreateForm());
    }

    @Test
    public void clearingTheViewModelPreventsNewFormsAndEdits() {
        viewModel.beginCreateForm();
        fillForm();
        RoutineUiState before = state();
        store.clear();
        assertFalse(viewModel.beginCreateForm());
        assertFalse(viewModel.beginEditForm());
        viewModel.setFormFrequency("9");
        viewModel.validateForm();
        viewModel.discardForm(before.form.token);
        assertSame(before, state());
        assertTrue(repository.callbacks.isEmpty());
    }

    private void fillForm() {
        viewModel.setFormHabit(1);
        viewModel.setFormFrequency("1");
        viewModel.selectFormDay(DayOfWeek.MONDAY, true);
        viewModel.setFormQuantity(DayOfWeek.MONDAY, "1");
        viewModel.setFormTime(DayOfWeek.MONDAY, 0, "07:00");
    }

    @Test
    public void authorizationFailureEndsTheDraftWithoutReloadOrResend() {
        for (int code : new int[]{401, 403}) {
            viewModel.beginCreateForm();
            fillForm();
            viewModel.saveForm();
            repository.posts.get(repository.posts.size() - 1).accept(ScreenState.error("Autorização", code, 0));
            drain();
            assertNull(state().form);
            assertEquals(code, state().reading.code);
            assertTrue(repository.callbacks.isEmpty());
        }
    }

    @Test
    public void malformedMutationResultIsUncertainAndRefreshDoesNotAuthorizeResend() {
        viewModel.beginCreateForm();
        fillForm();
        viewModel.saveForm();
        repository.posts.get(0).accept(null);
        drain();
        assertTrue(state().uncertainCreation);
        viewModel.refresh();
        reply(0, ScreenState.success(data(73, 1)));
        viewModel.requestResend();
        assertFalse(state().confirmResend);
        viewModel.checkCreation();
        reply(1, ScreenState.success(data(73, 1)));
        viewModel.refresh();
        reply(2, ScreenState.error("Sem rede"));
        assertFalse(state().checkedCreation);
        viewModel.requestResend();
        assertFalse(state().confirmResend);
    }

    @Test
    public void saveValidatesAndBlocksDoubleClickEditingAndDiscardDuringPost() {
        viewModel.beginCreateForm();
        viewModel.saveForm();
        assertTrue(repository.posts.isEmpty());
        fillForm();
        viewModel.saveForm();
        RoutineFormState submitted = state().form;
        viewModel.saveForm();
        viewModel.setFormFrequency("9");
        viewModel.discardForm(submitted.token);
        viewModel.requestFormClose(submitted.token);
        assertSame(submitted, state().form);
        assertEquals(1, repository.posts.size());
        assertTrue(state().creation.busy());
    }

    @Test
    public void confirmedSaveClosesEditorInvalidatesOldGetAndRetriesOnlyReload() {
        viewModel.load();
        viewModel.beginCreateForm();
        fillForm();
        viewModel.saveForm();
        repository.posts.get(0).accept(ScreenState.success(habit(105, 73, 1, "07:00")));
        drain();
        assertNull(state().form);
        assertTrue(state().savedAwaitingReload);
        assertEquals(2, repository.callbacks.size());
        reply(0, ScreenState.success(data(74, 2)));
        assertTrue(state().reading.busy());
        reply(1, ScreenState.error("Sem rede"));
        assertTrue(state().savedAwaitingReload);
        viewModel.saveForm();
        viewModel.retry();
        reply(2, ScreenState.success(data(73, 1)));
        assertFalse(state().savedAwaitingReload);
        assertEquals(INITIAL, state().selectedDate);
        assertEquals(1, repository.posts.size());
        repository.posts.get(0).accept(ScreenState.success(habit(105, 73, 1, "07:00")));
        drain();
        assertEquals(3, repository.callbacks.size());
    }

    @Test
    public void uncertainSaveNeedsSuccessfulExplicitCheckAndConfirmationBeforeResend() {
        viewModel.beginCreateForm();
        fillForm();
        viewModel.saveForm();
        repository.posts.get(0).accept(ScreenState.error("Incerto", -7, 0));
        drain();
        RoutineFormState draft = state().form;
        viewModel.saveForm();
        viewModel.requestResend();
        assertFalse(state().confirmResend);
        viewModel.checkCreation();
        reply(0, ScreenState.error("Sem rede"));
        viewModel.confirmResend();
        assertEquals(1, repository.posts.size());
        viewModel.checkCreation();
        reply(1, ScreenState.success(data(73, 1)));
        assertTrue(state().checkedCreation);
        assertSame(draft, state().form);
        viewModel.requestResend();
        assertTrue(state().confirmResend);
        assertTrue(provider.get(RoutineViewModel.class).state().getValue().confirmResend);
        viewModel.cancelResend();
        assertEquals(1, repository.posts.size());
        viewModel.requestResend();
        viewModel.confirmResend();
        viewModel.confirmResend();
        assertEquals(2, repository.posts.size());
    }

    @Test
    public void confirmedErrorsPreserveDraftAndRateLimitBlocksRetryUntilItsDeadline() {
        viewModel.beginCreateForm();
        fillForm();
        for (int code : new int[]{400, 409, 429}) {
            viewModel.saveForm();
            RoutineFormState draft = state().form;
            repository.posts.get(repository.posts.size() - 1).accept(ScreenState.error("Confira", code, code == 429 ? clock.millis() + 60000 : 0));
            drain();
            assertSame(draft, state().form);
            assertFalse(state().uncertainCreation);
        }
        int count = repository.posts.size();
        viewModel.saveForm();
        assertEquals(count, repository.posts.size());
        clock.now = clock.now.plusSeconds(61);
        viewModel.saveForm();
        assertEquals(count + 1, repository.posts.size());
    }

    @Test
    public void logoutAndClearRejectLateCreationReplies() {
        viewModel.beginCreateForm();
        fillForm();
        viewModel.saveForm();
        session.setValue(false);
        repository.posts.get(0).accept(ScreenState.success(habit(105, 73, 1, "07:00")));
        drain();
        assertNull(state().form);
        assertTrue(repository.callbacks.isEmpty());
        session.setValue(true);
        viewModel.beginCreateForm();
        fillForm();
        viewModel.saveForm();
        store.clear();
        repository.posts.get(1).accept(ScreenState.success(habit(105, 73, 1, "07:00")));
        assertTrue(main.isEmpty());
    }

    private RoutineUiState state() {
        return viewModel.state().getValue();
    }

    private RoutineData data(int user, int type) {
        return RoutineData.from(Collections.singletonList(response(105, user, type, "07:00")), clock);
    }

    private RoutineData.Habit habit(int id, int user, int type, String... times) {
        return RoutineData.from(Collections.singletonList(response(id, user, type, times)), clock).habits.get(0);
    }

    private UserHabitResponse response(int id, int user, int type, String... times) {
        UserHabitResponse value = new UserHabitResponse();
        value.id = id;
        value.userId = user;
        value.habitId = type;
        value.frequency = times.length;
        value.daysOfWeek = Collections.singletonList(6);
        UserHabitScheduleResponse schedule = new UserHabitScheduleResponse();
        schedule.dayOfWeek = 6;
        schedule.startTimes = Arrays.asList(times);
        value.schedule = Collections.singletonList(schedule);
        return value;
    }

    private void load(RoutineData data) {
        viewModel.load();
        reply(repository.callbacks.size() - 1, ScreenState.success(data));
    }

    private void reply(int request, ScreenState<RoutineData> result) {
        repository.callbacks.get(request).accept(result);
        drain();
    }

    private void replyDetail(int request, ScreenState<RoutineData.Habit> result) {
        repository.details.get(request).accept(result);
        drain();
    }

    private void drain() {
        while (!main.isEmpty()) main.remove(0).run();
    }
}
