taskManager
===========

A simple project using Akka persistence on Android

This project uses
+ android-sdk-plugin version 1.2.17 from https://github.com/pfn/android-sdk-plugin
+ Scala 2.11.1
+ Macroid 2.0.0-M2
+ Akka 2.3.3
+ akka-persistence-experimental 2.3.3
 
Please take a look at the build.sbt file for current configuration.

Current State
--------------

There are several ideas that are implemented here:

1. Every Android Activity uses Fragments. Fragments have a corresponding Actor, here called "Reactors", that are responsible for communication with the rest of the actor system.

2. The actor system is created and maintained in a singleton object named ActorSystemManager.
   This also holds a `controller` and a `fsmProcessor`.

3. `ActorSystemManager.controller` manages a stack of `ActivityActors` each one of them responsible of 
   handling events of a corresponding `ThereActivity` (an actual Android Activity of the application).
   For example:
   LogonAA (The Logon Android Activity) is handled by LogonActivityActor.
   MoodAA (The Mood Android Activity) is handled by MoodActivityActor.
   TaskListAA (The TaskList Android Activity) is handled by TasksActivityActor.

4. `ActorSystemManager.fsmProcessor` is in charge of message persistence.

5. `ThereActivity`s (like `LogonAA`) have one or more `ThereFragments` (like `LogonFragment`) each one
   "owning" a reactor (like `LogonReactor`). Every reactor is an Akka FSM that can be in two states:
   `WaitingForInitialiser` or `Working`. See `ThereActivityReactorTypes.scala`.

6. A `ThereActivity` is not the main responsible for keeping state information. This responsibility
   is delegated to the corresponding `ActivityActor` (which is an Akka FMS too). For example,
   `LogonActivityActor` keeps track of the user login (if `LogonAA` is in the `Initial` or `LoggedOn`
   state).

7. A `ThereActivity` also keeps track of the "data" for the corresponding `ThereActivity`. For example,
   `LogonActivityActor` receives the name and email introduced in the widgets created in `LogonFragment`
   and stores this information. See the definition of the type `LogonAA.A` (which is `UserData`) in
   `LogonAA.scala`. After a `ThereFragment` is destroyed (for example, after rotation of device) and
   created again, it restores the data from the backup in the corresponding `ActivityActor`.
   See `TaskListAA` and corresponding `TaskActivityActor` for an example using `ListFragment`.

8. The life cycle of reactors is tied to the life cycle of the corresponding `ThereFragment`. A reactor is
   created during `onCreate(Bundle)` and destroyed during `onDestroy()`. See `ThereActivity.scala`.

9. Logic (code) for the "control" of the application is not inside `ThereFragments`. It is supposed to live
   in the `controller` and the `ActivityActors`.


Where I want to go (not yet working)
------------------------------------

A `recovery mode for the application` involving Akka Persistence. Currently, `fsmProcessor` receives
persisted events very fast during it's recovery. But those messages are not delivered to the corresponding
reactors. Android Activities, fragments (and the corresponding actors --reactors) take some (little, but
not negligible) time to get created. <--- I NEED HELP HERE: I've very little experience with Akka
Persistence.

Ideally, I would like to avoid the creation and destruction of `ThereActivity`s and `ActivityActor`s at the
same time that recovery of persisted messages is done. Imagine a user navigates the Android Activities like
this: `LogonAA` -> `MoodAA` -> `TaskListAA` -> `MoodAA` -> `TaskListAA` -> `LogonAA`, persisting events
along the way. I would like to get the final `recovered` state and data and then build the corresponding
stack of Android Activities (`ThereActivity`s) _and_ `ActivityActors`. However, I don't know how to do this
without a (big) modification of the Akka FSMs for `ActivityActors`.

A completely different path (for my spare time)
-----------------------------------------------
Using Akka stuff on Android has been somewhat painful. Take a look at the messy Proguard configuration I got
to avoid runtime errors because Proguard is not able to see that a lot of things in Akka use **reflection**.

In a "beautiful" world, where I could do functional programming in my everyday work, I would rather use the
typelevel programming libraries: scalaz, shapeless, spire, etc. However, this little project **must** use
Akka and Akka Persistence.

Also: I am not as a strong functional programmer as I wish I were. I've been studying some scalaz and Haskell
in my free time, and have done some programming using `scalaz.State` and `scalaz.ReaderWriterStateT`. 
In my limited understanding I thought of this: 
+ The Reader part could be used for common configuration.
+ The Writer part to simulate the Persistence of events.
+ State is everywhere. The original problem (for example MoodAA is just a toy representing the idea of a FSM)
  uses hierarchichal FSM, which could be modeled by the ActivityActors as a set.

<--- I NEED HELP HERE TOO, because my `functional`-kung-fu is not very mature (still).


Credits
=======

+ Sponsored by Tim Pigden and Optrak.