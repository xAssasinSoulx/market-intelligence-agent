package com.marketintel;

import com.marketintel.bootstrap.ApplicationContext;
import com.marketintel.bootstrap.LaunchMode;

public class Main {

    public static void main(String[] args) {

        final LaunchMode mode;

        try {
            mode = LaunchMode.parse(args);
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            System.exit(2);
            return;
        }

        ApplicationContext context =
                ApplicationContext.createDefault();

        switch (mode) {
            case CLI -> context.createTerminalInterface().start();
            case GUI -> System.err.println(
                    "The graphical interface is not available in this build. "
                            + "Start the command-line interface with --cli."
            );
        }
    }
}
