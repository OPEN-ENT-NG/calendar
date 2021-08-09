package net.atos.entng.calendar.helpers;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.impl.CompositeFutureImpl;

import java.util.List;

public class FutureHelper {

    private FutureHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static <T> CompositeFuture all(List<Future<T>> futures) {
        return CompositeFutureImpl.all(futures.toArray(new Future[0]));
    }

    public static <T> CompositeFuture join(List<Future<T>> futures) {
        return CompositeFutureImpl.join(futures.toArray(new Future[0]));
    }
}
