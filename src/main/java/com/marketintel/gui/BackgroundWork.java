package com.marketintel.gui;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingWorker;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Runs network and model work off the Swing event thread, then applies
 * the result back on that thread. Controls passed in are disabled until
 * the task finishes so a second click cannot start a duplicate request.
 */
public final class BackgroundWork {

    private BackgroundWork() {
    }

    public static void submit(
            JLabel statusLabel,
            List<JComponent> controls,
            Runnable restoreControls,
            String busyMessage,
            Callable<String> task,
            Consumer<String> onText) {

        for (JComponent control : controls) {
            control.setEnabled(false);
        }

        statusLabel.setText(busyMessage);

        new SwingWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                restoreControls.run();

                try {
                    String text = get();
                statusLabel.setText("READY");
                    onText.accept(text);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("INTERRUPTED");
                    onText.accept("The request was interrupted.");
                } catch (ExecutionException execution) {
                    Throwable cause = execution.getCause() == null
                            ? execution
                            : execution.getCause();
                    statusLabel.setText("REQUEST FAILED");
                    onText.accept("Error: " + safeMessage(cause));
                }
            }
        }.execute();
    }

    private static String safeMessage(Throwable cause) {
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return "The request could not be completed.";
        }
        return message;
    }
}
