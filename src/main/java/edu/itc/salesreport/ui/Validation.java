package edu.itc.salesreport.ui;

public sealed interface Validation<T> permits Validation.Valid, Validation.Invalid {
    record Valid<T>(T value) implements Validation<T> {}
    record Invalid<T>(String message) implements Validation<T> {}
}