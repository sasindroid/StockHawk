package com.sam_chordas.android.stockhawk;

import android.app.Application;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import okhttp3.OkHttpClient;

/**
 * Created by sasikumarlakshmanan on 03/04/16.
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Stetho.initializeWithDefaults(this);

//        Stetho.initialize(Stetho.newInitializerBuilder(this)
//                .enableDumpapp(new DumperPluginsProvider() {
//                    @Override
//                    public Iterable<DumperPlugin> get() {
//                        return new Stetho.DefaultDumperPluginsBuilder(getBaseContext())
//                                .provide(new HelloWorldDumperPlugin())
//                                .finish();
//                    }
//                })
//                .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(getBaseContext()))
//                .build());

        new OkHttpClient.Builder()
                .addNetworkInterceptor(new StethoInterceptor())
                .build();


    }
}
