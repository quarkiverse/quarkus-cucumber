package io.quarkiverse.cucumber.test;

public class Factory {

    private static boolean justeHello = false;

    public static void setJusteHello(boolean justeHello) {
        Factory.justeHello = justeHello;
    }

    public static String hello() {
        return justeHello ? "hello" : "hello from factory";
    }
}
