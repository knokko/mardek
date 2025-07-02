GameData.plotVars.SUNSET = null;
GameData.plotVars.BRIEFING = 6;
GameData.plotVars.NO_SWITCH = 0;
_root.cont.PC.DrawFrame(8);
_root.cont.Deugan.DrawFrame(8);
_root.FadeIn(80);
_root.fader.quieten = true;
_root.fader.onMaxFade = function()
{
   _root.cont.Deugan.removeMovieClip();
   this.quieten = null;
   _root.MUSIC.setVolume(100);
   _root.playMusic("none");
   this.FadeOut(80);
};
END();
