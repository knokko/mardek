if(_root.area == "tv_arena")
{
   gotoAndStop("ch3arena");
}
else if(_root.area == "gemmine2")
{
   gotoAndStop("minehelp");
}
else if(_root.area == "moric_saucer5")
{
   gotoAndStop("Farewell");
}
else if(_root.area == "soothwood_shaman")
{
   gotoAndStop("soothwood_shaman");
}
else if(GameData.plotVars.MORIC_BEATEN == 5)
{
   gotoAndStop("afterMoric5");
}
else if(GameData.plotVars.MORIC_BEATEN == 4)
{
   gotoAndStop("afterMoric");
}
else if(_root.area == "lakequr")
{
   gotoAndStop("lakequr");
}
else if(_root.area == "glens2")
{
   gotoAndStop("glens");
}
else if(_root.area == "gc_PCdorm")
{
   if(GameData.plotVars.BRIEFING == 5)
   {
      gotoAndStop("briefing5");
   }
   else
   {
      gotoAndStop("Ch2_1");
   }
}
else if(_root.area == "soothwood")
{
   gotoAndStop("soothwood");
}
else
{
   nextFrame();
}
