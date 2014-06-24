import android.Keys._

android.Plugin.androidBuild

name := "TaskManager"

version := "1.0"

scalaVersion := "2.11.1"

platformTarget in Android := "android-19"

targetSdkVersion in Android := 19

minSdkVersion in Android := 14

run <<= run in Android

install <<= install in Android

apkbuildExcludes in Android ++= Seq(
  "reference.conf",
  "META-INF/LICENSE.txt",
  "META-INF/mime.types",
  "LICENSE.txt",
  "META-INF/native/linux32/libleveldbjni.so",
  "META-INF/native/linux64/libleveldbjni.so",
  "META-INF/native/osx/libleveldbjni.jnilib",
  "META-INF/native/windows32/leveldbjni.dll",
  "META-INF/native/windows64/leveldbjni.dll",
  "org/fusesource/leveldbjni/version.txt"
)

dexMaxHeap in Android := "8192m"

resolvers ++= Seq(
  //Resolver.sonatypeRepo("releases"),
  //"jcenter" at "http://jcenter.bintray.com"
  "Stanch repo" at "http://dl.bintray.com/stanch/maven"
)

// add linter
scalacOptions in (Compile, compile) ++= Seq(
  "-P:wartremover:cp:" + (dependencyClasspath in Compile).value
    .files.map(_.toURL.toString)
    .find(_.contains("org.macroid/macroid_")).get,
  "-P:wartremover:traverser:macroid.warts.CheckUi"
)

libraryDependencies ++= Seq(
  "org.macroid" %% "macroid" % "2.0.0-M2",
  "com.typesafe.akka" %% "akka-actor" % "2.3.3",
  "org.scalaz" %% "scalaz-core" % "7.0.6",
  "com.typesafe.akka" % "akka-persistence-experimental_2.11" % "2.3.3",
  compilerPlugin("org.brianmckenna" %% "wartremover" % "0.10")
)

proguardScala in Android := true

// Generic ProGuard rules
proguardOptions in Android ++= Seq(
  "-printseeds",
  "-verbose",
  "-dontobfuscate",
  "-dontoptimize",
  "-dontpreverify",
  "-ignorewarnings",
  "-keep class scala.Dynamic",
  "-keep class scala.collection.JavaConversions",
  "-dontwarn scala.**",
  "-dontnote javax.xml.**",
  "-dontnote org.w3c.dom.**",
  "-dontnote org.xml.sax.**",
  "-dontnote scala.Enumeration",
  "-keep class macroid.**",
  "-keep class * implements org.xml.sax.EntityResolver", // From Proguard Examples page
  //"""|-keepclasseswithmembers public class * {
  //  |    public static void main(java.lang.String[]);
  //  |}""".stripMargin,
  """|-keepclassmembers class * {
    |  ** MODULE$;
    |}""".stripMargin,
  """|-keepclassmembernames class scala.concurrent.forkjoin.ForkJoinPool {
    |    long eventCount;
    |    int  workerCounts;
    |    int  runControl;
    |    scala.concurrent.forkjoin.ForkJoinPool$WaitQueueNode syncStack;
    |    scala.concurrent.forkjoin.ForkJoinPool$WaitQueueNode spareStack;
    |}""".stripMargin,
  """| -keepclassmembernames class scala.concurrent.forkjoin.ForkJoinWorkerThread {
    |  int base;
    |  int sp;
    |  int runState;
    |}""".stripMargin,
  """| -keepclassmembernames class scala.concurrent.forkjoin.ForkJoinTask {
    |  int status;
    |}""".stripMargin,
  """| -keepclassmembernames class scala.concurrent.forkjoin.LinkedTransferQueue {
    |  scala.concurrent.forkjoin.LinkedTransferQueue$PaddedAtomicReference head;
    |  scala.concurrent.forkjoin.LinkedTransferQueue$PaddedAtomicReference tail;
    |  scala.concurrent.forkjoin.LinkedTransferQueue$PaddedAtomicReference cleanMe;
    |}""".stripMargin
)

