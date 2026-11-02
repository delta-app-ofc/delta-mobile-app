package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.MonthlyGoal;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.YearMonth;

public final class MonthlyGoalStorage {
    public interface Store {
        String read(String key);
        boolean write(String key, String value);
    }

    private final Store store;

    public MonthlyGoalStorage(Store store) { this.store = store; }

    public BigDecimal read(int userId, int propertyId, YearMonth month) {
        String value = store.read(MonthlyGoal.key(userId, propertyId, month));
        return value == null ? null : MonthlyGoal.parse(value);
    }

    public BigDecimal save(int userId, int propertyId, YearMonth month, String input) throws IOException {
        BigDecimal amount = MonthlyGoal.parse(input);
        String key = MonthlyGoal.key(userId, propertyId, month);
        if (!store.write(key, amount.toPlainString())) throw new IOException("Não foi possível salvar a meta neste aparelho.");
        BigDecimal confirmed = read(userId, propertyId, month);
        if (confirmed == null || amount.compareTo(confirmed) != 0) throw new IOException("Não foi possível confirmar a meta salva.");
        return confirmed;
    }
}
