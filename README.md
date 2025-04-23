# Uživatelská dokumentace

Program je jednoduchý vizualizér hudby vytvořený pomocí JavaFX.

Pro spuštění programu je potřeba si nainstalovat Java 19 (měla by být správně nastavena proměnná
prostředí JAVA_HOME), poté spustit v terminálu v adresáři programu příkaz "./mvnw clean javafx:run".

Po spuštění programu je otevřeno hlavní okno, v němž je potřeba nejprve stisknout tlačítko
"Select Playlist" pro výběr playlistu.

Po výběru playlistu do programu budou načteny všechny podporované hudební soubory z vybrané
složky. Podporované formáty jsou aac, m4a, mp3, pcm, wav a aiff.

[!] Při nahrání .mp3 souboru s vysokým bitrate je možné, že program nebude schopen správně
určit jeho délku. Toto je bohužel bugem knihovny javafx.media.

Skladby z vybraného playlistu budou uspořádany abecedně podle jejich jmen.

Po načtení playlistu se aktivují kontrolovací prvky programu nahoře: tlačítka Previous, Play/Pause,
Next, slider pro přetočení a slider pro nastavení hlasitosti.

Až se začne přehrávání skladby, začne být zobrazována generovaná vizualizace v černém
obdélníku v horní části hlavního okna.

Vizualizace se skládá z dvou částí: z tyček vepředu a kroužků vzadu.
Tyčky reprezentují zvuková pásma skladby a jejich délka závisí na síle daného pásma.
Kroužky mění svoji velikost pokaždé, když program detekuje takt skladby (toto nemusí fungovat přesně).

Pro přechod na jinou skladbu využijte tlačítka Previous a Next.
Po skončení skladby se začne přehrávat další skladba z playlistu. Po skončení poslední skladby se
přehrávání automaticky zastaví.
