package dev.lednevok1.devicespoof;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;
import android.content.Context;
import android.widget.Toast;
import android.app.Activity;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.lang.Object;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.lang.reflect.Field;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

@SuppressLint("DiscouragedPrivateApi")
@SuppressWarnings("ConstantConditions")
public class DeviceSpoof implements IXposedHookLoadPackage {
    private static final String TAG = DeviceSpoof.class.getSimpleName();
    
    private static void setPropValue(String key, Object value) {
        try {
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            XposedBridge.log("Failed to set prop: " + key + "\n" + Log.getStackTraceString(e));
        }
    }

    private static void createConfig() {
        try { 
            new File("/storage/emulated/0/documents/").mkdir();
            new File("/storage/emulated/0/documents/device.txt").createNewFile();
        
            BufferedWriter buffWriter = new BufferedWriter(new java.io.FileWriter("/storage/emulated/0/documents/device.txt"));
            buffWriter.write("XIAOMI\nM2004J19C\n\n// line 1 - manufacturer; line 2 - model");              buffWriter.flush(); buffWriter.close();
            XposedBridge.log("[DeviceSpoof] Created config (/storage/emulated/0/documents/device.txt)");
        } catch(java.io.IOException e) { XposedBridge.log(e); }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (!new File("/storage/emulated/0/documents/device.txt").exists()) { createConfig(); }
        try { 
            XposedBridge.log("[DeviceSpoof] Spoofing for: " + loadPackageParam.packageName);
            
            BufferedReader buffReader = new BufferedReader(new FileReader("/storage/emulated/0/documents/device.txt"));
            String buffer = "";
            
            buffer = buffReader.readLine(); 
            setPropValue("MANUFACTURER", buffer.isBlank() ? "XIAOMI" : buffer);
            
            buffer = buffReader.readLine();
            setPropValue("MODEL", buffer.isBlank() ? "M2004J19C" : buffer);
            buffReader.close();
            
            // shitcode 
            XposedHelpers.findAndHookMethod("android.content.ContextWrapper", loadPackageParam.classLoader, "attachBaseContext", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.thisObject instanceof Activity) {
                        Context appCtx = ((Activity) param.thisObject).getApplicationContext();
                        BufferedReader buffReader = new BufferedReader(new FileReader("/storage/emulated/0/documents/device.txt"));
                        
                        if (buffReader.readLine().isBlank() || buffReader.readLine().isBlank()) { 
                            Toast.makeText(appCtx, "[DeviceSpoof] Invalid device.txt, using defaults", Toast.LENGTH_LONG).show();
                        }
                        
                        buffReader.close();
                    }
                }
            });
        } catch(java.lang.Exception e) { XposedBridge.log(e); }
    }
}