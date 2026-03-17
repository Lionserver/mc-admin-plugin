package com.zehelper.adminplugin.api;

import com.zehelper.adminplugin.AdminPlugin;

/**
 * AdminPlugin 외부 API 클래스
 * 다른 플러그인에서 AdminPlugin 기능에 접근할 때 사용한다
 */
public class AdminPluginAPI {

    /** 플러그인이 활성화되어 있는지 확인한다 */
    public static boolean isEnabled() {
        return AdminPlugin.getInstance() != null && AdminPlugin.getInstance().isEnabled();
    }

    /** 플러그인 인스턴스를 반환한다 */
    public static AdminPlugin getPlugin() {
        return AdminPlugin.getInstance();
    }
}
