set "isMultiplayerMode=true"
set "IP=127.0.0.1"
set "PORT=6010"
java --add-exports java.base/java.lang=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED -Dsun.java2d.d3d=false -Dsun.java2d.uiScale=1 a2.MyGame %isMultiplayerMode% %IP% %PORT% UDP