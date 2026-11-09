package com.aphs.classroomuprising.ui;

/**
 * The result of a request that returns data but may hit a pending or rejected rule.
 * Exactly one of {@code value} and {@code failure} is present.
 */
public record Fetch<T>(T value, Outcome failure) {
    public Fetch {
        if ((value == null) == (failure == null)) throw new IllegalArgumentException("Exactly one of value/failure.");
        if (failure != null && failure.ok()) throw new IllegalArgumentException("A failure cannot be Done.");
    }
    static <T> Fetch<T> of(T value) { return new Fetch<>(value, null); }
    static <T> Fetch<T> failed(Outcome failure) { return new Fetch<>(null, failure); }
    public boolean ok() { return failure == null; }
}
