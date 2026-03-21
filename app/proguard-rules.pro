# WebView APP ProGuard 规则
# 保留 WebView JavaScript 接口，防止混淆后 JS 无法调用
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 保留 WebView 相关类
-keepclassmembers class android.webkit.** { *; }

# 保留 AppCompat 和 Material 组件
-keep class androidx.appcompat.widget.** { *; }
-keep class com.google.android.material.** { *; }

# 防止移除 Activity 类
-keep class cn.kikirepository.tk.** { *; }

# 抑制不影响功能的警告
-dontwarn okhttp3.**
-dontwarn okio.**
