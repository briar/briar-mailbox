# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontobfuscate
-keepattributes SourceFile, LineNumberTable, *Annotation*, Signature, InnerClasses, EnclosingMethod

# Keep our own classes, we remove unused ones ourselves
-keep class org.briarproject.** { *; }

# Keep the H2 classes that are loaded via reflection
-keep class org.h2.Driver { *; }
-keep class org.h2.engine.Engine { *; }
-keep class org.h2.store.fs.** { *; }

# Keep logging
-keep public class org.slf4j.** { *; }
-keep public class ch.qos.logback.** { *; }

# Keep Netty classes that are loaded via reflection
-keep class io.netty.util.ReferenceCountUtil { *; }
-keep class io.netty.buffer.WrappedByteBuf { *; }

# Keep classes needed for instrumentation tests (here because testProguardFiles doesn't work)
-keep class kotlin.LazyKt
-keep class com.fasterxml.jackson.core.type.TypeReference
-keep class com.fasterxml.jackson.databind.ObjectMapper { *; }

# Don't warn about unused dependencies of H2 classes
-dontwarn org.h2.**
-dontnote org.h2.**

-dontwarn com.fasterxml.jackson.databind.ext.Java7SupportImpl
-dontwarn io.netty.internal.tcnative.*
-dontwarn java.lang.management.*
-dontwarn org.apache.log4j.*
-dontwarn org.apache.logging.log4j.**
-dontwarn org.conscrypt.*
-dontwarn org.eclipse.jetty.npn.*
-dontwarn org.jetbrains.annotations.*
-dontwarn reactor.blockhound.integration.BlockHoundIntegration
-dontwarn javax.mail.**
