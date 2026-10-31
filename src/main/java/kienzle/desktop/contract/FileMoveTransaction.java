package kienzle.desktop.contract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Collects file moves and runs them together; if one fails, the moves already done are undone. */
final class FileMoveTransaction {
    private record Move(Path from, Path to) {
    }

    private final List<Move> planned = new ArrayList<>();
    private final Deque<Move> done = new ArrayDeque<>();

    void add(Path from, Path to) {
        planned.add(new Move(from, to));
    }

    void commit() throws IOException {
        try {
            for (Move move : planned) {
                Files.createDirectories(move.to().getParent());
                Files.move(move.from(), move.to(), StandardCopyOption.ATOMIC_MOVE);
                done.push(move);
            }
        } catch (IOException e) {
            rollback(e);
            throw e;
        }
    }

    /** Undoes all completed moves; problems while undoing are attached to {@code failure}. */
    void rollback(IOException failure) {
        while (!done.isEmpty()) {
            Move move = done.pop();
            try {
                Files.move(move.to(), move.from(), StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
        }
    }
}
