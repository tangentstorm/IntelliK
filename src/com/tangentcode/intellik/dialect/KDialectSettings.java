package com.tangentcode.intellik.dialect;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/** Project-level choice of K dialect. Defaults to K3. */
@Service(Service.Level.PROJECT)
@State(name = "IntelliKDialectSettings", storages = @Storage("intellik.xml"))
public final class KDialectSettings implements PersistentStateComponent<KDialectSettings.State> {

    public static class State {
        /** {@link KDialect#getId()} of the project's dialect. */
        public String dialect = KDialect.DEFAULT.getId();
    }

    private State state = new State();

    @NotNull
    public static KDialectSettings getInstance(@NotNull Project project) {
        return project.getService(KDialectSettings.class);
    }

    @NotNull
    public KDialect getDialect() {
        return KDialect.fromId(state.dialect);
    }

    public void setDialect(@NotNull KDialect dialect) {
        state.dialect = dialect.getId();
    }

    @NotNull
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State loaded) {
        state = loaded;
    }
}
