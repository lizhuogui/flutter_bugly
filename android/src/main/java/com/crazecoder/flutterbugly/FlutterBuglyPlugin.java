package com.crazecoder.flutterbugly;

import android.content.Context;
import android.text.TextUtils;

import com.crazecoder.flutterbugly.bean.BuglyInitResultInfo;
import com.crazecoder.flutterbugly.utils.JsonUtil;
import com.crazecoder.flutterbugly.utils.MapUtil;
import com.tencent.bugly.crashreport.CrashReport;

import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterBuglyPlugin
 */
public class FlutterBuglyPlugin implements MethodCallHandler, FlutterPlugin {
    private Context applicationContext;
    private MethodChannel methodChannel;
    private Result result;
    private boolean isResultSubmitted = false;

    public static void registerWith(Registrar registrar) {
        final FlutterBuglyPlugin instance = new FlutterBuglyPlugin();
        instance.onAttachedToEngine(registrar.context(), registrar.messenger());
    }

    @Override
    public void onAttachedToEngine(FlutterPluginBinding binding) {
        onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
    }

    private void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger) {
        this.applicationContext = applicationContext;
        methodChannel = new MethodChannel(messenger, "crazecoder/flutter_bugly");
        methodChannel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(FlutterPluginBinding binding) {
        applicationContext = null;
        methodChannel.setMethodCallHandler(null);
        methodChannel = null;
    }
    
    @Override
    public void onMethodCall(final MethodCall call, final Result result) {
        isResultSubmitted = false;
        this.result = result;
        if (call.method.equals("initBugly")) {
            if (call.hasArgument("appId")) {
                /* 在application中初始化时设置监听，监听策略的收取 */
                String appId = call.argument("appId").toString();
                CrashReport.initCrashReport(applicationContext, appId, BuildConfig.DEBUG);
                if (call.hasArgument("channel")) {
                    String channel = call.argument("channel");
                    if (!TextUtils.isEmpty(channel))
                        CrashReport.setAppChannel(applicationContext, channel);
                }
                result(getResultBean(true, appId, "Bugly 初始化成功"));
            } else {
                result(getResultBean(false, null, "Bugly appId不能为空"));
            }
        } else if (call.method.equals("setAppChannel")) {
            if (call.hasArgument("channel")) {
                String channel = call.argument("channel");
                CrashReport.setAppChannel(applicationContext, channel);
            }
            result(null);
        } else if (call.method.equals("setUserId")) {
            if (call.hasArgument("userId")) {
                String userId = call.argument("userId");
                CrashReport.setUserId(applicationContext, userId);
            }
            result(null);
        } else if (call.method.equals("setUserTag")) {
            if (call.hasArgument("userTag")) {
                Integer userTag = call.argument("userTag");
                if (userTag != null)
                    CrashReport.setUserSceneTag(applicationContext, userTag);
            }
            result(null);
        } else if (call.method.equals("putUserData")) {
            if (call.hasArgument("key") && call.hasArgument("value")) {
                String userDataKey = call.argument("key");
                String userDataValue = call.argument("value");
                CrashReport.putUserData(applicationContext, userDataKey, userDataValue);
            }
            result(null);
        } else if (call.method.equals("postCatchedException")) {
            postException(call);
            result(null);
        } else {
            result.notImplemented();
            isResultSubmitted = true;
        }

    }

    private void postException(MethodCall call) {
        String message = "";
        String detail = null;
        Map<String, String> map = null;
        if (call.hasArgument("crash_message")) {
            message = call.argument("crash_message");
        }
        if (call.hasArgument("crash_detail")) {
            detail = call.argument("crash_detail");
        }
        if (TextUtils.isEmpty(detail)) return;
        if (call.hasArgument("crash_data")) {
            map = call.argument("crash_data");
        }
        CrashReport.postException(8, "Flutter Exception", message, detail, map);
    }

    private void result(Object object) {
        if (result != null && !isResultSubmitted) {
            if (object == null) {
                result.success(null);
            } else {
                result.success(JsonUtil.toJson(MapUtil.deepToMap(object)));
            }
            isResultSubmitted = true;
        }
    }

    private BuglyInitResultInfo getResultBean(boolean isSuccess, String appId, String msg) {
        BuglyInitResultInfo bean = new BuglyInitResultInfo();
        bean.setSuccess(isSuccess);
        bean.setAppId(appId);
        bean.setMessage(msg);
        return bean;
    }
}