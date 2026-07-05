package ru.exeswi.exest.networking;

/** Which fake fullscreen UI the client should briefly show. */
public enum FakeUi {
    CRASH,
    LOADING,
    TITLE;

    private static final FakeUi[] VALUES = values();

    public static FakeUi byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : LOADING;
    }
}
