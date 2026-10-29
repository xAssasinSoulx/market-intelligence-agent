package com.marketintel;

import com.marketintel.bootstrap.ApplicationContext;
import com.marketintel.bootstrap.LaunchMode;
import com.marketintel.gui.MarketIntelFrame;

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
            case GUI -> MarketIntelFrame.launch(context);
        }
    }
}
