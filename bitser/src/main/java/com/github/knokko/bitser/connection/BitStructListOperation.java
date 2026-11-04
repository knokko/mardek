package com.github.knokko.bitser.connection;

public record BitStructListOperation<T>(BitStructListConnectionView<T> view, int operationID) {}