// ProGuard rules for akka
proguardOptions in Android ++= Seq(
  "-dontwarn sun.misc.Unsafe",
  "-dontwarn sun.reflect.Reflection",
  "-keep class akka.**",
  "-keep class scala.collection.immutable.StringLike { *; }",
  """|-keepclasseswithmembers class * {
    |    public <init>(java.lang.String, akka.actor.ActorSystem$Settings, akka.event.EventStream, akka.actor.Scheduler, akka.actor.DynamicAccess);
    |}""".stripMargin,
  //"""|-keepclasseswithmembernames class * {
  //  |    native <methods>;
  //  |}""".stripMargin,
  """|-keepclasseswithmembers class * {
    |    public <init>(android.content.Context, android.util.AttributeSet);
    |}""".stripMargin,
  """|-keepclasseswithmembers class * {
    |    public <init>(android.content.Context, android.util.AttributeSet, int);
    |}""".stripMargin,
  """|-keepclassmembers class * extends android.app.Activity {
    |    public void *(android.view.View);
    |}""".stripMargin,
  """|-keepclassmembers enum * {
    |    public static **[] values();
    |    public static ** valueOf(java.lang.String);
    |}""".stripMargin,
  """|-keep class * implements android.os.Parcelable {
    |    public static final android.os.Parcelable$Creator *;
    |}""".stripMargin,
  """|-keepclasseswithmembers class * {
    |    public <init>(com.typesafe.config.Config, akka.event.LoggingAdapter, java.util.concurrent.ThreadFactory);
    |}""".stripMargin,
  """|-keepclasseswithmembers class * {
    |    public <init>(java.lang.String, akka.actor.ActorSystem$Settings, akka.event.EventStream, akka.actor.DynamicAccess);
    |}""".stripMargin,
  "-keep class akka.actor.Actor$class { *; }",
  "-keep class akka.actor.LightArrayRevolverScheduler { *; }",
  "-keep class akka.actor.LocalActorRefProvider { *; }",
  "-keep class akka.actor.CreatorFunctionConsumer { *; }",
  "-keep class akka.actor.TypedCreatorFunctionConsumer { *; }",
  "-keep class akka.dispatch.BoundedDequeBasedMessageQueueSemantics { *; }",
  "-keep class akka.dispatch.UnboundedMessageQueueSemantics { *; }",
  "-keep class akka.dispatch.UnboundedDequeBasedMessageQueueSemantics { *; }",
  "-keep class akka.dispatch.DequeBasedMessageQueueSemantics { *; }",
  "-keep class akka.dispatch.MultipleConsumerSemantics { *; }",
  "-keep class akka.actor.LocalActorRefProvider$Guardian { *; }",
  "-keep class akka.actor.LocalActorRefProvider$SystemGuardian { *; }",
  "-keep class akka.dispatch.UnboundedMailbox { *; }",
  "-keep class akka.actor.DefaultSupervisorStrategy { *; }",
  "-keep class akka.event.Logging$LogExt { *; }"
)

