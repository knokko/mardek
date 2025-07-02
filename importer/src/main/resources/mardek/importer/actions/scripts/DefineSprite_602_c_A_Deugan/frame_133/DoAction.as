GameData.plotVars.SUNSET = "NIGHT";
GameData.plotVars.MORIC_BEATEN = 5;
_root.cont.PC.DrawFrame(8);
_root.cont.Deugan.DrawFrame(8);
_root.FadeIn(100);
_root.fader.quieten = true;
_root.fader.onMaxFade = function()
{
   _root.cont.Deugan.removeMovieClip();
   _root.DrawSprite({name:"Deugan",model:"deugan_soldier",x:3,y:2,walkspeed:-2,dir:"e",elem:"EARTH",conv:"c_A_Deugan",PC_STATS:GameData.plotVars.Allies.Deugan});
   _root.DrawSprite({name:"Emela",model:"emela_soldier",x:5,y:2,walkspeed:-2,dir:"w",elem:"WATER",conv:"c_A_Deugan",PC_STATS:GameData.plotVars.Allies.Emela});
   if(GameData.plotVars.Allies.Vehrn != null)
   {
      _root.DrawSprite({name:"Vehrn",model:"vehrn",x:3,y:3,walkspeed:-2,dir:"e",elem:"EARTH",conv:"c_A_Deugan",PC_STATS:GameData.plotVars.Allies.Vehrn});
   }
   if(GameData.plotVars.Allies.Zach != null)
   {
      _root.DrawSprite({name:"Zach",model:"zach",x:5,y:3,walkspeed:-2,dir:"w",elem:"AIR",conv:"c_A_Deugan",PC_STATS:GameData.plotVars.Allies.Zach});
   }
   DO_ACTIONS([["WAIT",10],["TALK","c_A_Deugan"]],"PC");
   this.quieten = false;
   _root.MUSIC.setVolume(100);
   _root.FadeOut(10);
   _root.playMusic("none");
   delete this.onMaxFade;
};
END();
