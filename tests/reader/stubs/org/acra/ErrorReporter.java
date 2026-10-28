package org.acra;
public class ErrorReporter { public void handleException(Throwable error) { throw new AssertionError(error); } }
