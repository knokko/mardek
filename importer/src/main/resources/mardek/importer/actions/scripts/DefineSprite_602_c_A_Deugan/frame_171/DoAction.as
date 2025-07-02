Say("RM","deep","Come on... Deugan\'s made his choice, and I can see there\'s no changing his mind. We need to go now, quickly!");
TempVars.DONT_SCROLL_MAP = true;
DO_ACTIONS([["RUN",6,0],["GOTO","s1"],["WAIT",6],["RUN",0,-7]],"PC",true);
DO_ACTIONS([["RUN",6,0],["RUN",0,-9]],"Emela",true);
DO_ACTIONS([["RUN",0,-1],["RUN",6,0],["RUN",0,-9],["UNFREEZE"]],_root.PCstats[3].name,true);
