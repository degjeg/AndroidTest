# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/danger/Android/sdk/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}


-keep class keep.Test
-keep class keep.cls.membernames.Test
#-keepclassmembers keep.cls.members*{*;}
-keepclasseswithmembernames class keep.cls.withmembers.Test{*;}
#-keepclasseswithmembers keep.cls.withmembername**{*;}
#-keep keepnames**{*;}
