unstitcher
==========

It’s a rewrite of [Dinnerbone’s utility](https://twitter.com/Dinnerbone/statuses/281796115647561728) to convert Minecraft texture packs to the 1.5 format.

Usage: `java -jar unstitcher.jar [-h | <input>.zip [<output>.zip]]`

Calling it without parameters invokes the GUI, else the logging takes place in the command line and no window is shown.

distinguishing features
-----------------------
* can be called from the command line for batch-conversions
* supports bigger texture packs due to more memory-efficient code
* supports converting animations (except lava and water as of yet)
