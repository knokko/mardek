GameData.plotVars.MORIC_BEATEN = 6;
GameData.plotVars.ZOMBIES = "CANONIA";
_root.lastDir = "s";
GameData.plotVars.NO_SWITCH = 0;
GameData.plotVars.MARDEK_ALONE = false;
SET_SPEAKER("Deugan");
JOINPARTY();
SET_SPEAKER("Emela");
JOINPARTY();
if(GameData.plotVars.LAST_P4 == "Vehrn")
{
   SET_SPEAKER("Vehrn");
   JOINPARTY();
   SET_SPEAKER("Zach");
   JOINPARTY();
}
else
{
   SET_SPEAKER("Vehrn");
   JOINPARTY();
   SET_SPEAKER("Zach");
   JOINPARTY();
}
delete GameData.plotVars.LAST_P4;
END();
