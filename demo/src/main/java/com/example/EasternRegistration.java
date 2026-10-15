package com.example;

/**
 * Application entry point for the Eastern Registration System.
 */
public class EasternRegistration {

    /**
     * Prevents construction of the application entry-point class.
     */
    private EasternRegistration() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Starts the interactive registration menu.
     *
     * @param args command-line arguments, which are not used
     * @throws Exception if required schedule data cannot be retrieved
     */
    public static void main(String[] args) throws Exception {
        MainMenu menu = new MainMenu();
        menu.open();
    }
}