// ProGuard rules for akka-persistence
proguardOptions in Android ++= Seq(
  "-keep class akka.persistence.**",
  """|-keepclasseswithmembers class * {
    |    public <init>(akka.actor.ActorRef, akka.persistence.PersistenceSettings);
    |}""".stripMargin,
  "-keep class akka.persistence.DeliveredByChannelBatching { *;}",
  "-keep class akka.actor.LocalActorRefProvider$Guardian { *; }",
  "-keep class akka.dispatch.UnboundedMailbox { *; }",
  "-keep public class akka.remote.RemoteActorRefProvider {public <init>(...);}",
  "-keep class akka.remote.RemoteActorRefProvider$RemotingTerminator {*;}",
  "-keep class akka.remote.transport.netty.NettyTransport {*;}",
  "-keep class akka.remote.transport.AkkaProtocolManager {*;}",
  "-keep class * extends akka.dispatch.RequiresMessageQueue {*;}",
  "-keep class akka.remote.transport.ProtocolStateActor$AssociationState {*;}",
  "-keep class akka.remote.PhiAccrualFailureDetector {*;}",
  "-keep class akka.remote.EndpointWriter {*;}",
  "-keep class akka.dispatch.UnboundedDequeBasedMailbox {*;}",
  "-keep class akka.remote.ReliableDeliverySupervisor {*;}",
  "-keep class akka.remote.RemoteWatcher {*;}",
  "-keep class akka.remote.EndpointManager {*;}",
  "-keep class akka.actor.CreatorFunctionConsumer {*;}",
  "-keep class akka.dispatch.BoundedDequeBasedMessageQueueSemantics {*;}",
  "-keep class akka.dispatch.UnboundedMessageQueueSemantics {*;}",
  "-keep class akka.dispatch.UnboundedDequeBasedMessageQueueSemantics {*;}",
  "-keep class akka.actor.LocalActorRefProvider$SystemGuardian {*;}",
  "-keep class * extends akka.remote.RemotingLifecycle",
  "-keep class akka.actor.DefaultSupervisorStrategy {*;}",
  "-keep class akka.remote.*",
  "-keep class scala.concurrent.duration.FiniteDuration",
  "-keep class scala.concurrent.ExecutionContext",
  "-keep class akka.actor.SerializedActorRef {*;}",
  "-keep class akka.actor.LightArrayRevolverScheduler {*;}",
  "-keep class akka.remote.netty.NettyRemoteTransport {*;}",
  "-keep class akka.serialization.JavaSerializer {*;}",
  "-keep class akka.serialization.ProtobufSerializer {*;}",
  "-keep class akka.serialization.ByteArraySerializer {*;}",
  "-keep class akka.event.Logging*",
  "-keep class akka.event.Logging$LogExt{*;}",
  "-keep class akka.io.TcpManager { *; }",
  "-keep class akka.routing.RoutedActorCell$RouterActorCreator { *; }",
  "-keep class akka.persistence.Channel { *; }",
  """|-keepclasseswithmembers class * {
    |    <init>(scala.None$, akka.persistence.ChannelSettings);
    |}""".stripMargin,
  "-keep class akka.persistence.ReliableDelivery { *; }",
  """|-keepclasseswithmembers class * {
    |    <init>(akka.persistence.ChannelSettings);
    |}""".stripMargin,
  "-keep class akka.persistence.snapshot.local.LocalSnapshotStore { *; }",
  """|-keepclasseswithmembers class akka.persistence.snapshot.local.LocalSnapshotStore {
    |    <init>();
    |}""".stripMargin,
  "-keep class akka.persistence.ChannelSettings { *; }",
  "-keep class akka.persistence.journal.leveldb.LeveldbJournal { *; }",
  // journal.leveldb.LeveldbJournal {
  """|-keepclasseswithmembers class akka.persistence.** {
    |    <init>();
    |}""".stripMargin,
  "-keep class akka.persistence.journal.leveldb.LeveldbStore { *; }",
  "-keep class akka.serialization.**",
  """|-keepclasseswithmembers class akka.persistence.** {
    |    <init>(...);
    |}""".stripMargin
)

proguardCache in Android ++= Seq(
  ProguardCache("scala") % "org.scala-lang" % "scala-library_2.11.1",
  ProguardCache("scala.reflect") % "org.scala-lang" % "scala-reflect_2.11.1",
  ProguardCache("android.support.v4") % "com.android.support" % "support-v4",
  ProguardCache("akka") % "com.typesafe.akka" % "akka-actor_2.11",
  ProguardCache("scalaz") % "org.scalaz" % "scalaz-core_2.11",
  ProguardCache("akka.persistence") % "com.typesafe.akka" % "akka-persistence-experimental_2.11",
  ProguardCache("com.google.common") % "com.google.guava" % "guava"
)

// Necessary to use akka-persistence on android
externalDependencyClasspath in Compile ~= { cp =>
  cp filterNot { attributed => attributed.data.getName contains "leveldbjni-all-1.7.jar"}
}

externalDependencyClasspath in Compile ~= { cp =>
  cp filterNot { attributed => attributed.data.getName contains "leveldbjni-win32-1.5.jar"}
}

externalDependencyClasspath in Compile ~= { cp =>
  cp filterNot { attributed => attributed.data.getName contains "leveldbjni-win64-1.5.jar"}
}

externalDependencyClasspath in Compile ~= { cp =>
  cp filterNot { attributed => attributed.data.getName contains "leveldbjni-linux64-1.5.jar"}
}

externalDependencyClasspath in Compile ~= { cp =>
  cp filterNot { attributed => attributed.data.getName contains "leveldbjni-osx-1.5.jar"}
}