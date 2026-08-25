package com.fps.calculadora;

import android.app.Activity;

import androidx.core.splashscreen.SplashScreen;

/**
 * Ponte em Java para o androidx.core.splashscreen: o compilador Kotlin deste
 * projeto não resolve o método estático gerado pelo @JvmStatic da lib
 * (`SplashScreen.installSplashScreen`), embora o bytecode o exponha
 * normalmente e o Java o enxergue sem problema.
 */
final class SplashScreenBridge {
    private SplashScreenBridge() {
    }

    static void install(Activity activity) {
        SplashScreen.installSplashScreen(activity);
    }
}
