# App-specific R8 rules belong here when a library or reflection-based feature requires them.

# Kakao SDK uses Retrofit 2.9, whose consumer rules predate R8 full mode.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault

-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Kakao SDK resolves enum constants by their original field names.
-keepclassmembers enum com.kakao.sdk.** { *; }

# Kakao Maps loads its internal bridge classes from native code.
-keep class com.kakao.vectormap.** { *; }
